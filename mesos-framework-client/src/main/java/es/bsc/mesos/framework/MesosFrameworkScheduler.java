package es.bsc.mesos.framework;

import es.bsc.mesos.framework.log.Loggers;
import es.bsc.mesos.framework.exceptions.FrameworkException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo;
import org.apache.mesos.Protos.ContainerInfo;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.NetworkInfo;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Protos.Value;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;


public class MesosFrameworkScheduler implements Scheduler {

    private static final String EMPTY = "";
    private static final String UNDEFINED_IP = "-1.-1.-1.-1";
    private static final int MAX_LAUNCH_RETRIES = 3;

    private static final String CPUS_RESOURCE = "cpus";
    private static final String MEM_RESOURCE = "mem";
    private static final String DISK_RESOURCE = "disk";
    private static final String PORTS_RESOURCE = "ports";

    private static final String DOCKER_COMMAND = "/usr/sbin/sshd -D";
    private static final String WORKER_NAME = "COMPSsWorker";

    private static final Logger LOGGER = LogManager.getLogger(Loggers.MF_SCHEDULER);

    private final int sshPort;
    private final int openPorts;
    private final long startingPort;

    private final AtomicInteger taskIdGenerator = new AtomicInteger();

    private FrameworkID frameworkId;

    private Semaphore registerSem;

    private final List<String> runningTasks;
    private final List<String> pendingTasks;
    private final Map<String, MesosTask> tasks;

    private String dockerImage;


    public MesosFrameworkScheduler(String image, int sshPort, int openPorts, long startingPort) {
        LOGGER.debug("Initialize " + this.getClass().getName() + "with image " + image);
        this.dockerImage = image;
        this.sshPort = sshPort;
        this.openPorts = openPorts;
        this.startingPort = startingPort;
        this.runningTasks = Collections.synchronizedList(new LinkedList<String>());
        this.pendingTasks = Collections.synchronizedList(new LinkedList<String>());
        this.tasks = Collections.synchronizedMap(new HashMap<String, MesosTask>());
    }

    public String getFrameworkId() {
        return frameworkId == null ? EMPTY : frameworkId.getValue();
    }

    public synchronized String generateWorkerId() {
        return WORKER_NAME + Integer.toString(taskIdGenerator.incrementAndGet());
    }

    public String requestWorker(SchedulerDriver driver, List<Resource> resources) {
        LOGGER.debug("Requested worker");
        String newWorkerId = generateWorkerId();
        synchronized (this) {
            pendingTasks.add(newWorkerId);
            tasks.put(newWorkerId, new MesosTask(newWorkerId, TaskState.TASK_STAGING, resources));
            driver.reviveOffers();
        }
        return newWorkerId;
    }

    private void releaseRegisterSem() {
        if (registerSem != null) {
            registerSem.release();
            registerSem = null;

            LOGGER.debug("Release framework register semaphore");
        }
    }

    private void acquireSem(Semaphore sem) {
        try {
            LOGGER.debug("Acquire semaphore " + sem.toString());
            sem.acquire();
        } catch (InterruptedException ex) {
            LOGGER.error("Error waiting for semaphore!", ex);
        }
    }

    private void acquireSem(Semaphore sem, long timeout, TimeUnit unit) {
        try {
            LOGGER.debug("Acquire semaphore " + sem.toString());
            sem.tryAcquire(timeout, unit);
        } catch (InterruptedException ex) {
            LOGGER.error("Error waiting for semaphore!", ex);
        }
    }

    public void waitTask(String id, TaskState state, long timeout, TimeUnit unit) throws FrameworkException {
        Semaphore sem = new Semaphore(0);
        synchronized (tasks) {
            if (!tasks.containsKey(id)) {
                throw new FrameworkException("Task with id " + id + " does not exist");
            } else if (tasks.get(id).getState() == state) {
                // Task already in that state, nothing to do
                return;
            }
            tasks.get(id).addWait(state, sem);
        }
        LOGGER.debug("Waiting task " + id + " " + timeout + " " + unit.toString() + " to change state to " + state.toString());
        acquireSem(sem, timeout, unit);
        synchronized (tasks) {
            if (!tasks.containsKey(id)) {
                throw new FrameworkException("Task with id " + id + " does not exist");
            } else if (tasks.get(id).getState() != state) {
                pendingTasks.remove(id);
                runningTasks.remove(id);
                tasks.remove(id);
                throw new FrameworkException("Timeout waiting task " + id + " to change to " + state.toString());
            }
        }
    }

