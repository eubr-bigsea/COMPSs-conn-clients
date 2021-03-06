package es.bsc.conn.clients.rocci;

import es.bsc.conn.clients.exceptions.ConnClientException;
import es.bsc.conn.clients.loggers.Loggers;
import es.bsc.conn.clients.rocci.types.json.JSONResources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * ROCCI Client version 4.2.5
 *
 */
public class RocciClient {

	private static final Logger LOGGER = LogManager.getLogger(Loggers.ROCCI);

	private static final String JSON_RESOURCES_OPEN = "{\"resources\":";
	private static final String JSON_RESOURCES_CLOSE = "}";

	// Flags for occi commands (care spaces)
	private static final String FLAG_ACTION = "--action ";
	private static final String FLAG_RESOURCE = "--resource ";

	private final String cmdLine; // String with the necessary information for any other operation
	private final String attributes;

	/**
	 * Instantiates a new ROCCI Client with a default CMD and the given attributes
	 *
	 * @param cmdString
	 * @param attr
	 */
	public RocciClient(List<String> cmdString, String attr) {
		LOGGER.info("Initializing RocciClient");
		StringBuilder sb = new StringBuilder();
		for (String s : cmdString) {
			sb.append(s).append(" ");
		}
		cmdLine = sb.toString();
		LOGGER.debug("cmdLine       : " + cmdLine);
		attributes = attr;
	}

	/**
	 * Returns the description of the VM with id @resourceId
	 *
	 * @param resourceId
	 * @return
	 * @throws ConnClientException
	 */
	public String describeResource(String resourceId) throws ConnClientException {
		String resDesc = "";
		String cmd = cmdLine + FLAG_ACTION + "describe " + FLAG_RESOURCE + resourceId;

		try {
			LOGGER.debug("Describe CMD: " + cmd);
			resDesc = executeCmd(cmd);
		} catch (ConnClientException ie) {
			LOGGER.error("Error on Describe CMD", ie);
			throw new ConnClientException(ie);
		}
		return resDesc;
	}

	/**
	 * Returns the status of the VM with id @resourceId
	 *
	 * @param resourceId
	 * @return
	 * @throws ConnClientException
	 */
	public String getResourceStatus(String resourceId) throws ConnClientException {
		LOGGER.debug("Get Status from Resource " + resourceId);
		String jsonOutput = describeResource(resourceId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		jsonOutput = JSON_RESOURCES_OPEN + jsonOutput + JSON_RESOURCES_CLOSE;

		// Convert the json string back to object
		JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);

		// Get state
		return obj.getResources().get(0).getAttributes().getOcci().getCompute().getState();
	}

	/**
	 * Returns the IP addresses of the VM with id @resourceId
	 *
	 * @param resourceId
	 * @return
	 * @throws ConnClientException
	 */
	public String[] getResourceAddress(String resourceId) throws ConnClientException {
		LOGGER.debug("Get Address from Resource " + resourceId);
		ArrayList<String> resIPs = new ArrayList<String>();
		String jsonOutput = describeResource(resourceId);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		jsonOutput = JSON_RESOURCES_OPEN + jsonOutput + JSON_RESOURCES_CLOSE;

		// convert the json string back to object
		JSONResources obj = gson.fromJson(jsonOutput, JSONResources.class);

		for (int i = 0; i < obj.getResources().get(0).getLinks().size(); i++) {
			if (obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface() != null) {
				resIPs.add(obj.getResources().get(0).getLinks().get(i).getAttributes().getOcci().getNetworkinterface()
						.getAddress());
			}
		}
		String[] resultIPs = new String[resIPs.size()];
		for (int i = 0; i < resIPs.size(); i++) {
			resultIPs[i] = resIPs.get(i);
		}

		return resultIPs;
	}

