package es.bsc.mesos.framework;

import es.bsc.mesos.framework.log.Loggers;
import es.bsc.mesos.framework.exceptions.FrameworkException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos.Credential;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskState;


public class MesosFramework {

    private static final int FAILOVER_TIMEOUT = 120_000;

    // 3 min default timeout for all register, wait run, kill
    private static final String DEFAULT_TIMEOUT = "180000";
    private static final String DEFAULT_TIMEOUT_UNITS = "MILLISECONDS";

    private static final String TRUE = "true";

    private static final String FRAMEWORK_NAME = "COMPSs Framework";
    private static final String COMPSS_PRINCIPAL = "compss-framework-java";

    private static final String SERVER_IP = "Server";

    // Mesos Framework options
    private static final String MESOS_CHECKPOINT = "mesos-checkpoint";
    private static final String MESOS_AUTHENTICATE = "mesos-authenticate";
    private static final String MESOS_DEFAULT_PRINCIPAL = "mesos-default-principal";
    private static final String MESOS_DEFAULT_SECRET = "mesos-default-secret";

    private static final String MESOS_DOCKER_IMAGE = "mesos-docker-image";
    private static final String MESOS_FRAMEWORK_REGISTER_TIMEOUT = "mesos-framework-register-timeout";
    private static final String MESOS_FRAMEWORK_REGISTER_TIMEOUT_UNITS = "mesos-framework-register-timeout-units";
    private static final String MESOS_WORKER_WAIT_TIMEOUT = "mesos-worker-wait-timeout";
    private static final String MESOS_WORKER_WAIT_TIMEOUT_UNITS = "mesos-worker-wait-timeout-units";
    private static final String MESOS_WORKER_KILL_TIMEOUT = "mesos-worker-kill-timeout";
    private static final String MESOS_WORKER_KILL_TIMEOUT_UNITS = "mesos-worker-kill-timeout-units";
    private static final String MESOS_WORKER_SSH_PORT = "mesos-worker-ssh-port";
    private static final String MESOS_WORKER_STARTING_PORT = "mesos-worker-starting-port";
    private static final String MESOS_WORKER_NUM_OPEN_PORTS = "mesos-worker-num-open-ports";

    private static final String DEFAULT_DOCKER_IMAGE = "compss/compss:latest";

    private static final String DEFAULT_WORKER_OPEN_PORTS = "10";
    private static final String DEFAULT_WORKER_STARTING_PORT = "43100";
    private static final String DEFAULT_WORKER_SSH_PORT = "22";

    private static final Logger LOGGER = LogManager.getLogger(Loggers.MF);

    private final long runWorkerTimeout;
    private final TimeUnit runWorkerTimeoutUnits;
    private final long killWorkerTimeout;
    private final TimeUnit killWorkerTimeoutUnits;

    private final MesosFrameworkScheduler scheduler;
    private final MesosSchedulerDriver driver;


    private String getProperty(Map<String, String> props, String key, String defaultValue) {
        return props.containsKey(key) ? props.get(key) : defaultValue;
    }

