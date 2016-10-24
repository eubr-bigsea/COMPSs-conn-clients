package es.bsc.conn.docker.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import es.bsc.conn.loggers.Loggers;


/**
 * Synchronous DockerClient to create containers, remove them, etc. on a given host
 */
public class DockerClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.DOCKER);

    private static final int BYTES_IN_ONE_GB = 1024 * 1024 * 1024;
    private static final int MIN_CPU_SHARES = 2;
    private static final int MIN_MEMORY_SIZE_GB = 1;

    private com.github.dockerjava.api.DockerClient dockerClient;


    /**
     * This constructor is only called internally Use build method externally
     */
    private DockerClient() {
    }

    /**
     * Builds a DockerClient out of a DockerClientConfig.
     * 
     * @param config
     * @return
     */
    public static DockerClient build(DockerClientConfig config) {
        LOGGER.info("Creating DockerClient from build method");
        DockerClient dc = new DockerClient();
        dc.dockerClient = DockerClientBuilder.getInstance(config).build();

        return dc;
    }

    /**
     * Gets a list of all the containers in the host (either running, stopped, restarting, etc.)
     */
    public List<Container> getContainersList() {
        List<Container> l = new ArrayList<Container>();
        l.addAll(getStoppedContainersList());
        l.addAll(getRunningContainersList());
        l.addAll(dockerClient.listContainersCmd().withShowAll(true).withStatusFilter("restarting").exec());
        return l;
    }

    /**
     * Gets a list of all the running containers in the host
     */
    public List<Container> getRunningContainersList() {
        List<Container> l = new ArrayList<Container>();
        l.addAll(dockerClient.listContainersCmd().withShowAll(true).withStatusFilter("running").exec());
        return l;
    }

    /**
     * Gets a list of all the stopped containers in the host (this includes the "CREATED" containers but not started)
     */
    public List<Container> getStoppedContainersList() {
        List<Container> l = new ArrayList<Container>();
        l.addAll(dockerClient.listContainersCmd().withShowAll(true).withStatusFilter("created").exec());
        l.addAll(dockerClient.listContainersCmd().withShowAll(true).withStatusFilter("paused").exec());
        l.addAll(dockerClient.listContainersCmd().withShowAll(true).withStatusFilter("exited").exec());
        return l;
    }

    /**
     * Removes all the containers that are not being used (they are stopped). This can be useful if you want to make
     * sure that none of the stopped container names will collide with one you are creating right now.
     */
    public void removeStoppedContainers() {
        List<Container> containersList = getStoppedContainersList();
        for (Container c : containersList) {
            removeContainer(c.getId());
        }
    }

    /**
     * Forces the removal of all the containers (either running or stopped).
     */
    public void removeAllContainers() {
        List<Container> containersList = getStoppedContainersList();
        for (Container c : containersList) {
            removeContainer(c.getId());
        }
    }

    /**
     * Stops all the running containers.
     */
    public void stopAllContainers() {
        List<Container> runningContainers = getRunningContainersList();
        for (Container c : runningContainers) {
            stopContainer(c.getId());
        }
    }

    /**
     * Starts a previously created container.
     * 
     * @param The
     *            id of the container to be started
     */
    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
    }

    /**
     * Restarts a previously created container.
     * 
     * @param The
     *            id of the container to be restarted
     */
    public void restartContainer(String containerId) {
        dockerClient.restartContainerCmd(containerId).exec();
    }

    /**
     * Returns a data structure representing the inspect text that returns "docker inspect", of the container with id
     * containerId.
     * 
     * @param containerId
     * @return
     */
    public InspectContainerResponse inspectContainer(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).withContainerId(containerId).exec();
    }

    /**
     * Returns a data structure representing the network settings of the container with id containerId.
     * 
     * @param containerId
     * @return
     */
    public NetworkSettings getNetworkSettings(String containerId) {
        return inspectContainer(containerId).getNetworkSettings();
    }

    /**
     * Returns the RAM size in GB for the container with id containerId. Specifically, it returns the "Memory" field in
     * the docker inspect. IMPORTANT: this will be 0 if they aren't explicitly set by the user.
     */
    public int getMemoryGB(String containerId) {
        Long mem = inspectContainer(containerId).getHostConfig().getMemory();
        int memGB = (int) (mem / BYTES_IN_ONE_GB);
        return memGB;
    }

    /**
     * Returns the cpu shares for the container with id containerId. This is useful for Docker Swarm for example, where
     * cpu shares are the same as the number of processors used. IMPORTANT: this will be 0 if they aren't explicitly set
     * by the user.
     */
    public int getCpuShares(String containerId) {
        return inspectContainer(containerId).getHostConfig().getCpuShares();
    }

    /**
     * Returns the disk size for the container with id containerId. IMPORTANT: this will be 0 if they aren't explicitly
     * set by the user.
     */
    public int getDiskSize(String containerId) {
        Integer size = inspectContainer(containerId).getSizeRootFs();
        return size == null ? 0 : size.intValue();
    }

    /**
     * Returns the ip of the container with id containerId.
     * 
     * @param containerId
     * @return
     */
    public String getIpAddress(String containerId) {
        return inspectContainer(containerId).getNetworkSettings().getIpAddress();
    }

    /**
     * Creates a new container in the host, BUT DOES NOT START IT.
     * 
     * @param image
     *            The name of the image
     * @param containerName
     *            The name of the container, must be UNIQUE.
     * @param cmd
     *            The command of the container. IMPORTANT: If it contains separated words, they must go into separate
     *            argument strings. For example ("/bin/sleep", "1337").
     * @param exposedPorts
     *            The name of the container, must be UNIQUE.
     * @param reqCpuShares
     *            The requested cpu shares.
     * @param reqMemoryGB
     *            The requested memory of the container in GB.
     * @param reqDiskSizeGB
     *            The requested disk size in GB.
     * @return The created container's id.
     */
    public String createContainer(String image, String containerName, int[] exposedPorts, int reqCpuShares, float reqMemoryGB,
            String... cmd) {

        List<ExposedPort> exposedPortsList = new ArrayList<ExposedPort>();
        for (int p : exposedPorts) {
            exposedPortsList.add(new ExposedPort(p));
        }

        CreateContainerCmd ccc = dockerClient.createContainerCmd(image).withName(containerName).withCmd(cmd)
                .withCpuShares(Math.max(MIN_CPU_SHARES, reqCpuShares))
                .withMemory(Math.max(MIN_MEMORY_SIZE_GB, (long) (reqMemoryGB * BYTES_IN_ONE_GB))).withExposedPorts(exposedPortsList);

        CreateContainerResponse ccr = ccc.exec();
        return ccr.getId();
    }

    public String createContainer(String image, String containerName, int[] exposedPorts, String... cmd) {

        List<ExposedPort> exposedPortsList = new ArrayList<ExposedPort>();
        for (int p : exposedPorts) {
            exposedPortsList.add(new ExposedPort(p));
        }

        CreateContainerCmd ccc = dockerClient.createContainerCmd(image).withName(containerName).withCmd(cmd)
                .withExposedPorts(exposedPortsList);

        CreateContainerResponse ccr = ccc.exec();
        return ccr.getId();
    }

    /**
     * Creates a container without opening ports.
     */
    public String createContainer(String image, String containerName, String... cmd) {
        return createContainer(image, containerName, new int[] {}, cmd);
    }

    /**
     * Returns the first found container whose name is containerName.
     */
    public Container getContainerByName(String containerName) {
        List<Container> containers = getContainersList();
        for (Container c : containers) {
            String cName = c.getNames()[0].replace("/", ""); // Don't know why, names have a beginning '/'
            if (containerName.equals(cName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the container whose id is containerId. The id of a container is the long hash that Docker binds to it.
     */
    public Container getContainerById(String containerId) {
        List<Container> containers = getContainersList();
        for (Container c : containers) {
            if (containerId.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Stops a container.
     */
    public void stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
    }

    /**
     * Forces the removal of one container
     * 
     * @param containerName
     */
    public void removeContainer(String containerId) {
        RemoveContainerCmd rcc = dockerClient.removeContainerCmd(containerId).withForce(true);
        rcc.exec();
    }

    /**
     * Returns the internal DockerClient, which has extended functionalities. This can save you if you want to do more
     * complex stuff but can't/don't want to change this DockerClient class. You're welcome :)
     */
    public com.github.dockerjava.api.DockerClient getInternalDockerClient() {
        return dockerClient;
    }

}
