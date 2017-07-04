package es.bsc.conn.clients.vmm.types;

/**
 * Represents a VM Request
 * 
 */
public class VMRequest {

    private final String name;
    private final String image;
    private final int cpus;
    private final int ramMb;
    private final int diskGb;
    private final String applicationId;
    private final String preferredHost;
    private boolean needsFloatingIp = false;


    /**
     * Instantiate a new VM Request
     * 
     * @param name
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     * @param applicationId
     * @param preferredHost
     * @param needsFloatingIp
     */
    public VMRequest(String name, String image, int cpus, int ramMb, int diskGb, String applicationId, String preferredHost,
            boolean needsFloatingIp) {

        this.name = name;
        this.image = image;
        this.cpus = cpus;
        this.ramMb = ramMb;
        this.diskGb = diskGb;
        this.applicationId = applicationId;
        this.preferredHost = preferredHost;
        this.needsFloatingIp = needsFloatingIp;
    }

    /**
     * Returns if the VM needs a floating IP or not
     * 
     * @return
     */
    public boolean needsFloatingIp() {
        return needsFloatingIp;
    }

    /**
     * @return the image
     */
    public String getImage() {
        return image;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @return the cpus
     */
    public int getCpus() {
        return cpus;
    }

    /**
     * @return the ramMb
     */
    public int getRamMb() {
        return ramMb;
    }

    /**
     * @return the diskGb
     */
    public int getDiskGb() {
        return diskGb;
    }

    /**
     * @return the preferredHost
     */
    public String getPreferredHost() {
        return preferredHost;
    }

}
