package es.bsc.conn.clients.docker;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

import es.bsc.conn.clients.docker.DockerClient;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.Test;


/**
 * Unit test for Docker Client. To test this, please open a local Docker daemon in port 2375 (tcp://localhost:2375)
 */
public class DockerClientTest {

    private static final Logger LOGGER = LogManager.getLogger("Console");

    private static final String[] containerNames = { "testContainer1", "testContainer2", "testContainer3" };

    private static DockerClient dc;


    /**
     * Create the test. In this case, create the DockerClient
     */
    public DockerClientTest() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("tcp://localhost:2375")
                .withDockerTlsVerify(false).build();

        dc = DockerClient.build(config);
    }

    private boolean containsId(List<Container> containers, String id) {
        for (Container c : containers) {
            if (c.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Before
    public void BeforeTest() {
        LOGGER.info("Starting test...");
        LOGGER.info("Removing containers from previous tests... (this can take a while)");
        for (String cName : containerNames) {
            Container c = dc.getContainerByName(cName);
            if (c != null) {
                dc.removeContainer(c.getId());
            }
        }
    }

    private void assertRunning(String containerId) {
        assertTrue(containsId(dc.getContainersList(), containerId));
        assertFalse(containsId(dc.getStoppedContainersList(), containerId));
        assertTrue(containsId(dc.getRunningContainersList(), containerId));
    }

    private void assertStopped(String containerId) {
        assertTrue(containsId(dc.getContainersList(), containerId));
        assertTrue(containsId(dc.getStoppedContainersList(), containerId));
        assertFalse(containsId(dc.getRunningContainersList(), containerId));
    }

    private void assertRemoved(String containerId) {
        assertFalse(containsId(dc.getContainersList(), containerId));
    }

    @Test
    public void createDockerContainer() throws Exception {
        LOGGER.info("Creating test containers...");
        String containerId1 = dc.createContainer("compss/compss", containerNames[0], "/bin/sleep", "10");
        String containerId2 = dc.createContainer("compss/compss", containerNames[1], "/bin/sleep", "999");
        String containerId3 = dc.createContainer("compss/compss", containerNames[2], "/bin/sleep", "999");
        assertStopped(containerId1);
        assertStopped(containerId2);
        assertStopped(containerId3);
        LOGGER.info("Containters created!");
        LOGGER.info("");

        LOGGER.info("Starting containers...");
        dc.startContainer(containerId1);
        dc.startContainer(containerId2);
        dc.startContainer(containerId3);
        assertRunning(containerId1);
        assertRunning(containerId2);
        assertRunning(containerId3);
        LOGGER.info("Containers started!");
        LOGGER.info("");

        LOGGER.info("Sleeping...");
        Thread.sleep(15_000);
        LOGGER.info("");

        LOGGER.info("Checking that container 1 is stopped because it has finished...");
        assertStopped(containerId1);
        assertRunning(containerId2);
        assertRunning(containerId3);
        LOGGER.info("");

        LOGGER.info("Removing all stopped containers...");
        dc.removeStoppedContainers();

        LOGGER.info("Checking that container 1 has been removed...");
        assertRemoved(containerId1);
        assertRunning(containerId2);
        assertRunning(containerId3);
        LOGGER.info("");

        LOGGER.info("Stopping container 3...");
        dc.stopContainer(containerId3);
        LOGGER.info("Checking that container 3 has been stopped...");
        assertRemoved(containerId1);
        assertRunning(containerId2);
        assertStopped(containerId3);
        LOGGER.info("");

        LOGGER.info("Removing container 3...");
        dc.removeContainer(containerId3);

        LOGGER.info("Checking that container 3 has been removed...");
        assertRemoved(containerId1);
        assertRunning(containerId2);
        assertRemoved(containerId3);
        LOGGER.info("");

        LOGGER.info("TEST PASSED :) !!!");
    }
}
