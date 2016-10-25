package es.bsc.conn.clients.vmm.types;

import java.util.ArrayList;


public class VMs {

    private final ArrayList<VMRequest> vms = new ArrayList<VMRequest>();


    public VMs() {
    }

    public ArrayList<VMRequest> getVms() {
        return vms;
    }

    public void addVM(VMRequest vm) {
        this.vms.add(vm);
    }

}
