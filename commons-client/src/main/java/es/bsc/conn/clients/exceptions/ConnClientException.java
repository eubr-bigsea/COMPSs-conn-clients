package es.bsc.conn.clients.exceptions;

public class ConnClientException extends Exception {

    /**
     * Exception Version UID are 2L in all Runtime
     */
    private static final long serialVersionUID = 2L;


    public ConnClientException(String message) {
        super(message);
    }

    public ConnClientException(Exception e) {
        super(e);
    }

    public ConnClientException(String msg, Exception e) {
        super(msg, e);
    }

}
