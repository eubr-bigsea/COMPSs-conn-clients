package es.bsc.conn.clients.mesos.framework.log;

import es.bsc.conn.clients.exceptions.NonInstantiableException;


public final class Loggers {

    // Mesos Framework
    public static final String MF = es.bsc.conn.clients.loggers.Loggers.MESOS;

    // Mesos Framework Scheduler
    public static final String MF_SCHEDULER = MF + ".Scheduler";

    // Mesos Offer
    public static final String MESOS_OFFER = MF + ".Offer";

    // Mesos Task
    public static final String MESOS_TASK = MF + ".Task";


    private Loggers() {
        throw new NonInstantiableException("Loggers should not be instantiated");
    }

}
