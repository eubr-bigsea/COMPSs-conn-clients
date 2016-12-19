package es.bsc.mesos.framework;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;

import es.bsc.conn.clients.mesos.framework.MesosFramework;


public class MesosFrameworkTest {

    // Test Logger
    private static final Logger logger = LogManager.getLogger("Console");


    private Value.Range buildRange(long begin, long end) {
        return Value.Range.newBuilder().setBegin(begin).setEnd(end).build();
    }

    private Value.Scalar buildScalar(double value) {
        return Value.Scalar.newBuilder().setValue(value).build();
    }

    private Resource buildResource(String name, Value.Ranges ranges) {
        return Resource.newBuilder().setName(name).setType(Value.Type.RANGES).setRanges(ranges).build();
    }

    private Resource buildResource(String name, double value) {
        return Resource.newBuilder().setName(name).setType(Value.Type.SCALAR).setScalar(buildScalar(value)).build();
    }

    @Test
    public void testFramework() throws Exception {
        HashMap<String, String> properties = new HashMap<String, String>();
        // properties.put("Server", "localhost:5050");
        properties.put("Server", "zk://192.168.99.100:2181/mesos");
        properties.put("mesos-docker-network", "compss-net");

        String dockerImage = "compss/compss:2.0";
        String appName = "test.Test";

        MesosFramework mf = new MesosFramework(properties);

        logger.info("FrameworkID " + mf.getId());
        Thread.sleep(5_000);

        // Create resources
        Value.Ranges ports = Value.Ranges.newBuilder()
                .addRange(buildRange(22L, 22L))
                .addRange(buildRange(43000L, 43005L)).build();

        List<Resource> resources = new LinkedList<Resource>();
        resources.add(buildResource("cpus", 0.8));
        resources.add(buildResource("mem", 256));
        resources.add(buildResource("disk", 256));
        resources.add(buildResource("ports", ports));
        List<Resource> resources2 = new LinkedList<Resource>();
        resources2.add(buildResource("cpus", 0.6));
        resources2.add(buildResource("mem", 380));
        resources2.add(buildResource("disk", 256));
        resources2.add(buildResource("ports", ports));

        // Request workers information
        String idWorker = mf.requestWorker(appName, dockerImage, resources);
        String idWorker2 = mf.requestWorker(appName, dockerImage, resources);
        String idWorker3 = mf.requestWorker(appName, dockerImage, resources2);

        String ip = mf.waitWorkerUntilRunning(idWorker);
        String ip2 = mf.waitWorkerUntilRunning(idWorker2);

        logger.debug("Worker1 IP: " + ip);
        logger.debug("Worker2 IP: " + ip2);

        Thread.sleep(10_000);
        // Remove workers
        mf.removeWorker(idWorker);

        String ip3 = mf.waitWorkerUntilRunning(idWorker3);
        logger.debug("Worker3 IP: " + ip3);

        Thread.sleep(10_000);
        mf.removeWorker(idWorker2);
        mf.removeWorker(idWorker3);

        // Stop
        Thread.sleep(20_000);

        mf.stop();
    }

}
