package es.bsc.conn.clients.vmm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

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
    public String createVM(String image, int cpus, int ramMb, int diskGb) throws Exception {
        VMRequest vm = new VMRequest(image, cpus, ramMb, diskGb);
        VMs vms = new VMs();
        vms.addVM(vm);
        
        JSONObject obj = new JSONObject(vms);
        LOGGER.debug("Submitting vm creation ...");
        ClientResponse cr = resource.path("vms").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, obj.toString());
        if (cr.getStatus() == Status.OK.getStatusCode()) {
            String s = cr.getEntity(String.class);
            JSONObject res = new JSONObject(s);
            String id = (String) res.getJSONArray("ids").getJSONObject(0).get("id");
            LOGGER.debug("VM submitted with id " + id);
            return id;
        } else {
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw (new Exception("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase()));
        }
    }

    /**
     * Returns the description of a given vmId
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public VMDescription getVMDescription(String id) throws Exception {
        LOGGER.debug("Getting vm description ...");
        ClientResponse cr = resource.path("vms").path(id).get(ClientResponse.class);
        if (cr.getStatus() == Status.OK.getStatusCode()) {
            String s = cr.getEntity(String.class);
            LOGGER.debug("Obtained description " + s);
            return new VMDescription(new JSONObject(s));

        } else {
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw (new Exception("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase()));
        }
    }

    /*
     * public void stopVM(String vmId) throws Exception { LOGGER.debug("Getting vm description ..."); JSONObject res =
     * new JSONObject(); res = res.put("action", "stop"); ClientResponse cr =
     * resource.path("vms").path(vmId).put(ClientResponse.class, res.toString()); if (cr.getStatus() !=
     * Status.NO_CONTENT.getStatusCode()){
     * LOGGER.error("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase());
     * throw(new
     * Exception("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase())); }
     * 
     * }
     */

    /**
     * Deletes a given vmId
     * @param vmId
     * @throws Exception
     */
    public void deleteVM(String vmId) throws Exception {
        LOGGER.debug("Getting vm description ...");

        ClientResponse cr = resource.path("vms").path(vmId).delete(ClientResponse.class);
        if (cr.getStatus() != Status.NO_CONTENT.getStatusCode()) {
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw (new Exception("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase()));
        }
    }

}
