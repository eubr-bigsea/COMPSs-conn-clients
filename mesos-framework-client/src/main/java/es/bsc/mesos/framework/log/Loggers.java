package es.bsc.mesos.framework.log;


public final class Loggers {

    // Integrated Toolkit
    public static final String IT = "integratedtoolkit";

    // Conn (to differentiate from IT Connectors)
    public static final String CONN = IT + ".Conn";

    // Connectors
    public static final String CONN_CONNECTORS = CONN + ".Connectors";

    // Mesos Framework
    public static final String MF = CONN_CONNECTORS + ".MesosFramework";

    // Mesos Framework Scheduler
    public static final String MF_SCHEDULER = MF + ".Scheduler";

    //
    public static final String MESOS_OFFER = MF + ".Offer";

    //
    public static final String MESOS_TASK = MF + ".Task";


    private Loggers() {
        throw new AssertionError("Loggers should not be instantiated");
    }

}
