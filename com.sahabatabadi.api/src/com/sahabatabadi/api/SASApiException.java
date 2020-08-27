package com.sahabatabadi.api;

/**
 * Custom Exception class to represent Exceptions being thrown when processing
 * a {@link Document} object.
 */
public class SASApiException extends Exception {
    /**
     * Auto-genereated serial version UID by Eclipse
     */
    private static final long serialVersionUID = 7446213057086374281L;

    /**
     * Document being processed when exception is thrown
     */
    private Document doc;

    /**
     * Constructs a new exception with null as its detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to initCause.
     * 
     * @param currentDoc document being processed when exception is thrown
     */
    public SASApiException(Document currentDoc) {
        super();
        this.doc = currentDoc;
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is
     * not initialized, and may subsequently be initialized by a call to initCause.
     * 
     * @param currentDoc document being processed when exception is thrown
     * @param message    the detail message. The detail message is saved for later
     *                   retrieval by the getMessage() method.
     */
    public SASApiException(Document currentDoc, String message) {
        super(message);
        this.doc = currentDoc;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param currentDoc document being processed when exception is thrown
     * @param message    the detail message (which is saved for later retrieval by
     *                   the getMessage() method).
     * @param cause      the cause (which is saved for later retrieval by the
     *                   getCause() method). (A null value is permitted, and
     *                   indicates that the cause is nonexistent or unknown.)
     */
    public SASApiException(Document currentDoc, String message, Throwable cause) {
        super(message, cause);
        this.doc = currentDoc;
    }

    /**
     * Gets the document being processed when exception is thrown
     * 
     * @return document being processed on exception
     */
    public Document getBadDocument() {
        return this.doc;
    }
}
