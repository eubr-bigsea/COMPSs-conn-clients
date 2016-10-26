package es.bsc.conn.clients.mesos.framework;

import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskState;

import es.bsc.conn.clients.mesos.framework.log.Loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;


public class MesosTask {

    private List<Resource> requirements;
    private TaskState state;
    private Map<TaskState, Semaphore> waitSems;
    private String ip;
    private String id;

    // Times tried to launch task and failed
    private int retries;

    private static final Logger LOGGER = LogManager.getLogger(Loggers.MESOS_TASK);


    public MesosTask(String id, TaskState state, List<Resource> requirements) {
        this.id = id;
        this.state = state;
        this.requirements = requirements;
        this.waitSems = new EnumMap<TaskState, Semaphore>(TaskState.class);
        this.retries = 0;
    }

    public String getId() {
        return id;
    }

    public List<Resource> getRequirements() {
        return requirements;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
        releaseSem(state);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getRetries() {
        return retries;
    }

    public synchronized void incrementRetries() {
        retries += 1;
    }

    public void addWait(TaskState state, Semaphore sem) {
        waitSems.put(state, sem);
    }

    private void releaseSem(TaskState state) {
        if (waitSems.containsKey(state)) {
            LOGGER.debug("Release semaphore " + waitSems.get(state).toString() + " for state " + state.toString() + ", task " + id);
            waitSems.get(state).release();
            waitSems.remove(state);
        }
    }

    @Override
    public String toString() {
        return String.format("[Task %s] state: %s", id, state.toString());
    }
}
