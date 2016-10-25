package es.bsc.conn.clients.vmm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.Test;


/**
 * Unit test for ROCCI Client
 *
 */
public class VMMClientTest {

    private static final Logger LOGGER = LogManager.getLogger("Console");


    /**
     * Create the test
     */
    public VMMClientTest() {
        LOGGER.info("Creating test instance");
    }

    @Before
    public void BeforeTest() {
        LOGGER.info("Starting tests");
    }

    @Test
    public void vmmClientTest() throws Exception {
        LOGGER.info("Test passed");
    }
}