    public MesosFramework(Map<String, String> props) throws FrameworkException {

        if (!props.containsKey(SERVER_IP)) {
            throw new FrameworkException("Missing Mesos master IP");
        }
        String mesosMasterIp = props.get(SERVER_IP);

        FrameworkInfo.Builder frameworkBuilder = FrameworkInfo.newBuilder().setFailoverTimeout(FAILOVER_TIMEOUT).setUser("") // Have
                                                                                                                             // Mesos
                                                                                                                             // fill
                                                                                                                             // in
                                                                                                                             // the
                                                                                                                             // current
                                                                                                                             // user.
                .setName(FRAMEWORK_NAME);

        long registerTimeout = Long.parseLong(getProperty(props, MESOS_FRAMEWORK_REGISTER_TIMEOUT, DEFAULT_TIMEOUT));
        TimeUnit registerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_FRAMEWORK_REGISTER_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));
        runWorkerTimeout = Long.parseLong(getProperty(props, MESOS_WORKER_WAIT_TIMEOUT, DEFAULT_TIMEOUT));
        runWorkerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_WORKER_WAIT_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));
        killWorkerTimeout = Long.parseLong(getProperty(props, MESOS_WORKER_KILL_TIMEOUT, DEFAULT_TIMEOUT));
        killWorkerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_WORKER_KILL_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));

        int sshPortWorker = Integer.parseInt(getProperty(props, MESOS_WORKER_SSH_PORT, DEFAULT_WORKER_SSH_PORT));
        int openPortsWorker = Integer.parseInt(getProperty(props, MESOS_WORKER_NUM_OPEN_PORTS, DEFAULT_WORKER_OPEN_PORTS));
        long startingPortWorker = Long.parseLong(getProperty(props, MESOS_WORKER_STARTING_PORT, DEFAULT_WORKER_STARTING_PORT));

        String mesosDockerImage = getProperty(props, MESOS_DOCKER_IMAGE, DEFAULT_DOCKER_IMAGE);

        scheduler = new MesosFrameworkScheduler(mesosDockerImage, sshPortWorker, openPortsWorker, startingPortWorker);

        if (props.containsKey(MESOS_CHECKPOINT) && TRUE.equals(props.get(MESOS_CHECKPOINT))) {
            LOGGER.info("Enabling checkpoint for the framework");
            frameworkBuilder.setCheckpoint(true);
        }
        if (props.containsKey(MESOS_AUTHENTICATE) && TRUE.equals(props.get(MESOS_AUTHENTICATE))) {
            LOGGER.info("Enabling authentication for the framework");

            if (!props.containsKey(MESOS_DEFAULT_PRINCIPAL)) {
                LOGGER.error("Expecting authentication principal in the environment");
                throw new FrameworkException("Missing principal in mesos authentication");
            }
            if (!props.containsKey(MESOS_DEFAULT_SECRET)) {
                LOGGER.error("Expecting authentication secret in the environment");
                throw new FrameworkException("Missing secret in mesos authentication");
            }
            Credential credential = Credential.newBuilder().setPrincipal(props.get(MESOS_DEFAULT_PRINCIPAL))
                    .setSecret(props.get(MESOS_DEFAULT_SECRET)).build();

            frameworkBuilder.setPrincipal(props.get(MESOS_DEFAULT_PRINCIPAL));
            driver = new MesosSchedulerDriver(scheduler, frameworkBuilder.build(), mesosMasterIp, credential);
        } else {
            frameworkBuilder.setPrincipal(COMPSS_PRINCIPAL);
            driver = new MesosSchedulerDriver(scheduler, frameworkBuilder.build(), mesosMasterIp);
        }
        LOGGER.info("Starting Mesos Framework, connecting to " + mesosMasterIp);
        driver.suppressOffers();
        driver.start();
        try {
            scheduler.waitRegistration(registerTimeout, registerTimeoutUnits);
        } catch (FrameworkException fe) {
            driver.stop();
            throw fe;
        } catch (Exception e) {
            throw new FrameworkException(e.getMessage() + " Mesos master IP: " + mesosMasterIp);
        }
    }

    public String getId() {
        LOGGER.info("Get framework ID");
        return scheduler.getFrameworkId();
    }

    public String requestWorker(List<Resource> resources) {
        LOGGER.info("Requested a worker");
        return scheduler.requestWorker(driver, resources);
    }

    public String waitWorkerUntilRunning(String id) {
        LOGGER.info("Waiting worker with id " + id);
        try {
            scheduler.waitTask(id, TaskState.TASK_RUNNING, runWorkerTimeout, runWorkerTimeoutUnits);
        } catch (FrameworkException fe) {
            LOGGER.warn("Exception raised waiting for worker " + id);
            LOGGER.warn(fe);
        }
        return scheduler.getTaskIp(id);
    }

    public void removeWorker(String id) {
        LOGGER.info("Remove worker with id " + id);
        try {
            scheduler.removeTask(driver, id, killWorkerTimeout, killWorkerTimeoutUnits);
        } catch (FrameworkException fe) {
            LOGGER.warn("Could not remove worker " + id);
            LOGGER.warn(fe);
        }
    }

    public void stop() {
        LOGGER.info("Stoping Mesos Framework");
        driver.stop();
    }

}
