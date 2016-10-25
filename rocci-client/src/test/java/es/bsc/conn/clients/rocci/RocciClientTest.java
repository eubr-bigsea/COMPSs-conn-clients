package es.bsc.conn.clients.rocci;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.Test;


/**
 * Unit test for ROCCI Client
 *
 */
public class RocciClientTest {

    private static final Logger LOGGER = LogManager.getLogger("Console");


    /**
     * Create the test
     */
    public RocciClientTest() {
        LOGGER.info("Creating test instance");
    }

    @Before
    public void BeforeTest() {
        LOGGER.info("Starting tests");
    }

    @Test
    public void rocciClientTest() throws Exception {
        LOGGER.info("Test passed");
    }
}
