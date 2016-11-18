package es.bsc.conn.clients.vmm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import es.bsc.conn.clients.exceptions.ConnClientException;
import es.bsc.conn.clients.loggers.Loggers;
import es.bsc.conn.clients.vmm.types.VMDescription;
import es.bsc.conn.clients.vmm.types.VMRequest;
import es.bsc.conn.clients.vmm.types.VMs;

/**
 * VMM Client implementation
 * 
 */
public class VMMClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.VMM);
    private static final String ERROR_INCORRECT_RETURN = "Incorrect return code: ";
    
    private static final int MIN_CPUS = 1;
    private static final int MIN_RAM = 512;
    private static final int MIN_DISK = 1;

    private final Client client;
    private final WebResource resource;

    /**
     * Constructor
     * 
     * @param url
     */
    public VMMClient(String url) {
        super();
        this.client = new Client();
        this.resource = client.resource(url);

    }

    /**
     * Creates a VM with the given description
     * 
     * @param name
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     * @param applicationId
     * @param needsFloatingIp
     * @return
     * @throws ConnClientException
     */
    public String createVM(String name, String image, int cpus, int ramMb, int diskGb, String applicationId, boolean needsFloatingIp) 
            throws ConnClientException {
        
        // Check cpus, ram and disk parameters
        int usableCPUs = (cpus >= MIN_CPUS) ? cpus : MIN_CPUS;
        int usableRamMb = (ramMb >= MIN_RAM) ? ramMb : MIN_RAM;
        int usableDiskGb = (diskGb >= MIN_DISK) ? diskGb : MIN_DISK;
        
        // Create request
        VMRequest vm = new VMRequest(name, image, usableCPUs, usableRamMb, usableDiskGb, applicationId, needsFloatingIp);
        VMs vms = new VMs();
        vms.addVM(vm);
        
        JSONObject obj = new JSONObject(vms);
        LOGGER.debug("Submitting vm creation ... \n"+obj.toString());
        ClientResponse cr = resource.path("vms").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, obj.toString());
        if (cr.getStatus() == Status.OK.getStatusCode()) {
            String id = null;
            try {
                String s = cr.getEntity(String.class);
                JSONObject res = new JSONObject(s);
                id = (String) res.getJSONArray("ids").getJSONObject(0).get("id");
            } catch (JSONException je) {
                throw new ConnClientException(je);
            }
                
            LOGGER.debug("VM submitted with id " + id);
            return id;
        } else {
            String msg = ERROR_INCORRECT_RETURN + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase();
            LOGGER.error(msg);
            throw new ConnClientException(msg);
        }
    }

    /**
     * Returns the description of a given vmId
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public VMDescription getVMDescription(String id) throws ConnClientException {
        LOGGER.debug("Getting vm description ...");
        ClientResponse cr = resource.path("vms").path(id).get(ClientResponse.class);
        if (cr.getStatus() == Status.OK.getStatusCode()) {
            String s = cr.getEntity(String.class);
            LOGGER.debug("Obtained description " + s);
            try {
                return new VMDescription(new JSONObject(s));
            } catch (JSONException je) {
                throw new ConnClientException(je);
            }
        } else {
            String msg = ERROR_INCORRECT_RETURN + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase();
            LOGGER.error(msg);
            throw new ConnClientException(msg);
        }
    }

    /**
     * Deletes a given vmId
     * @param vmId
     * @throws Exception
     */
    public void deleteVM(String vmId) throws ConnClientException {
        LOGGER.debug("Getting vm destruction ...");

        ClientResponse cr = resource.path("vms").path(vmId).delete(ClientResponse.class);
        if (cr.getStatus() != Status.NO_CONTENT.getStatusCode()) {
            String msg = ERROR_INCORRECT_RETURN + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase();
            LOGGER.error(msg);
            throw new ConnClientException(msg);
        }
    }

}