    public void waitRegistration() {
        LOGGER.debug("Wait for framework to register");
        if (frameworkId != null) {
            releaseRegisterSem();
        } else {
            registerSem = new Semaphore(0);
            acquireSem(registerSem);
        }
    }

    public void waitRegistration(long timeout, TimeUnit unit) throws FrameworkException {
        LOGGER.debug("Wait for framework to register " + timeout + " " + unit.toString());
        if (frameworkId != null) {
            releaseRegisterSem();
        } else {
            registerSem = new Semaphore(0);
            acquireSem(registerSem, timeout, unit);
            if (frameworkId == null) {
                throw new FrameworkException("Could not register framework. Check that Mesos master IP is correct.");
            }
        }
    }

    public void removeTask(SchedulerDriver driver, String id, long timeout, TimeUnit unit) throws FrameworkException {
        synchronized (this) {
            // Task still in pending queue, not launched to run in Mesos
            if (pendingTasks.contains(id)) {
                pendingTasks.remove(id);
                return;
            } else if (!tasks.containsKey(id)) {
                runningTasks.remove(id);
                throw new FrameworkException("Task with id " + id + " does not exist");
            }
        }
        driver.killTask(TaskID.newBuilder().setValue(id).build());
        waitTask(id, TaskState.TASK_KILLED, timeout, unit);
        tasks.remove(id);
    }

    private List<MesosOffer> processOffers(List<Offer> offers) {
        List<MesosOffer> mesosOffers = new LinkedList<MesosOffer>();
        for (Offer offer : offers) {
            mesosOffers.add(new MesosOffer(offer));
        }
        return mesosOffers;
    }

