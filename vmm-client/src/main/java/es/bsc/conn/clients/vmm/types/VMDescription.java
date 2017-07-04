package es.bsc.conn.clients.vmm.types;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Description of a VM
 *
 */
public class VMDescription extends VMRequest {

    private final String id;
    private final String state;
    private final String ipAddress;
    private final String hostName;


    /**
     * Instantiate a new VM Description
     * 
     * @param id
     * @param name
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     * @param applicationId
     * @param preferredHost
     * @param needsFloatingIp
     * @param state
     * @param ipAddress
     * @param hostName
     */
    public VMDescription(String id, String name, String image, int cpus, int ramMb, int diskGb, String applicationId, String preferredHost,
            boolean needsFloatingIp, String state, String ipAddress, String hostName) {

        super(name, image, cpus, ramMb, diskGb, applicationId, preferredHost, needsFloatingIp);

        this.id = id;
        this.state = state;
        this.ipAddress = ipAddress;
        this.hostName = hostName;
    }

    /**
     * 
     * @param jsonObject
     * @throws JSONException
     */
    public VMDescription(JSONObject jsonObject) throws JSONException {
        super((String) jsonObject.get("name"), (String) jsonObject.get("image"), (int) jsonObject.get("cpus"),
                (int) jsonObject.get("ramMb"), (int) jsonObject.get("diskGb"), (String) jsonObject.get("applicationId"), "",
                (boolean) jsonObject.get("needsFloatingIp"));

        this.id = (String) jsonObject.get("id");
        this.state = (String) jsonObject.get("state");
        this.ipAddress = (String) jsonObject.get("ipAddress");
        this.hostName = (String) jsonObject.get("hostName");
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

}
