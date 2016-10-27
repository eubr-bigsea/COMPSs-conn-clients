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


public class VMMClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.VMM);

    private final Client client;
    private final WebResource resource;


    /**
     * Constructor
     */
    public VMMClient(String url) {
        super();
        this.client = new Client();
        this.resource = client.resource(url);

    }

    /**
     * Creates a VM with the given description
     * 
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     * @return the vmId
     * @throws Exception
     */
    public String createVM(String name, String image, int cpus, int ramMb, int diskGb, String applicationId, boolean needsFloatingIp) throws Exception {
        VMRequest vm = new VMRequest(name, image, cpus, ramMb, diskGb, applicationId, needsFloatingIp);
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
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw new ConnClientException("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
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
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw new ConnClientException("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
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
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw new ConnClientException("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
        }
    }

}
