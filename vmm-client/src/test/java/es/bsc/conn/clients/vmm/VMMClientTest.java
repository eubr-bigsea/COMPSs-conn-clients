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
	//private static final String VMM_URL = "http://bscgrid28.bsc.es:34372/api/v1/";
	//private static final String IMAGE_ID = "576c3c9a-f301-471a-9de5-a06e0e0e0960";
	private static final String VMM_URL = "http://bscgrid28.bsc.es:34371/api/v1/";
	private static final String IMAGE_ID = "edd82938-16f1-45f3-830f-1ee839e8e220";
	private static final int CPU = 4;
	private static final int MEM = 4096;
	private static final int DISK = 5;
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
        VMMClient client = new VMMClient(VMM_URL);
        String id = client.createVM("test",IMAGE_ID, CPU, MEM, DISK, "application","juno1" ,true);
        assertNotNull(id);
        LOGGER.info("VM created with id: " + id);
        
        VMDescription vmd = client.getVMDescription(id);
        assertNotNull(vmd);
        LOGGER.info("VM " + id + " description [status: " + vmd.getState() + "hostname: " + vmd.getHostName() + "ipaddress: "
                + vmd.getIpAddress());
        
        assertEquals(CPU, vmd.getCpus());
        assertEquals(MEM, vmd.getRamMb());
        assertEquals(DISK, vmd.getDiskGb());
        assertEquals(IMAGE_ID, vmd.getImage());
        //client.deleteVM(id);
    }
}
