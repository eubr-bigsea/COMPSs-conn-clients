package es.bsc.conn.clients.slurm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;


public class JobDescriptionTest {

    private static final String JOB_DESCR = "JobId=754209 JobName=bash UserId=bsc19611(7014) "
            + "GroupId=bsc19(2950) Priority=102 Nice=0 Account=bsc19 QOS=bsc_cs "
            + "JobState=RUNNING Reason=None Dependency=(null) Requeue=1 Restarts=0 " + "BatchFlag=0 Reboot=0 ExitCode=0:0 RunTime=00:13:53 "
            + "TimeLimit=2-00:00:00 TimeMin=N/A SubmitTime=2017-05-19T12:07:19 "
            + "EligibleTime=2017-05-19T12:07:19 StartTime=2017-05-19T12:07:21 "
            + "EndTime=2017-05-21T12:07:21 PreemptTime=None SuspendTime=None "
            + "SecsPreSuspend=0 Partition=projects AllocNode:Sid=nvblogin1:19380 "
            + "ReqNodeList=(null) ExcNodeList=(null) NodeList=nva[59-60],nvb[10,13-15] BatchHost=nva59 "
            + "NumNodes=2 NumCPUs=2 CPUs/Task=1 ReqB:S:C:T=0:0:*:* " + "TRES=cpu=2,mem=4000,node=2 Socks/Node=* NtasksPerN:B:S:C=0:0:*:* "
            + "CoreSpec=* MinCPUsNode=1 MinMemoryCPU=2000M MinTmpDiskNode=0 "
            + "Features=(null) Gres=gpu:2 Reservation=(null) Shared=OK Contiguous=0 " + "Licenses=(null) Network=(null) Command=(null) "
            + "WorkDir=/gpfs/home/bsc19/bsc19611 Power= SICP=0";

    private static final String[] NODES = new String[] { "nva59", "nva60", "nvb10", "nvb13", "nvb14", "nvb15" };
    private static final String REQUEST = "-N3 --cpus_per_task=2 -n4 --mem=8000 --gres=gpu:1";


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testJDFromOutput() {
        JobDescription jobDesc = new JobDescription(JOB_DESCR);
        assertEquals(jobDesc.getNodeList().size(), NODES.length);
        Set<String> ns = new HashSet<>();

        for (String n : NODES) {
            ns.add(n);
        }

        for (String assignedNode : jobDesc.getNodeList()) {
            assertTrue(ns.contains(assignedNode));
        }

        assertEquals(jobDesc.getProperty("NumCPUs"), "2");
        assertEquals(jobDesc.getProperty("NumNodes"), "2");
        assertEquals(jobDesc.getProperty("mem"), "4000");
        assertEquals(jobDesc.getProperty("Gres"), "gpu:2");

    }

    @Test
    public void testGenerateRequest() {
        Map<String, String> req = new HashMap<>();
        req.put(JobDescription.NUM_NODES, "3");
        req.put(JobDescription.NUM_CPUS, "8");
        req.put(JobDescription.CPUS_TASK, "2");
        req.put(JobDescription.MEM, "8000");
        req.put(JobDescription.GRES, "gpu:1");

        JobDescription jobDesc = new JobDescription(req);
        System.out.println(jobDesc.generateRequest());
        assertEquals(jobDesc.generateRequest(), REQUEST);
        assertEquals(jobDesc.getProperty("NumCPUs"), "8");
        assertEquals(jobDesc.getProperty("NumNodes"), "3");
        assertEquals(jobDesc.getProperty("mem"), "8000");
        assertEquals(jobDesc.getProperty("Gres"), "gpu:1");

    }

    @Test
    public void testParseNodeList0() {
        String nodeListStr = "nvb22";
        Set<String> ns = new HashSet<>();
        ns.add("nvb22");
        List<String> nodeList = new LinkedList<>();
        JobDescription.parseNodelist(nodeListStr, nodeList);
        assertEquals(ns.size(), nodeList.size());
        for (String assignedNode : nodeList) {
            assertTrue(ns.contains(assignedNode));
        }

    }

    @Test
    public void testParseNodeList1() {
        String nodeListStr = "nvb[3,8-9]";
        Set<String> ns = new HashSet<>();
        ns.add("nvb3");
        ns.add("nvb8");
        ns.add("nvb9");
        List<String> nodeList = new LinkedList<>();
        JobDescription.parseNodelist(nodeListStr, nodeList);

        assertEquals(ns.size(), nodeList.size());
        for (String assignedNode : nodeList) {
            assertTrue(ns.contains(assignedNode));
        }

    }

    @Test
    public void testParseNodeList2() {
        String nodeListStr = "nvb[8-9]";
        Set<String> ns = new HashSet<>();
        ns.add("nvb8");
        ns.add("nvb9");
        List<String> nodeList = new LinkedList<>();
        JobDescription.parseNodelist(nodeListStr, nodeList);

        assertEquals(ns.size(), nodeList.size());
        for (String assignedNode : nodeList) {
            assertTrue(ns.contains(assignedNode));
        }

    }

    @Test
    public void testParseNodeList3() {
        String nodeListStr = "nvb[3,5]";
        Set<String> ns = new HashSet<>();
        ns.add("nvb3");
        ns.add("nvb5");
        List<String> nodeList = new LinkedList<>();
        JobDescription.parseNodelist(nodeListStr, nodeList);

        assertEquals(ns.size(), nodeList.size());
        for (String assignedNode : nodeList) {
            assertTrue(ns.contains(assignedNode));
        }

    }

    @Test
    public void testParseNodeList4() {
        String nodeListStr = "nvb[3-5,7]";
        Set<String> ns = new HashSet<>();
        ns.add("nvb3");
        ns.add("nvb4");
        ns.add("nvb5");
        ns.add("nvb7");
        List<String> nodeList = new LinkedList<>();
        JobDescription.parseNodelist(nodeListStr, nodeList);

        assertEquals(ns.size(), nodeList.size());
        for (String assignedNode : nodeList) {
            assertTrue(ns.contains(assignedNode));
        }
    }

}
