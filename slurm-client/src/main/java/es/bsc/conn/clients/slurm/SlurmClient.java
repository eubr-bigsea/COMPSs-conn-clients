package es.bsc.conn.clients.slurm;

import es.bsc.conn.clients.exceptions.ConnClientException;
import es.bsc.conn.clients.loggers.Loggers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * ROCCI Client version 4.2.5
 *
 */
public class SlurmClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.SLURM);
    private static final String EXPECTED_RESULT = "Submitted batch job ";
    private final String mainJobId, masterId;
    private HashMap<String,String> nodeToJobId = new HashMap();
    private HashMap<String,List<String>> jobIdToNodes = new HashMap(); 


    /**
     * Instantiates a new ROCCI Client with a default CMD and the given attributes
     *
     * @param cmdString
     * @param attr
     */
    public SlurmClient(List<String> nodeIds, String masterId) {
        LOGGER.info("Initializing SLURM Client");
        this.masterId = masterId;
        mainJobId = System.getenv("SLURM_JOB_ID");
        if (mainJobId!=null && nodeIds != null && !nodeIds.isEmpty()){
        	jobIdToNodes.put(mainJobId, nodeIds);
        	for (String nodeId: nodeIds){
        		nodeToJobId.put(nodeId, mainJobId);
        	}
        }else{
        	LOGGER.error("ERROR no SLURM_JOB_ID defined SLURM client will not work");
        }
    }

    /**
     * Returns the description of the VM with id @resourceId
     *
     * @param resourceId
     * @return
     * @throws ConnClientException
     */
    public JobDescription getJobDescription(String jobId) throws ConnClientException {
        String cmd = "scontrol show JobId=" + jobId ;
        try {
            LOGGER.debug("Describe CMD: " + cmd);
            return new JobDescription(executeCmd(cmd));
        } catch (ConnClientException ie) {
            LOGGER.error("Error on Describe CMD", ie);
            throw new ConnClientException(ie);
        }
        
    }
    
    public String getJobStatus(String jobId) throws ConnClientException {
        String cmd = "sacct -j" + jobId + "-n -P -o status " ;
        try {
            LOGGER.debug("Describe CMD: " + cmd);
            return executeCmd(cmd);
        } catch (ConnClientException ie) {
            LOGGER.error("Error on Describe CMD", ie);
            throw new ConnClientException(ie);
        }
        
    }

   

    

    /**
     * Deletes a Node with id @resourceId
     *
     * @param resourceId
     * @throws ConnClientException 
     */
    public void deleteCompute(String resourceId) throws ConnClientException {
    	if (!resourceId.equals(masterId)){
    		String nodeJobId = nodeToJobId.get(resourceId);
    		String cancelJob="";
    		if (nodeJobId!=null){
    			List<String> nodesInJob= jobIdToNodes.get(nodeJobId);
    			if (nodesInJob.size()==1){
    				cancelJob = "scancel "+ nodeJobId;
    			}
    		}
    		String args = "NodeList="+ masterId;
    		Set<String> nodeList = nodeToJobId.keySet();
    		for (String node: nodeList){
    			if (!node.equals(resourceId)){
    				args= args.concat(","+node);
    			}
    		}
    		String cmd = "scontrol update JobId="+ mainJobId + " " + args + "; " + cancelJob ;
    		try {
    			executeCmd(cmd);
    			if (nodeJobId!=null){
    				jobIdToNodes.get(nodeJobId).remove(resourceId);
    				if (!cancelJob.isEmpty()){
    					jobIdToNodes.remove(nodeJobId);
    				}
    			}
    			nodeToJobId.remove(resourceId);
    			
    		} catch (ConnClientException e) {
    			LOGGER.error("Cannot delete resource with id " + resourceId, e);
    			throw e;
    		}
    	}else{
    		LOGGER.warn("Trying to remove master node. It is not allowed"); 
    	}
        
    }

    public String createCompute(JobDescription jobDesc, String script) throws ConnClientException{
        String cmd = "sbatch --dependency=expand:"+mainJobId + " " 
        			+ jobDesc.generateRequest()+ " " + script;
            LOGGER.debug("Create CMD: " + cmd);
            return parseJobIDFormCreationOutput(executeCmd(cmd));
    }
    
    private String parseJobIDFormCreationOutput(String executeResult) throws ConnClientException {
		if (executeResult.startsWith(EXPECTED_RESULT)){
			return executeResult.substring(EXPECTED_RESULT.length());
		}else{
			throw new ConnClientException("Results is not starting as expected. Current: \"" 
					+ executeResult + "\" does not starts with \"" +EXPECTED_RESULT+"\"");
		}
    
	}

	public void addNodesToMain(String jobId, JobDescription jdesc) throws ConnClientException{
    	String cmd = "scontrol update JobId="+mainJobId +" NumNodes=ALL NumCPUS=ALL";
        LOGGER.debug("update job CMD: " + cmd);
        executeCmd(cmd);
        jobIdToNodes.put(jobId, jdesc.getNodeList());
    }


    private String executeCmd(String cmd) throws ConnClientException {
        try {
            LOGGER.info("Execute CMD: " + cmd);
            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError1 = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            LOGGER.debug("Execute CMD Output:");
            String line;
            StringBuilder cmdResult = new StringBuilder();
            while ((line = stdInput1.readLine()) != null) {
                LOGGER.debug(line);
                cmdResult.append(line);
            }

            // Read any errors from the attempted command
            LOGGER.error("Execute CMD Error:");
            while ((line = stdError1.readLine()) != null) {
                LOGGER.error(line);
            }

            p.waitFor();

            LOGGER.info("Excute CMD exitValue: " + p.exitValue());
            LOGGER.info("__________________________________________");

            return cmdResult.toString();
        } catch (IOException | InterruptedException e) {
            throw new ConnClientException(e);
        }
    }

}
