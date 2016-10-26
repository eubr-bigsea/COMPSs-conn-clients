package es.bsc.conn.clients.rocci;

import es.bsc.conn.clients.exceptions.ConnClientException;
import es.bsc.conn.clients.loggers.Loggers;
import es.bsc.conn.clients.rocci.types.json.JSONResources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class RocciClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.ROCCI);

    private String cmdLine = "";
    private String attributes = "";


    public RocciClient(List<String> cmd_string, String attr) {
        LOGGER.info("Initializing RocciClient");
        for (String s : cmd_string) {
            cmdLine += s + " ";
        }
        attributes = attr;
    }

    public String describeResource(String resourceId) throws ConnClientException {
        String resDesc = "";
        String cmd = cmdLine + "--action describe" + " --resource " + resourceId;

        try {
            LOGGER.debug("Describe CMD: " + cmd);
            resDesc = executeCmd(cmd);
        } catch (InterruptedException e) {
            LOGGER.error("Error on Describe CMD", e);
        }
        return resDesc;
    }

    public String getResourceStatus(String resourceId) throws ConnClientException {
        LOGGER.debug("Get Status from Resource " + resourceId);
        String resStatus = null;
        String jsonOutput = describeResource(resourceId);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        jsonOutput = "{\"resources\":" + jsonOutput + "}";

        // convert the json string back to object
        JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);
        resStatus = obj.getResources().get(0).getAttributes().getOcci().getCompute().getState();

        return resStatus;
    }

    public String getResourceAddress(String resourceId) throws ConnClientException {
        LOGGER.debug("Get Address from Resource " + resourceId);
        String resIP = null;
        String jsonOutput = describeResource(resourceId);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        jsonOutput = "{\"resources\":" + jsonOutput + "}";

        // convert the json string back to object
        JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);

        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); i++) {
            if (obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface() != null) {
                resIP = obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface().getAddress();
                break;
            }
        }
        return resIP;
    }
    
    public Object[] getHardwareDescription(String resourceId) throws ConnClientException {
        LOGGER.debug("Get Hardware description from Resource " + resourceId);
        
        String jsonOutput = describeResource(resourceId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        jsonOutput = "{\"resources\":" + jsonOutput + "}";

        // Convert the json string back to object
        JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);
        
        // Retrieve hardware information
        // Memory
        Float memory = null; 
        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); ++i) {
            if (obj.getResources().get(0).getAttributes().getOcci().getCompute().getMemory() != null) {
                memory = obj.getResources().get(0).getAttributes().getOcci().getCompute().getMemory();
                break;
            }
        }
        
        // Storage
        Float storage = null;
        
        // Cores
        Integer cores = null;
        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); ++i) {
            if (obj.getResources().get(0).getAttributes().getOcci().getCompute().getCores() != null) {
                cores = obj.getResources().get(0).getAttributes().getOcci().getCompute().getCores();
                break;
            }
        }
        
        // Architecture
        String architecture = null;
        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); ++i) {
            if (obj.getResources().get(0).getAttributes().getOcci().getCompute().getArchitecture() != null) {
                architecture = obj.getResources().get(0).getAttributes().getOcci().getCompute().getArchitecture();
                break;
            }
        }
        
        // Speed
        Float speed = null;
        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); ++i) {
            if (obj.getResources().get(0).getAttributes().getOcci().getCompute().getSpeed() != null) {
                speed = obj.getResources().get(0).getAttributes().getOcci().getCompute().getSpeed();
                break;
            }
        }

        // Create return hardware description of the form [memSize, storageSize, cores, architecture, speed]
        return new Object[] { memory, storage, cores, architecture, speed };
    }

    public void deleteCompute(String resourceId) {
        String cmd = cmdLine + "--action delete" + " --resource " + resourceId;
        try {
            executeCmd(cmd);
        } catch (ConnClientException | InterruptedException e) {
            LOGGER.error("Cannot delete resource with id " + resourceId, e);
        }
    }

    public String createCompute(String osTPL, String resourceTPL) {
        String s = "";

        String cmd = cmdLine + " --action create" + " --resource compute -M os_tpl#" + osTPL + " -M resource_tpl#" + resourceTPL
                + " --attribute occi.core.title=\"" + attributes + "\"";

        try {
            LOGGER.debug("Create CMD: " + cmd);
            s = executeCmd(cmd);
        } catch (ConnClientException | InterruptedException e) {
            LOGGER.error("Error on Create CMD", e);
        }

        return s;
    }

    private String executeCmd(String cmdArgs) throws ConnClientException, InterruptedException {
        String returnSTR = "";
        String[] cmd = { "/bin/bash", "-c", "occi " + cmdArgs };
        try {
            LOGGER.info("Execute CMD: " + Arrays.toString(cmd));
            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError1 = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            LOGGER.debug("Execute CMD Output:");
            String s1;
            while ((s1 = stdInput1.readLine()) != null) {
                LOGGER.debug(s1);
                returnSTR += s1;
            }

            // Read any errors from the attempted command
            LOGGER.error("Execute CMD Error:");
            while ((s1 = stdError1.readLine()) != null) {
                LOGGER.error(s1);
            }

            p.waitFor();
            LOGGER.info("Excute CMD exitValue: " + p.exitValue());
            LOGGER.info("__________________________________________");
            return returnSTR;
        } catch (IOException e) {
            throw new ConnClientException(e);
        }
    }

}
