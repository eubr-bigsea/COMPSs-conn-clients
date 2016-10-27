package es.bsc.conn.clients.vmm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;

import es.bsc.conn.clients.vmm.types.VMDescription;


/**
 * Unit test for ROCCI Client
 *
 */
public class VMMClientTest {

    private static final Logger LOGGER = LogManager.getLogger("Console");


    /**
     * Create the test
     */

    @Before
    public void BeforeTest() {
        LOGGER.info("Starting tests");
    }

    @Test
    public void vmmClientTest() throws Exception {
        VMMClient client = new VMMClient("http://bscgrid28.bsc.es:34372/api/v1/");
        String id = client.createVM("test_jorge","576c3c9a-f301-471a-9de5-a06e0e0e0960", 2, 2048, 10, "myApp",true);
        assertNotNull(id);
        LOGGER.info("VM created with id: "+ id);
        VMDescription vmd = client.getVMDescription(id);
        assertNotNull(vmd);
        LOGGER.info("VM "+ id + " description [status: " + vmd.getState() +"hostname: "+ vmd.getHostName()+ "ipaddress: "+ vmd.getIpAddress());
        assertEquals(2, vmd.getCpus());
        assertEquals(2048, vmd.getRamMb());
        assertEquals(10, vmd.getDiskGb());
        assertEquals("576c3c9a-f301-471a-9de5-a06e0e0e0960", vmd.getImage());
        client.deleteVM(id);
    }
}
