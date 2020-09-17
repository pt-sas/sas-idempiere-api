package com.sahabatabadi.api.rmi;

/**
 * Wrapper exception for Exception and any of its inherited classes to make it
 * a checked exception.
 */
public class MasterDataNotFoundException extends Exception {
    /**
     * Auto-genereated serial version UID by Eclipse
     */
    private static final long serialVersionUID = 461853077216045252L;

    /**
     * Original Exception that is wrapped by this exception class
     */
    private Throwable cause;

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param cause      the cause, which is saved for later retrieval by the
     *                   {@link #getCause()} method.
     */
    public MasterDataNotFoundException(Throwable cause) {
        this.cause = cause;
    }

    /**
     * Returns the cause of this throwable.
     */
    public Throwable getCause() {
        return cause;
    }
}
