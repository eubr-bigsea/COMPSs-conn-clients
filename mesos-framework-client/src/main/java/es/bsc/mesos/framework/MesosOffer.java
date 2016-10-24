package es.bsc.mesos.framework;

import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Offer;

import java.util.List;
import java.util.LinkedList;


public class MesosOffer {

    private static final int CPUS_WEIGHT = 3;
    private static final int MEM_WEIGHT = 2;
    private static final int DISK_WEIGHT = 1;

    private double cpus;
    private double mem;
    private double disk;
    private List<Range> ports;
    private Offer offer;


    public MesosOffer() {
        cpus = 0.0;
        mem = 0.0;
        disk = 0.0;
        ports = new LinkedList<Range>();
    }

    public MesosOffer(Offer offer) {
        this();
        this.offer = offer;
        countResources(offer.getResourcesList());
    }

    public MesosOffer(List<Resource> resources) {
        this();
        countResources(resources);
    }

    public double getCpus() {
        return cpus;
    }

    public double getMem() {
        return mem;
    }

    public double getDisk() {
        return disk;
    }

    public Offer getOffer() {
        return offer;
    }

    private void countResources(List<Resource> resources) {
        for (Resource resource : resources) {
            switch (resource.getName()) {
                case "cpus":
                    cpus += resource.getScalar().getValue();
                    break;
                case "mem":
                    mem = resource.getScalar().getValue();
                    break;
                case "disk":
                    disk = resource.getScalar().getValue();
                    break;
                case "ports":
                    ports.addAll(resource.getRanges().getRangeList());
                    break;
                default:
                    // Nothing
            }
        }
    }

    public String toString(Range r) {
        return String.format("[%d-%d]", r.getBegin(), r.getEnd());
    }

    private Range buildRange(long begin, long end) {
        return Range.newBuilder().setBegin(begin).setEnd(end).build();
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        for (Range r : ports) {
            bld.append(r);
        }
        String portsList = bld.toString();
        return String.format("Offer: {cpus: %.2f, mem: %.2f, disk: %.2f, ports: %s}", cpus, mem, disk, portsList);
    }

    public boolean hasEnoughPorts(int minPorts) {
        int portsCount = 0;
        for (Range portsRange : this.ports) {
            portsCount += (int) (portsRange.getEnd() - portsRange.getBegin()) + 1;
        }
        return portsCount >= minPorts;
    }

    public List<Range> getMinPorts(int minPorts) {
        List<Range> minPortsList = new LinkedList<Range>();
        int addedPorts = 0;
        for (int i = 0; i < this.ports.size(); i++) {
            Range r = this.ports.get(i);
            int range = (int) (r.getEnd() - r.getBegin());
            if (addedPorts + range < minPorts) {
                addedPorts += range;
                minPortsList.add(r);
                this.ports.remove(i);
            } else {
                long end = r.getBegin() + (minPorts - addedPorts) - 1;
                minPortsList.add(buildRange(r.getBegin(), end));
                this.ports.remove(i);
                this.ports.add(i, buildRange(end + 1, r.getEnd()));
                break;
            }
        }
        return minPortsList;
    }

    public double distance(MesosOffer offer) {
        double cpusScore = ((offer.cpus - this.cpus) / this.cpus) * CPUS_WEIGHT;
        double memScore = ((offer.mem - this.mem) / this.mem) * MEM_WEIGHT;
        double diskScore = ((offer.disk - this.disk) / this.disk) * DISK_WEIGHT;

        if (cpusScore < 0.0 || memScore < 0.0 || diskScore < 0.0) {
            return -1.0;
        }
        return cpusScore + memScore + diskScore;
    }

    public void removeResourcesFrom(MesosOffer offer) {
        this.cpus = Math.max(this.cpus - offer.getCpus(), 0.0);
        this.mem = Math.max(this.mem - offer.getMem(), 0.0);
        this.disk = Math.max(this.disk - offer.getDisk(), 0.0);
    }
}
