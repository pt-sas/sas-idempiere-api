package com.sahabatabadi.api.rmi;

/**
 * Wrapper exception for Exception and any of its inherited classes to make it a
 * checked exception.
 */
public class MasterDataNotFoundException extends Exception {
    /**
     * Auto-genereated serial version UID by Eclipse
     */
    private static final long serialVersionUID = 461853077216045252L;

    /**
     * Constructs a wrapper exception with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     */
    public MasterDataNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a wrapper exception with the specified detail message and cause.
     * 
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).
     */
    public MasterDataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
