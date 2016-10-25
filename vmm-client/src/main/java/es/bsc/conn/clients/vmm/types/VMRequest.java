package es.bsc.conn.clients.vmm.types;

public class VMRequest {

    private final String image;
    private final int cpus;
    private final int ramMb;
    private final int diskGb;


    /**
     * @param image
     * @param cpus
     * @param ramMb
     * @param diskGb
     */
    public VMRequest(String image, int cpus, int ramMb, int diskGb) {
        this.image = image;
        this.cpus = cpus;
        this.ramMb = ramMb;
        this.diskGb = diskGb;
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
