package es.bsc.conn.clients.mesos.framework;

import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Offer;

import java.util.List;
import java.util.LinkedList;


public class MesosOffer {

    // Constants
    private static final int CPUS_WEIGHT = 3;
    private static final int MEM_WEIGHT = 2;
    private static final int DISK_WEIGHT = 1;

    private static final String CPUS_RESOURCE = "cpus";
    private static final String MEM_RESOURCE = "mem";
    private static final String DISK_RESOURCE = "disk";
    private static final String PORTS_RESOURCE = "ports";

    // Resources
    private double cpus;
    private double mem;
    private double disk;
    private List<Range> ports;
    private Offer offer;

    /**
     * Empty Mesos offer.
     */
    public MesosOffer() {
        cpus = 0.0;
        mem = 0.0;
        disk = 0.0;
        ports = new LinkedList<>();
        offer = null;
    }

    /**
     * Creates a Mesos offer with resources from offer.
     *
     * @param  offer Offer received from Mesos. It contains resources available to use.
     */
    public MesosOffer(Offer offer) {
        this();
        this.offer = offer;
        countResources(offer.getResourcesList());
    }

    /**
     * Creates a Mesos offer from a list of Resources.
     *
     * @param  resources List of resources (cpus, mem, disk, ports).
     */
    public MesosOffer(List<Resource> resources) {
        this();
        countResources(resources);
    }

    /**
     * @return MesosOffer number of CPUs.
     */
    public double getCpus() {
        return cpus;
    }

    /**
     * @return MesosOffer memory capacity in MBytes.
     */
    public double getMem() {
        return mem;
    }

    /**
     * @return MesosOffer disk capacity in MBytes.
     */
    public double getDisk() {
        return disk;
    }

    /**
     * @return Offer if exists, otherwise null.
     */
    public Offer getOffer() {
        return offer;
    }

    /**
     * @param  r
     * @return Range to String.
     */
    public String rangeToString(Range r) {
        return String.format("(%d-%d)", r.getBegin(), r.getEnd());
    }

    /**
     * @return MesosOffer to String.
     */
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        for (Range r : ports) {
            bld.append(rangeToString(r));
        }
        String portsList = "[" + bld.toString() + "]";
        return String.format("Offer: {cpus: %.2f, mem: %.2f, disk: %.2f, ports: %s}", cpus, mem, disk, portsList);
    }

    /**
     * Counts the number of ports available in the offer.
     *
     * @return Number of ports.
     */
    public int getNumPorts() {
        int portsCount = 0;
        for (Range portsRange: this.ports) {
            portsCount += (int) (portsRange.getEnd() - portsRange.getBegin()) + 1;
        }
        return portsCount;
    }

    /**
     * @return List of ports ranges.
     */
    public List<Range> getPortsList() {
        return ports;
    }

    /**
     * @param  minPorts Number of ports to compare.
     * @return          True if offer has minPorts or more.
     */
    public boolean hasEnoughPorts(int minPorts) {
        return this.getNumPorts() >= minPorts;
    }

    /**
     * Gets minPorts from offer ports.
     *
     * @param  minPorts Number of ports to pick.
     * @return          List of ports Ranges that have a total of minPorts.
     */
    public List<Range> getMinPorts(int minPorts) {
        List<Range> minPortsList = new LinkedList<>();
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

    /**
     * Calculates the distance between self and offer. If offer has not enough resources returns
     * -1. If it has enough, returns a double. The more closer to 0, the more similar is the offer.
     *
     * @param  offer Offer to compare to.
     * @return       Distance between the two offers.
     */
    public double distance(MesosOffer offer) {
        double cpusScore = ((offer.cpus - this.cpus) / this.cpus) * CPUS_WEIGHT;
        double memScore = ((offer.mem - this.mem) / this.mem) * MEM_WEIGHT;
        double diskScore = ((offer.disk - this.disk) / this.disk) * DISK_WEIGHT;

        if (cpusScore < 0.0 || memScore < 0.0 || diskScore < 0.0) {
            return -1.0;
        }
        return cpusScore + memScore + diskScore;
    }

    /**
     * Removes resources from offer to MesosOffer.
     *
     * @param offer Offer to remove resources from.
     */
    public void removeResourcesFrom(MesosOffer offer) {
        this.cpus = Math.max(this.cpus - offer.getCpus(), 0.0);
        this.mem = Math.max(this.mem - offer.getMem(), 0.0);
        this.disk = Math.max(this.disk - offer.getDisk(), 0.0);
    }


    private void countResources(List<Resource> resources) {
        for (Resource resource : resources) {
            switch (resource.getName()) {
                case CPUS_RESOURCE:
                    cpus += resource.getScalar().getValue();
                    break;
                case MEM_RESOURCE:
                    mem += resource.getScalar().getValue();
                    break;
                case DISK_RESOURCE:
                    disk += resource.getScalar().getValue();
                    break;
                case PORTS_RESOURCE:
                    // This does not check if ranges are in conflict (ej: 2-5 and 3-7). Assuming correctness
                    ports.addAll(resource.getRanges().getRangeList());
                    break;
                default:
                    // Nothing
            }
        }
    }

    private Range buildRange(long begin, long end) {
        return Range.newBuilder().setBegin(begin).setEnd(end).build();
    }
}
