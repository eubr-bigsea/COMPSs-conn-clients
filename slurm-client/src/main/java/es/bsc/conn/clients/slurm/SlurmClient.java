package es.bsc.conn.clients.slurm;

import es.bsc.conn.clients.exceptions.ConnClientException;
import es.bsc.conn.clients.loggers.Loggers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * SLURM Client version 4.2.5
 *
 */
public class SlurmClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.SLURM);
    private static final String EXPECTED_RESULT = "Submitted batch job ";
    private static final String SLURM_CMD = "[Client] Slurm CMD: ";
    private static final String ERROR_SLURM_CMD = "[Client] Error on Slurm CMD";

    private final String mainJobId;
    private final String masterId;
    private Map<String, String> nodeToJobId = new HashMap<>();
    private Map<String, List<String>> jobIdToNodes = new HashMap<>();
    private final int initialNodes;
    private final boolean ssh;
    private final boolean expand;
    


    /**
     * Instantiates a new SLURM Client with a default CMD and the given attributes
     * 
     * @param masterId
     */
    public SlurmClient(String masterId, boolean ssh, boolean expand) {
        
        this.masterId = masterId;
        this.ssh = ssh;
        this.expand = expand;
        LOGGER.info("[Client] Initializing SLURM Client ("+ this.masterId + this.ssh + this.expand+")");
        List<String> nodeIds = parseNodes();
        this.initialNodes = nodeIds.size();
        
        this.mainJobId = System.getenv("SLURM_JOB_ID");
        if (mainJobId != null && nodeIds != null && !nodeIds.isEmpty()) {
            jobIdToNodes.put(mainJobId, nodeIds);
            for (String nodeId : nodeIds) {
                nodeToJobId.put(nodeId, mainJobId);
            }
        } else {
            LOGGER.error("[Client] ERROR no SLURM_JOB_ID defined SLURM client will not work");
        }
    }

    /**
     * Returns the number of initial nodes
     * 
     * @return the initialNodes
     */
    public int getInitialNodes() {
        return initialNodes;
    }

    private List<String> parseNodes() {
        String slurmNL = System.getenv("SLURM_JOB_NODELIST");
        LinkedList<String> list = new LinkedList<>();
        if (slurmNL != null) {
            JobDescription.parseNodelist(slurmNL, list);
        }

        return list;
    }

    /**
     * Returns the description of the Job with id @resourceId
     * 
     * @param jobId
     * @return
     * @throws ConnClientException
     */
    public JobDescription getJobDescription(String jobId) throws ConnClientException {
        String cmd = "scontrol show JobId=" + jobId;
        try {
            LOGGER.debug(SLURM_CMD + cmd);
            return new JobDescription(executeCmd(cmd));
        } catch (ConnClientException ie) {
            LOGGER.error(ERROR_SLURM_CMD, ie);
            throw new ConnClientException(ie);
        }

    }

    /**
     * Returns the job status
     * 
     * @param jobId
     * @return
     * @throws ConnClientException
     */
    public String getJobStatus(String jobId) throws ConnClientException {
        String cmd = "sacct -j" + jobId + "-n -P -o status ";
        try {
            LOGGER.debug(SLURM_CMD + cmd);
            return executeCmd(cmd);
        } catch (ConnClientException ie) {
            LOGGER.error(ERROR_SLURM_CMD, ie);
            throw new ConnClientException(ie);
        }

    }

    /**
     * Cancels a job
     * 
     * @param jobId
     * @throws ConnClientException
     */
    public void cancelJob(String jobId) throws ConnClientException {
        String cmd = "scancel " + jobId;
        try {
            LOGGER.debug(SLURM_CMD + cmd);
            executeCmd(cmd);
        } catch (ConnClientException ie) {
            LOGGER.error(ERROR_SLURM_CMD, ie);
            throw new ConnClientException(ie);
        }

    }
    
    public void updateJob(String jobId, String args){
    	String cmd = "scontrol update JobId="+ jobId + " " + args ;
		try {
			LOGGER.debug(SLURM_CMD + cmd);
			executeCmd(cmd);
		} catch (ConnClientException e) {
			LOGGER.warn(ERROR_SLURM_CMD , e);
		}
    }

    /**
     * Deletes a Node with id @resourceId
     *
     * @param resourceId
     * @throws ConnClientException
     */
    public void deleteCompute(String resourceId) throws ConnClientException {
        if (!resourceId.equals(masterId)) {
            String nodeJobId = nodeToJobId.get(resourceId);
            // Remove node from node to JobId list
            nodeToJobId.remove(resourceId);
            // Getting updated nodelist
            String args = "NodeList=" + masterId;
            Set<String> nodeList = nodeToJobId.keySet();
            int nodes = 1;
            for (String node : nodeList) {
                if (!node.equals(masterId)) {
                    args = args.concat("," + node);
                    nodes++;
                }
            }
            args = args.concat(" NumNodes=" + nodes);
            updateJob(mainJobId, args);
            if (nodeJobId != null) {
                List<String> nodesInJob = jobIdToNodes.get(nodeJobId);
                nodesInJob.remove(resourceId);
                if (nodesInJob.isEmpty()) {
                    jobIdToNodes.remove(nodeJobId);
                    try {
                        cancelJob(nodeJobId);
                    } catch (ConnClientException e) {
                        LOGGER.warn("Cannot cancel job " + nodeJobId + " from resource " + resourceId, e);
                    }
                } else {
                    LOGGER.warn("Job " + nodeJobId + " is not empty. Skiping job cancel ");
                }
            } else {
                LOGGER.warn("JobId for resource " + resourceId + " does not exist");
            }
        } else {
            LOGGER.warn("Trying to remove master node. It is not allowed");
        }

    }

    /**
     * Creates a compute
     * 
     * @param jobDesc
     * @param script
     * @return
     * @throws ConnClientException
     */
    public String createCompute(JobDescription jobDesc, String script) throws ConnClientException {
    	String cmd = null;
    	if(expand){
        	cmd = "sbatch --dependency=expand:" + mainJobId + " " + jobDesc.generateRequest() + " " + script;
        }else{
        	cmd = "sbatch "+ jobDesc.generateRequest() + " " + script;
        }
        LOGGER.debug(SLURM_CMD + cmd);
        return parseJobIDFormCreationOutput(executeCmd(cmd));
    }

    private String parseJobIDFormCreationOutput(String executeResult) throws ConnClientException {
        if (executeResult.startsWith(EXPECTED_RESULT)) {
            return executeResult.substring(EXPECTED_RESULT.length());
        } else {
            throw new ConnClientException("Results is not starting as expected. Current: \"" + executeResult + "\" does not starts with \""
                    + EXPECTED_RESULT + "\"");
        }

    }

    /**
     * Adds a new node to the main pool
     * 
     * @param jobId
     * @param jdesc
     * @throws ConnClientException
     */
    public void addNodesToMain(String jobId, JobDescription jdesc) throws ConnClientException {
        List<String> nodeIds = jdesc.getNodeList();
        jobIdToNodes.put(jobId, nodeIds);
        for (String nodeId : nodeIds) {
            nodeToJobId.put(nodeId, jobId);
        }
        String args = "NodeList=" + masterId;
        Set<String> nodeList = nodeToJobId.keySet();
        int nodes = 1;
        for (String node : nodeList) {
            if (!node.equals(masterId)) {
                args = args.concat("," + node);
                nodes++;
            }
        }
        args = args.concat(" NumNodes=" + nodes);
        updateJob(mainJobId, args);

    }

    private String executeCmd(String cmdToRun) throws ConnClientException {
        try {
        	String cmd ;
        	if (ssh){
        		cmd = "ssh "+ masterId + " " + cmdToRun;
        	}else{
        		cmd = cmdToRun;
        	}
            LOGGER.info("[Client] Execute CMD ("+ssh+"): " + cmd);
            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError1 = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            LOGGER.debug("[Client] Execute CMD Output:");
            String line;
            StringBuilder cmdResult = new StringBuilder();
            while ((line = stdInput1.readLine()) != null) {
                LOGGER.debug(line);
                cmdResult.append(line);
            }

            // Read any errors from the attempted command
            LOGGER.error("[Client] Execute CMD Error:");
            while ((line = stdError1.readLine()) != null) {
                LOGGER.error(line);
            }

            p.waitFor();

            LOGGER.info("[Client] Excute CMD exitValue: " + p.exitValue());
            LOGGER.info("__________________________________________");

            return cmdResult.toString();
        } catch (IOException | InterruptedException e) {
            throw new ConnClientException(e);
        }
    }

}