    private int bestFit(MesosOffer requirements, List<MesosOffer> offers) {
        int index = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < offers.size(); i++) {
            MesosOffer mo = offers.get(i);
            double score = requirements.distance(mo);
            if (mo.hasEnoughPorts(openPorts) && score >= 0.0 && score < bestScore) {
                index = i;
                bestScore = score;
            }
        }
        return index;
    }

    private DockerInfo.PortMapping buildPortMapping(int container, int host, String protocol) {
        return DockerInfo.PortMapping.newBuilder().setContainerPort(container).setHostPort(host).setProtocol(protocol).build();
    }

    private void addPortsToDocker(DockerInfo.Builder builder, List<Value.Range> ports) {
        LinkedList<Integer> portsToAssign = new LinkedList<Integer>();
        portsToAssign.add(new Integer(sshPort));
        for (int i = 0; i < openPorts - 1; i++) {
            portsToAssign.add((int) startingPort + i);
        }
        assignPorts: for (Value.Range r : ports) {
            for (int host = (int) r.getBegin(); host <= r.getEnd(); host++) {
                Integer containerPort = portsToAssign.pollFirst();
                if (containerPort == null) {
                    // There is no more ports to assign
                    break assignPorts;
                }
                builder.addPortMappings(buildPortMapping((int) containerPort, host, "tcp"));
            }
        }
    }

    private DockerInfo getDockerInfo(List<Value.Range> ports) {
        DockerInfo.Builder dockerInfoBuilder = DockerInfo.newBuilder().setImage(dockerImage).setNetwork(DockerInfo.Network.BRIDGE);
        addPortsToDocker(dockerInfoBuilder, ports);
        return dockerInfoBuilder.build();
    }

    private void launchTask(SchedulerDriver driver, Offer offer, TaskInfo task) {
        List<TaskInfo> tasksToSubmit = new ArrayList<TaskInfo>();
        List<OfferID> offerIds = new ArrayList<OfferID>();
        tasksToSubmit.add(task);
        offerIds.add(offer.getId());
        driver.launchTasks(offerIds, tasksToSubmit);
    }

    private Value.Ranges buildRanges(List<Value.Range> ranges) {
        Value.Ranges.Builder rangesBuilder = Value.Ranges.newBuilder();
        for (Value.Range r : ranges) {
            rangesBuilder.addRange(r);
        }
        return rangesBuilder.build();
    }

    private Value.Scalar buildScalar(double value) {
        return Value.Scalar.newBuilder().setValue(value).build();
    }

    private Resource buildResource(String name, double value) {
        return Resource.newBuilder().setName(name).setType(Value.Type.SCALAR).setScalar(buildScalar(value)).build();
    }

    private Resource buildResource(String name, List<Value.Range> ranges) {
        return Resource.newBuilder().setName(name).setType(Value.Type.RANGES).setRanges(buildRanges(ranges)).build();
    }

    private TaskInfo getTaskInfo(String idTask, MesosOffer reqs, MesosOffer offer) {
        TaskID taskId = TaskID.newBuilder().setValue(idTask).build();

        List<Value.Range> pickedPorts = offer.getMinPorts(openPorts);
        CommandInfo commandInfoDocker = CommandInfo.newBuilder().setValue(DOCKER_COMMAND).build();

        // container info
        ContainerInfo.Builder containerInfoBuilder = ContainerInfo.newBuilder();
        containerInfoBuilder.setType(ContainerInfo.Type.DOCKER);
        containerInfoBuilder.setDocker(getDockerInfo(pickedPorts));

        // create task to run
        TaskInfo taskInfo = TaskInfo.newBuilder()
                .setName("Task " + idTask)
                .setTaskId(taskId)
                .setSlaveId(offer.getOffer().getSlaveId())
                .addResources(buildResource(CPUS_RESOURCE, reqs.getCpus()))
                .addResources(buildResource(MEM_RESOURCE, reqs.getMem()))
                .addResources(buildResource(DISK_RESOURCE, reqs.getDisk()))
                .addResources(buildResource(PORTS_RESOURCE, pickedPorts))
                .setContainer(containerInfoBuilder)
                .setCommand(commandInfoDocker)
                .build();

        LOGGER.debug("Launching task " + taskId.getValue());
        return taskInfo;
    }

    private void declineOffers(SchedulerDriver driver, List<OfferID> offerIds) {
        for (OfferID id : offerIds) {
            LOGGER.debug("Decline offer: " + id.getValue());
            driver.declineOffer(id);
        }
    }

    private List<OfferID> getOfferIdList(List<Offer> offers) {
        List<OfferID> ids = new LinkedList<OfferID>();
        for (Offer o : offers) {
            ids.add(o.getId());
        }
        return ids;
    }

    @Override
    public synchronized void resourceOffers(SchedulerDriver driver, List<Offer> offers) {
        LOGGER.info(String.format("Received %d offers", offers.size()));
        List<OfferID> unusedOffers = getOfferIdList(offers);
        if (pendingTasks.isEmpty()) {
            LOGGER.info("Empty worker requests queue");
            declineOffers(driver, unusedOffers);
            driver.suppressOffers();
            return;
        }
        List<MesosOffer> processedOffers = processOffers(offers);
        for (int n = 0; n < pendingTasks.size(); n++) {
            String id = pendingTasks.get(n);
            if (!tasks.containsKey(id)) {
                LOGGER.warn("No such id exists: " + id);
                continue;
            }
            MesosOffer requirements = new MesosOffer(tasks.get(id).getRequirements());
            int index = bestFit(requirements, processedOffers);
            if (index == -1) {
                LOGGER.debug("Request does not fit: " + requirements.toString());
                continue;
            }
            MesosOffer offer = processedOffers.get(index);
            TaskInfo task = getTaskInfo(id, requirements, offer);
            OfferID offerId = offer.getOffer().getId();
            launchTask(driver, offer.getOffer(), task);

            LOGGER.info("Launching task " + id + " in offer " + offerId);
            unusedOffers.remove(offerId);
            offer.removeResourcesFrom(requirements);
            pendingTasks.remove(n);
        }
        declineOffers(driver, unusedOffers);
    }

    @Override
    public void offerRescinded(SchedulerDriver driver, OfferID offerId) {
        LOGGER.debug("Offer rescined: " + offerId.getValue());
    }

    public synchronized String getTaskIp(String id) {
        if (tasks.containsKey(id)) {
            LOGGER.info(String.format("IP %s assigned for task %s", tasks.get(id).getIp(), id));
            return tasks.get(id).getIp();
        }
        return UNDEFINED_IP;
    }

    private boolean findIpAddress(NetworkInfo ni, TaskStatus ts) {
        for (NetworkInfo.IPAddress ip : ni.getIpAddressesList()) {
            LOGGER.debug("Found IP address in network: " + ip.getIpAddress());
            synchronized (tasks) {
                tasks.get(ts.getTaskId().getValue()).setIp(ip.getIpAddress());
            }
            return true;
        }
        return false;
    }

    private void getIpAddress(TaskStatus status) {
        // TODO Check that network assigned is correct, for now using first IP found
        List<NetworkInfo> networks = status.getContainerStatus().getNetworkInfosList();
        for (NetworkInfo ni : networks) {
            if (findIpAddress(ni, status)) {
                return;
            }
        }
    }

    @Override
    public synchronized void statusUpdate(SchedulerDriver driver, TaskStatus status) {
        String id = status.getTaskId().getValue();
        TaskState state = status.getState();
        LOGGER.debug(String.format("Status update: task %s is in state %s. Reason: %s Message: %s", id, state,
                status.getReason().getNumber(), status.getMessage()));
        if (!tasks.containsKey(id)) {
            LOGGER.warn("No such id exists: " + id);
            return;
        }
        MesosTask mt = tasks.get(id);
        getIpAddress(status);
        mt.setState(state);
        switch (state) {
            case TASK_LOST:
            case TASK_ERROR:
            case TASK_FAILED:
                LOGGER.warn(id + " Task failed! adding to pending");
                mt.incrementRetries();
                if (mt.getRetries() < MAX_LAUNCH_RETRIES) {
                    runningTasks.remove(id);
                    pendingTasks.add(id);
                    driver.reviveOffers();
                } else {
                    LOGGER.warn("Reached max retries for launch task " + id);
                }
                break;
            case TASK_FINISHED:
            case TASK_KILLED:
                LOGGER.debug(id + " Task killed successfully.");
                runningTasks.remove(id);
                pendingTasks.remove(id);
                break;
            case TASK_RUNNING:
                pendingTasks.remove(id);
                runningTasks.add(id);
                break;
            default:
                // Nothing to do
        }
    }

    @Override
    public synchronized void registered(SchedulerDriver driver, FrameworkID frameworkId, MasterInfo masterInfo) {
        LOGGER.info("Framework registered with ID " + frameworkId.getValue());
        this.frameworkId = frameworkId;
        releaseRegisterSem();
    }

    @Override
    public synchronized void reregistered(SchedulerDriver driver, MasterInfo masterInfo) {
        LOGGER.info("Framework Reregistered");
        releaseRegisterSem();
    }

    @Override
    public void disconnected(SchedulerDriver driver) {
        LOGGER.warn("Framework Disconnected!");
    }

    @Override
    public void frameworkMessage(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, byte[] data) {
        LOGGER.debug("Message received");
    }

    @Override
    public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {
        // Nothing to do, need to Override
    }

    @Override
    public void executorLost(SchedulerDriver driver, ExecutorID executorId, SlaveID slaveId, int status) {
        // Nothing to do, need to Override
    }

    @Override
    public void error(SchedulerDriver driver, String message) {
        LOGGER.warn("Error: " + message);
    }

}
