package es.bsc.conn.clients.exceptions;

/**
 * Generic ConnClient exception
 *
 */
public class ConnClientException extends Exception {

    /**
     * Exception Version UID are 2L in all Runtime
     */
    private static final long serialVersionUID = 2L;


    /**
     * Instantiate a new ConnClientException from a given message
     * 
     * @param message
     */
    public ConnClientException(String message) {
        super(message);
    }

    /**
     * Instantiate a new ConnClientException from a nested exception
     * 
     * @param e
     */
    public ConnClientException(Exception e) {
        super(e);
    }

    /**
     * Instantiate a new ConnClientException from a nested exception and with a given message
     * 
     * @param msg
     * @param e
     */
    public ConnClientException(String msg, Exception e) {
        super(msg, e);
    }

}
