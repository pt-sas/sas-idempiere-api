package com.sahabatabadi.api;

/**
 * Custom Exception class to represent Exceptions being thrown when processing a
 * {@link ApiInjectable} object.
 */
public class SASApiException extends Exception {
    /**
     * Auto-genereated serial version UID by Eclipse
     */
    private static final long serialVersionUID = 7446213057086374281L;

    /**
     * Document being processed when exception is thrown
     */
    private ApiInjectable doc;

    /**
     * Window ID of the document being processed
     */
    private int windowId;

    /**
     * Tab ID of the document being processed
     */
    private int tabId;

    /**
     * Constructs a new exception with null as its detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to initCause.
     * 
     * @param currentDoc document being processed when exception is thrown
     * @param windowId   window ID of the document being processed
     * @param tabId      tab ID of the document being processed
     */
    public SASApiException(ApiInjectable currentDoc, int windowId, int tabId) {
        super();
        this.doc = currentDoc;
        this.windowId = windowId;
        this.tabId = tabId;
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is
     * not initialized, and may subsequently be initialized by a call to initCause.
     * 
     * @param currentDoc document being processed when exception is thrown
     * @param windowId   window ID of the document being processed
     * @param tabId      tab ID of the document being processed
     * @param message    the detail message. The detail message is saved for later
     *                   retrieval by the getMessage() method.
     */
    public SASApiException(ApiInjectable currentDoc, int windowId, int tabId, String message) {
        super(message);
        this.doc = currentDoc;
        this.windowId = windowId;
        this.tabId = tabId;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param currentDoc document being processed when exception is thrown
     * @param windowId   window ID of the document being processed
     * @param tabId      tab ID of the document being processed
     * @param message    the detail message (which is saved for later retrieval by
     *                   the getMessage() method).
     * @param cause      the cause (which is saved for later retrieval by the
     *                   getCause() method). (A null value is permitted, and
     *                   indicates that the cause is nonexistent or unknown.)
     */
    public SASApiException(ApiInjectable currentDoc, int windowId, int tabId, String message, Throwable cause) {
        super(message, cause);
        this.doc = currentDoc;
        this.windowId = windowId;
        this.tabId = tabId;
    }

    /**
     * Gets the document being processed when exception is thrown
     * 
     * @return document being processed on exception
     */
    public ApiInjectable getBadDocument() {
        return this.doc;
    }

    /**
     * Gets the ID of the iDempiere Window related to the document being processed
     * 
     * @return window ID of the document being processed
     */
    public int getWindowId() {
        return this.windowId;
    }

    /**
     * Gets the ID of the iDempiere Tab related to the document being processed
     * 
     * @return tab ID of the document being processed
     */
    public int getTabId() {
        return this.tabId;
    }
}
