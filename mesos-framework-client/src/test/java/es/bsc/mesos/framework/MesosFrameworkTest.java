package es.bsc.mesos.framework;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;


public class MesosFrameworkTest {

    // Test Logger
    private static final Logger logger = LogManager.getLogger("Console");


    private Value.Scalar buildScalar(double value) {
        return Value.Scalar.newBuilder().setValue(value).build();
    }

    private Resource buildResource(String name, double value) {
        return Resource.newBuilder().setName(name).setType(Value.Type.SCALAR).setScalar(buildScalar(value)).build();
    }

    @Test
    public void testFramework() throws Exception {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("Server", "localhost:5050");

        MesosFramework mf = new MesosFramework(properties);

        logger.info("FrameworkID " + mf.getId());
        Thread.sleep(5_000);
        
        // Create resources
        List<Resource> resources = new LinkedList<Resource>();
        resources.add(buildResource("cpus", 1.2));
        resources.add(buildResource("mem", 2048));
        resources.add(buildResource("disk", 4096));
        List<Resource> resources2 = new LinkedList<Resource>();
        resources2.add(buildResource("cpus", 2.2));
        resources2.add(buildResource("mem", 3072));
        resources2.add(buildResource("disk", 8192));

        // Request workers information
        String idWorker = mf.requestWorker(resources);
        String idWorker2 = mf.requestWorker(resources);
        String idWorker3 = mf.requestWorker(resources2);

        String ip = mf.waitWorkerUntilRunning(idWorker);
        String ip2 = mf.waitWorkerUntilRunning(idWorker2);
        String ip3 = mf.waitWorkerUntilRunning(idWorker3);
        
        logger.debug("Worker1 IP: " + ip);
        logger.debug("Worker2 IP: " + ip2);
        logger.debug("Worker3 IP: " + ip3);
        
        // Remove workers
        Thread.sleep(5_000);
        
        mf.removeWorker(idWorker);
        mf.removeWorker(idWorker2);
        mf.removeWorker(idWorker3);
        
        // Stop 
        Thread.sleep(20_000);

        mf.stop();
    }

}
