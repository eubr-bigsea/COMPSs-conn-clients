package es.bsc.conn.vmm.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import es.bsc.conn.loggers.Loggers;
import es.bsc.conn.vmm.types.VMDescription;
import es.bsc.conn.vmm.types.VMRequest;
import es.bsc.conn.vmm.types.VMs;


public class VMMClient {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.VMM);

    private Client client;
    private WebResource resource;


    /**
     * 
     */
    public VMMClient(String url) {
        super();
        this.client = new Client();
        this.resource = client.resource(url);

    }

    public String createVM(String name, String image, int cpus, int ramMb, int diskGb, String applicationId) throws Exception {
        VMRequest vm = new VMRequest(name, image, cpus, ramMb, diskGb, applicationId);
        VMs vms = new VMs();
        vms.getVms().add(vm);
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

    public void deleteVM(String vmId) throws Exception {
        LOGGER.debug("Getting vm description ...");

        ClientResponse cr = resource.path("vms").path(vmId).delete(ClientResponse.class);
        if (cr.getStatus() != Status.NO_CONTENT.getStatusCode()) {
            LOGGER.error("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase());
            throw (new Exception("Incorrect return code: " + cr.getStatus() + "." + cr.getClientResponseStatus().getReasonPhrase()));
        }
    }

}
