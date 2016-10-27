package es.bsc.conn.clients.vmm.types;

public class VMRequest {

    private final String name;
	private final String image;
    private final int cpus;
    private final int ramMb;
    private final int diskGb;
    private final String applicationId;
    private boolean needsFloatingIp = false;


    /**
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     */
    public VMRequest(String name, String image, int cpus, int ramMb, int diskGb, String applicationId, boolean needsFloatingIp) {
        this.name = name;
    	this.image = image;
        this.cpus = cpus;
        this.ramMb = ramMb;
        this.diskGb = diskGb;
        this.applicationId = applicationId;
        this.needsFloatingIp = needsFloatingIp;
    }

    public boolean needsFloatingIp(){
    	return needsFloatingIp;
    }
    
    public boolean getNeedsFloatingIp(){
    	return needsFloatingIp;
    }
    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @return the image
     */
    public String getImage() {
        return image;
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

}
