package es.bsc.conn.clients.mesos.framework;

import es.bsc.conn.clients.mesos.framework.exceptions.FrameworkException;
import es.bsc.conn.clients.mesos.framework.log.Loggers;

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

/**
 * Representation of a Mesos Framework client
 *
 */
public class MesosFramework {

    private static final int FAILOVER_TIMEOUT = 120_000;

    // 3 min default timeout for all register, wait run, kill
    private static final String DEFAULT_TIMEOUT = "180000";
    private static final String DEFAULT_TIMEOUT_UNITS = "MILLISECONDS";

    private static final String TRUE = "true";

    private static final String DEFAULT_FRAMEWORK_NAME = "COMPSs Framework";
    private static final String COMPSS_PRINCIPAL = "compss-framework-java";

    private static final String SERVER_IP = "Server";

    // Mesos Framework options
    private static final String MESOS_FRAMEWORK_NAME = "mesos-framework-name";

    private static final String MESOS_CONTAINERIZER = "mesos-containerizer";
    private static final String MESOS_CHECKPOINT = "mesos-checkpoint";
    private static final String MESOS_AUTHENTICATE = "mesos-authenticate";
    private static final String MESOS_PRINCIPAL = "mesos-principal";
    private static final String MESOS_SECRET = "mesos-secret";
    private static final String MESOS_DOCKER_NETWORK_NAME = "mesos-docker-network-name";
    private static final String MESOS_DOCKER_NETWORK_TYPE = "mesos-docker-network-type";
    private static final String MESOS_DOCKER_MOUNT_VOLUME = "mesos-docker-mount-volume";
    private static final String MESOS_DOCKER_VOLUME_HOST_PATH = "mesos-docker-volume-host-path";
    private static final String MESOS_DOCKER_VOLUME_CONTAINER_PATH = "mesos-docker-volume-container-path";
    private static final String MESOS_DOCKER_COMMAND = "mesos-docker-command";

    private static final String MESOS_WORKER_NAME = "mesos-woker-name";
    private static final String MESOS_FRAMEWORK_HOSTNAME = "mesos-framework-hostname";
    private static final String MESOS_FRAMEWORK_REGISTER_TIMEOUT = "mesos-framework-register-timeout";
    private static final String MESOS_FRAMEWORK_REGISTER_TIMEOUT_UNITS = "mesos-framework-register-timeout-units";
    private static final String MESOS_WORKER_WAIT_TIMEOUT = "mesos-worker-wait-timeout";
    private static final String MESOS_WORKER_WAIT_TIMEOUT_UNITS = "mesos-worker-wait-timeout-units";
    private static final String MESOS_WORKER_KILL_TIMEOUT = "mesos-worker-kill-timeout";
    private static final String MESOS_WORKER_KILL_TIMEOUT_UNITS = "mesos-worker-kill-timeout-units";

    private static final String MESOS_DEFAULT_WORKER_NAME = "Worker";
    private static final String MESOS_DEFAULT_DOCKER_COMMAND = "/usr/sbin/sshd -D";

    private static final Logger LOGGER = LogManager.getLogger(Loggers.MF);

    private final long runWorkerTimeout;
    private final TimeUnit runWorkerTimeoutUnits;
    private final long killWorkerTimeout;
    private final TimeUnit killWorkerTimeoutUnits;

    private final MesosFrameworkScheduler scheduler;
    private final MesosSchedulerDriver driver;

    private String frameworkName = DEFAULT_FRAMEWORK_NAME;
    private String workerName = MESOS_DEFAULT_WORKER_NAME;
    private String dockerCommand = MESOS_DEFAULT_DOCKER_COMMAND;


