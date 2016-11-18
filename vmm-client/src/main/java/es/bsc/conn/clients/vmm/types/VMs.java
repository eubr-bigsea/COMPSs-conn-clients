package es.bsc.conn.clients.vmm.types;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents all active VMs within the connector
 * 
 */
public class VMs {

    private final ArrayList<VMRequest> activeVMs = new ArrayList<>();


    /**
     * Instantiate a new VMs representation
     */
    public VMs() {
        // The attributes are already initialized statically
    }

    /**
     * Returns the list of active VMs
     * 
     * @return
     */
    public List<VMRequest> getVms() {
        return activeVMs;
    }

    /**
     * Adds a new active vm @vm
     * 
     * @param vm
     */
    public void addVM(VMRequest vm) {
        this.activeVMs.add(vm);
    }

}
