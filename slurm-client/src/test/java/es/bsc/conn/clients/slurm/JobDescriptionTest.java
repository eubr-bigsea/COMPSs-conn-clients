package es.bsc.conn.clients.slurm;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class JobDescriptionTest {
	
	private static final String jd = "JobId=754209 JobName=bash UserId=bsc19611(7014) "
			+ "GroupId=bsc19(2950) Priority=102 Nice=0 Account=bsc19 QOS=bsc_cs "
			+ "JobState=RUNNING Reason=None Dependency=(null) Requeue=1 Restarts=0 "
			+ "BatchFlag=0 Reboot=0 ExitCode=0:0 RunTime=00:13:53 "
			+ "TimeLimit=2-00:00:00 TimeMin=N/A SubmitTime=2017-05-19T12:07:19 "
			+ "EligibleTime=2017-05-19T12:07:19 StartTime=2017-05-19T12:07:21 "
			+ "EndTime=2017-05-21T12:07:21 PreemptTime=None SuspendTime=None "
			+ "SecsPreSuspend=0 Partition=projects AllocNode:Sid=nvblogin1:19380 "
			+ "ReqNodeList=(null) ExcNodeList=(null) NodeList=nva[59-60],nvb[10,13-15] BatchHost=nva59 "
			+ "NumNodes=2 NumCPUs=2 CPUs/Task=1 ReqB:S:C:T=0:0:*:* "
			+ "TRES=cpu=2,mem=4000,node=2 Socks/Node=* NtasksPerN:B:S:C=0:0:*:* "
			+ "CoreSpec=* MinCPUsNode=1 MinMemoryCPU=2000M MinTmpDiskNode=0 "
			+ "Features=(null) Gres=gpu:2 Reservation=(null) Shared=OK Contiguous=0 "
			+ "Licenses=(null) Network=(null) Command=(null) "
			+ "WorkDir=/gpfs/home/bsc19/bsc19611 Power= SICP=0";
	
	private static final String[] nodes = new String[]{"nva59","nva60","nvb10","nvb13","nvb14", "nvb15"};
	private static final String request = "-N3 --cpus_per_task=2 -n4 --mem=8000 --gres=gpu:1";
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testJDFromOutput() {
		JobDescription jobDesc = new JobDescription(jd);
		assertEquals(jobDesc.getNodeList().size(),nodes.length);
		Set<String> ns = new HashSet<String>();
		
		for (String n:nodes){
			ns.add(n);
		}
		
		for (String assignedNode: jobDesc.getNodeList()){
			assertTrue(ns.contains(assignedNode));
		}
		
		assertEquals(jobDesc.getProperty("NumCPUs"),"2");
		assertEquals(jobDesc.getProperty("NumNodes"),"2");
		assertEquals(jobDesc.getProperty("mem"),"4000");
		assertEquals(jobDesc.getProperty("Gres"),"gpu:2");
		
	}
	
	@Test
	public void testGenerateRequest() {
		HashMap<String,String> req = new HashMap();
		req.put(JobDescription.NUM_NODES, "3");
		req.put(JobDescription.NUM_CPUS, "8");
		req.put(JobDescription.CPUS_TASK, "2");
		req.put(JobDescription.MEM, "8000");
		req.put(JobDescription.GRES, "gpu:1");
		
		JobDescription jobDesc = new JobDescription(req);
		System.out.println(jobDesc.generateRequest());
		assertEquals(jobDesc.generateRequest(),request);
		assertEquals(jobDesc.getProperty("NumCPUs"),"8");
		assertEquals(jobDesc.getProperty("NumNodes"),"3");
		assertEquals(jobDesc.getProperty("mem"),"8000");
		assertEquals(jobDesc.getProperty("Gres"),"gpu:1");
		
	}

}
