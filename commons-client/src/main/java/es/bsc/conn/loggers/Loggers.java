package es.bsc.conn.loggers;

import es.bsc.conn.exceptions.NonInstantiableException;


public class Loggers {

    // Integrated Toolkit
    public static final String IT = "integratedtoolkit";

    // Root connectors logger name
    public static final String CONNECTORS = IT + ".Connectors";
    public static final String CONNECTORS_IMPL = IT + ".ConnectorsImpl";

    // Specific connector client loggers for each implementation
    public static final String ROCCI = CONNECTORS + ".Rocci";
    public static final String JCLOUDS = CONNECTORS + ".JClouds";
    public static final String DOCKER = CONNECTORS + ".Docker";
    public static final String MESOS = CONNECTORS + ".Mesos";


    private Loggers() {
        throw new NonInstantiableException("Loggers should not be instantiated");
    }

}