	/**
	 * Returns the hardware description of the VM with id @resourceId
	 *
	 * @param resourceId
	 * @return
	 * @throws ConnClientException
	 */
	public Object[] getHardwareDescription(String resourceId) throws ConnClientException {
		LOGGER.debug("Get Hardware description from Resource " + resourceId);

		String jsonOutput = describeResource(resourceId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		jsonOutput = JSON_RESOURCES_OPEN + jsonOutput + JSON_RESOURCES_CLOSE;

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

		// Create return hardware description of the form [memSize, storageSize, cores,
		// architecture, speed]
		return new Object[] { memory, storage, cores, architecture, speed };
	}

	/**
	 * Deletes the VM with id @resourceId
	 *
	 * @param resourceId
	 */
	public void deleteCompute(String resourceId) {
		String cmd = cmdLine + FLAG_ACTION + "delete " + FLAG_RESOURCE + resourceId;
		try {
			executeCmd(cmd);
		} catch (ConnClientException e) {
			LOGGER.error("Cannot delete resource with id " + resourceId, e);
		}
	}

	/**
	 * Creates a new VM with the given @osTPL and @resourceTPL and returns its id
	 *
	 * @param osTPL
	 * @param resourceTPL
	 * @return
	 */
	public String createCompute(String osTPL, String resourceTPL) throws ConnClientException {
		String s = "";

		String cmd = cmdLine + " " + FLAG_ACTION + "create " + "--resource compute -M os_tpl#" + osTPL
				+ " -M resource_tpl#" + resourceTPL + " --attribute occi.core.title=\"" + attributes + "\"";

		LOGGER.debug("Create CMD: " + cmd);
		s = executeCmd(cmd);
		return s;
	}

	/**
	 * Example: occi --endpoint https://extcloud05.ebi.ac.uk:8787/occi1.2/ --auth
	 * token --token $OS_TOKEN --resource [compute link] --action link --link
	 * [network link]
	 * 
	 * @param resourceId
	 * @param link
	 */
	public void attachLink(String resourceId, String link) {
		// Get only the necessary flags from cmdLine
		String[] parts = cmdLine.split(" ");
		String endpoint = "";
		String auth = "";
		String token = "";
		String psw = "";
		String caPath = "";
		String userCred = "";
		for (int i = 0; i < parts.length; i++) {
			switch (parts[i]) {
			case "--endpoint":
				endpoint = parts[i + 1];
				break;
			case "--auth":
				auth = parts[i + 1];
				break;
			case "--token":
				token = parts[i + 1];
				break;
			case "--password":
				psw = parts[i + 1];
				break;
			case "--ca-path":
				caPath = parts[i + 1];
				break;
			case "--user-cred":
				userCred = parts[i + 1];
				break;
			default:
				LOGGER.warn("Unrecognised tag: " + parts[i] + ". Skipping");
			}
		}
		String authInfo;
		if ("token".equals(auth)) {
			authInfo = " --auth " + auth + " --token " + token;
		} else {
			authInfo = " --auth " + auth + " --password " + psw + " --ca-path " + caPath + " --user-cred " + userCred;
		}
		String cmd = "occi --endpoint " + endpoint + authInfo + " --resource " + resourceId + " --action link --link "
				+ link;
		try {
			executeCmd(cmd);
		} catch (ConnClientException e) {
			LOGGER.error("Cannot attach link " + link + " to resource with id " + resourceId, e);
		}
	}

	private String executeCmd(String cmdArgs) throws ConnClientException {
		String[] cmd = { "/bin/bash", "-c", "occi " + cmdArgs };
		try {
			LOGGER.info("Execute CMD: " + Arrays.toString(cmd));
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
			StringBuilder errorMsg = new StringBuilder();
			while ((line = stdError1.readLine()) != null) {
				LOGGER.error(line);
				errorMsg.append(line);
			}

			p.waitFor();

			LOGGER.info("Execute CMD exitValue: " + p.exitValue());
			LOGGER.info("__________________________________________");

			if (p.exitValue() != 0) {
				throw new ConnClientException(errorMsg.toString());
			}
			return cmdResult.toString();
		} catch (IOException | InterruptedException e) {
			throw new ConnClientException(e);
		}
	}

}
