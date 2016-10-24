package es.bsc.mesos.framework.exceptions;


public class FrameworkException extends Exception {
    /**
     * Exception Version UID are 2L in all Runtime
     */
    private static final long serialVersionUID = 2L;

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(Exception e) {
        super(e);
    }

    public FrameworkException(String msg, Exception e) {
        super(msg, e);
    }

}
