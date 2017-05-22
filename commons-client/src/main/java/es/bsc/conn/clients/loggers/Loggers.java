package es.bsc.conn.clients.loggers;

import es.bsc.conn.clients.exceptions.NonInstantiableException;

/**
 * Loggers' names for Connector Clients
 *
 */
public class Loggers {

    // Integrated Toolkit
    public static final String IT = "integratedtoolkit";

    // Root connectors logger name
    public static final String CONNECTORS = IT + ".Connectors";
    public static final String CONN = CONNECTORS + ".Conn";
    public static final String CONN_CLIENTS = CONN + ".Clients";

    // Specific connector client loggers for each implementation
    public static final String ROCCI = CONN_CLIENTS + ".Rocci";
    public static final String JCLOUDS = CONN_CLIENTS + ".JClouds";
    public static final String DOCKER = CONN_CLIENTS + ".Docker";
    public static final String MESOS = CONN_CLIENTS + ".Mesos";
    public static final String VMM = CONN_CLIENTS + ".VMM";
    public static final String SLURM = CONN_CLIENTS + ".SLURM";

    private Loggers() {
        throw new NonInstantiableException("Loggers should not be instantiated");
    }

}
