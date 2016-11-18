package es.bsc.conn.clients.exceptions;

/**
 * Generic exception for Non Instantiable classes
 *
 */
public class NonInstantiableException extends RuntimeException {

    /**
     * Exceptions Version UID are 2L in all Runtime
     */
    private static final long serialVersionUID = 2L;


    /**
     * Creates a new exception for non instantiable class with the given classname
     * 
     * @param className
     */
    public NonInstantiableException(String className) {
        super("Class " + className + " can not be instantiated.");
    }

}
