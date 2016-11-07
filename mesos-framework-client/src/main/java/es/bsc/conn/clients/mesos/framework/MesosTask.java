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

    private static final Logger LOGGER = LogManager.getLogger(Loggers.MESOS_TASK);
    
    // Properties
    private List<Resource> requirements;
    private TaskState state;
    private Map<TaskState, Semaphore> waitSems;
    private String ip;
    private String id;
    private String imageName;

    // Times tried to launch task and failed
    private int retries;


    /**
     * Represents a Task to execute in Mesos.
     *
     * @param id           Identifier.
     * @param imageName    Docker image.
     * @param state        State of the task, TASK_STAGING by default.
     * @param requirements List of resources required for the task.
     */
    public MesosTask(String id, String imageName, TaskState state, List<Resource> requirements) {
        this.id = id;
        this.imageName = imageName;
        this.state = state;
        this.requirements = requirements;
        this.waitSems = new EnumMap<>(TaskState.class);
        this.retries = 0;
    }

    /**
     * @return MesosTask identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Docker image name.
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * @return List of resources required.
     */
    public List<Resource> getRequirements() {
        return requirements;
    }

    /**
     * @return Mesos State for the task.
     */
    public TaskState getState() {
        return state;
    }

    /**
     * Sets the state of the Mesos task. If exists a semaphore waiting, it is released.
     *
     * @param state New state to set.
     */
    public void setState(TaskState state) {
        this.state = state;
        releaseSem(state);
    }

    /**
     * @return Docker mesos container IP.
     */
    public String getIp() {
        return ip;
    }

    /**
     * @param ip New IP to assign.
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * @return Number of failed schedulings to mesos.
     */
    public int getRetries() {
        return retries;
    }

    /**
     * Increment the number of failed schedulings.
     */
    public synchronized void incrementRetries() {
        retries += 1;
    }

    /**
     * Adds a semaphore to wait for Task to be in state specified.
     *
     * @param state State to wait for.
     * @param sem   Semaphore to use.
     */
    public void addWait(TaskState state, Semaphore sem) {
        waitSems.put(state, sem);
    }

    /**
     * @return MesosTask string.
     */
    @Override
    public String toString() {
        return String.format("[Task %s] state: %s", id, state.toString());
    }

    private void releaseSem(TaskState state) {
        if (waitSems.containsKey(state)) {
            LOGGER.debug("Release semaphore " + waitSems.get(state).toString() + " for state " + state.toString() + ", task " + id);
            waitSems.get(state).release();
            waitSems.remove(state);
        }
    }

}