    /**
     * Creates a new MesosFramework client with the given properties
     *
     * @param props
     * @throws FrameworkException
     */
    public MesosFramework(Map<String, String> props) throws FrameworkException {

        if (!props.containsKey(SERVER_IP)) {
            throw new FrameworkException("Missing Mesos master IP");
        }
        String mesosMasterIp = props.get(SERVER_IP);

        // Have mesos fill in the current user
        FrameworkInfo.Builder frameworkBuilder = FrameworkInfo.newBuilder().setFailoverTimeout(FAILOVER_TIMEOUT).setUser("")
                .setName(DEFAULT_FRAMEWORK_NAME);

        long registerTimeout = Long.parseLong(getProperty(props, MESOS_FRAMEWORK_REGISTER_TIMEOUT, DEFAULT_TIMEOUT));
        TimeUnit registerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_FRAMEWORK_REGISTER_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));
        runWorkerTimeout = Long.parseLong(getProperty(props, MESOS_WORKER_WAIT_TIMEOUT, DEFAULT_TIMEOUT));
        runWorkerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_WORKER_WAIT_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));
        killWorkerTimeout = Long.parseLong(getProperty(props, MESOS_WORKER_KILL_TIMEOUT, DEFAULT_TIMEOUT));
        killWorkerTimeoutUnits = TimeUnit.valueOf(getProperty(props, MESOS_WORKER_KILL_TIMEOUT_UNITS, DEFAULT_TIMEOUT_UNITS));

        scheduler = new MesosFrameworkScheduler();

        if (props.containsKey(MESOS_CHECKPOINT) && TRUE.equals(props.get(MESOS_CHECKPOINT))) {
            LOGGER.info("Enabling checkpoint for the framework");
            frameworkBuilder.setCheckpoint(true);
        }
        if (props.containsKey(MESOS_FRAMEWORK_HOSTNAME)) {
            LOGGER.info("Setting hostname for the framework: " + props.get(MESOS_FRAMEWORK_HOSTNAME));
            frameworkBuilder.setHostname(props.get(MESOS_FRAMEWORK_HOSTNAME));
        }
        if (props.containsKey(MESOS_FRAMEWORK_NAME)) {
            LOGGER.info("Setting name for the framework: " + props.get(MESOS_FRAMEWORK_NAME));
            frameworkBuilder.setName(props.get(MESOS_FRAMEWORK_NAME));
        }
        if (props.containsKey(MESOS_WORKER_NAME)) {
            LOGGER.info("Setting name for the framework: " + props.get(MESOS_WORKER_NAME));
            workerName = props.get(MESOS_WORKER_NAME);
        }
        if (props.containsKey(MESOS_DOCKER_COMMAND)) {
            LOGGER.info("Setting docker command to use for the worker: " + props.get(MESOS_DOCKER_COMMAND));
            dockerCommand = props.get(MESOS_DOCKER_COMMAND);
        }
        if (props.containsKey(MESOS_DOCKER_NETWORK_NAME)) {
            LOGGER.info("Using custom network for Docker: " + props.get(MESOS_DOCKER_NETWORK_NAME));
            scheduler.useDockerNetworkName(props.get(MESOS_DOCKER_NETWORK_NAME));
        }
        if (props.containsKey(MESOS_DOCKER_NETWORK_TYPE)) {
            LOGGER.info("Using Docker network type: " + props.get(MESOS_DOCKER_NETWORK_TYPE));
            scheduler.useDockerNetworkType(props.get(MESOS_DOCKER_NETWORK_TYPE));
        }
        if (props.containsKey(MESOS_CONTAINERIZER)) {
            LOGGER.info("Using containerizer: " + props.get(MESOS_CONTAINERIZER));
            scheduler.useContainerizer(props.get(MESOS_CONTAINERIZER));
        }
        if (props.containsKey(MESOS_DOCKER_MOUNT_VOLUME) &&
                TRUE.equals(props.get(MESOS_DOCKER_MOUNT_VOLUME))) {
            if (props.containsKey(MESOS_DOCKER_VOLUME_HOST_PATH) &&
                    props.containsKey(MESOS_DOCKER_VOLUME_CONTAINER_PATH)) {
                LOGGER.info("Will mount volume in Docker containers: " + props.get(MESOS_DOCKER_VOLUME_HOST_PATH) + ":" + props.get(MESOS_DOCKER_VOLUME_CONTAINER_PATH));
                scheduler.useDockerVolume(props.get(MESOS_DOCKER_VOLUME_HOST_PATH),
                                          props.get(MESOS_DOCKER_VOLUME_CONTAINER_PATH));
            } else {
                LOGGER.warn("Docker mount volume set to TRUE but no " +
                            MESOS_DOCKER_VOLUME_HOST_PATH + " and " +
                            MESOS_DOCKER_VOLUME_CONTAINER_PATH + " specified");
            }
        }
        if (props.containsKey(MESOS_AUTHENTICATE) && TRUE.equals(props.get(MESOS_AUTHENTICATE))) {
            LOGGER.info("Enabling authentication for the framework");

            if (!props.containsKey(MESOS_PRINCIPAL)) {
                LOGGER.error("Expecting authentication principal in the environment");
                throw new FrameworkException("Missing principal in mesos authentication");
            }
            if (!props.containsKey(MESOS_SECRET)) {
                LOGGER.error("Expecting authentication secret in the environment");
                throw new FrameworkException("Missing secret in mesos authentication");
            }
            Credential credential = Credential.newBuilder().setPrincipal(props.get(MESOS_PRINCIPAL))
                    .setSecret(props.get(MESOS_SECRET)).build();

            frameworkBuilder.setPrincipal(props.get(MESOS_PRINCIPAL));
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
        }
    }

    /**
     * @return Framework identifier returned by Mesos.
     */
    public String getId() {
        LOGGER.info("Get framework ID");
        return scheduler.getFrameworkId();
    }

    /**
     * Request a worker to be run on Mesos.
     *
     * @param appName
     * @param imageName
     * @param resources
     * @return Identifier assigned to new worker
     */
    public String requestWorker(String appName, String imageName, List<Resource> resources) {
        String name = appName + workerName;
        LOGGER.info("Requested a worker");
        return scheduler.requestWorker(driver, name, imageName, dockerCommand, resources);
    }

    /**
     * Wait for worker with identifier id.
     *
     * @param  id Worker identifier.
     * @return Worker IP address.
     */
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

    /**
     * Stop worker running/staging in Mesos.
     *
     * @param id
     */
    public void removeWorker(String id) {
        LOGGER.info("Remove worker with id " + id);
        try {
            scheduler.removeTask(driver, id, killWorkerTimeout, killWorkerTimeoutUnits);
        } catch (FrameworkException fe) {
            LOGGER.warn("Could not remove worker " + id);
            LOGGER.warn(fe);
        }
    }

    /**
     * Stop the Mesos Framework.
     */
    public void stop() {
        LOGGER.info("Stoping Mesos Framework");
        driver.stop();
    }

    private String getProperty(Map<String, String> props, String key, String defaultValue) {
        return props.containsKey(key) ? props.get(key) : defaultValue;
    }

}
