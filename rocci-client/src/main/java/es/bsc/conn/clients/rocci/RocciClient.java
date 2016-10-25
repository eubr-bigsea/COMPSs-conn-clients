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

    private String cmd_line = "";
    private String attributes = "";


    public RocciClient(List<String> cmd_string, String attr) {
        LOGGER.info("Initializing RocciClient");
        for (String s : cmd_string) {
            cmd_line += s + " ";
        }
        attributes = attr;
    }

    public String describe_resource(String resource_id) throws ConnClientException {
        String res_desc = "";
        String cmd = cmd_line + "--action describe" + " --resource " + resource_id;

        try {
            LOGGER.debug("Describe CMD: " + cmd);
            res_desc = execute_cmd(cmd);
        } catch (InterruptedException e) {
            LOGGER.error("Error on Describe CMD", e);
        }
        return res_desc;
    }

    public String get_resource_status(String resource_id) throws ConnClientException {
        LOGGER.debug("Get Status from Resource " + resource_id);
        String res_status = null;
        String jsonOutput = describe_resource(resource_id);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        jsonOutput = "{\"resources\":" + jsonOutput + "}";

        // convert the json string back to object
        JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);
        res_status = obj.getResources().get(0).getAttributes().getOcci().getCompute().getState();

        return res_status;
    }

    public String get_resource_address(String resource_id) throws ConnClientException {
        LOGGER.debug("Get Address from Resource " + resource_id);
        String res_ip = null;
        String jsonOutput = describe_resource(resource_id);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        jsonOutput = "{\"resources\":" + jsonOutput + "}";

        // convert the json string back to object
        JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);

        for (int i = 0; i < obj.getResources().get(0).getLinks().size(); i++) {
            if (obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface() != null) {
                res_ip = obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface().getAddress();
                break;
            }
        }
        return res_ip;
    }

    public void delete_compute(String resource_id) {
        String cmd = cmd_line + "--action delete" + " --resource " + resource_id;
        try {
            execute_cmd(cmd);
        } catch (ConnClientException | InterruptedException e) {
            LOGGER.error("Cannot delete resource with id " + resource_id, e);
            System.out.println(e);
        } catch (Exception e) {
            LOGGER.error("Cannot delete resource with id " + resource_id, e);
        }
    }

    public String create_compute(String os_tpl, String resource_tpl) {
        String s = "";

        String cmd = cmd_line + " --action create" + " --resource compute -M os_tpl#" + os_tpl + " -M resource_tpl#" + resource_tpl
                + " --attribute occi.core.title=\"" + attributes + "\"";

        try {
            LOGGER.debug("Create CMD: " + cmd);
            s = execute_cmd(cmd);
        } catch (ConnClientException | InterruptedException e) {
            LOGGER.error("Error on Create CMD", e);
        }

        return s;
    }

    private String execute_cmd(String cmd_args) throws ConnClientException, InterruptedException {
        String return_string = "";
        String[] cmd_line = { "/bin/bash", "-c", "occi " + cmd_args };
        try {
            LOGGER.info("Execute CMD: " + Arrays.toString(cmd_line));
            Process p = Runtime.getRuntime().exec(cmd_line);

            BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError1 = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            LOGGER.debug("Execute CMD Output:");
            String s1 = null;
            while ((s1 = stdInput1.readLine()) != null) {
                LOGGER.debug(s1);
                return_string += s1;
            }

            // read any errors from the attempted command
            LOGGER.error("Execute CMD Error:");
            while ((s1 = stdError1.readLine()) != null) {
                LOGGER.error(s1);
            }

            p.waitFor();
            LOGGER.info("Excute CMD exitValue: " + p.exitValue());
            LOGGER.debug("__________________________________________");
            return return_string;
        } catch (IOException e) {
            throw new ConnClientException(e);
        }
    }

}
