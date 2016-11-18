package es.bsc.conn.clients.mesos.framework.exceptions;

/**
 * Generic Mesos Framework exception
 *
 */
public class FrameworkException extends Exception {

    /**
     * Exception Version UID are 2L in all Runtime
     */
    private static final long serialVersionUID = 2L;


    /**
     * Instantiate a new FrameworkException from a given message
     * 
     * @param message
     */
    public FrameworkException(String message) {
        super(message);
    }

    /**
     * Instantiate a new FrameworkException from a nested exception
     * 
     * @param e
     */
    public FrameworkException(Exception e) {
        super(e);
    }

    /**
     * Instantiate a new FrameworkException from a nested exception and with a given message
     * 
     * @param msg
     * @param e
     */
    public FrameworkException(String msg, Exception e) {
        super(msg, e);
    }

}
