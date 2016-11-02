package es.bsc.conn.clients.vmm.types;

import java.util.ArrayList;
import java.util.List;


public class VMs {

    private final ArrayList<VMRequest> activeVMs = new ArrayList<>();


    public VMs() {
        // The attributes are already initialized statically
    }

    public List<VMRequest> getVms() {
        return activeVMs;
    }

    public void addVM(VMRequest vm) {
        this.activeVMs.add(vm);
    }

}
