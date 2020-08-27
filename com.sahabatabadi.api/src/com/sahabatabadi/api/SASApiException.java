package com.sahabatabadi.api;

public class SASApiException extends Exception {
    /**
     * Auto-genereated serial version UID by Eclipse
     */
    private static final long serialVersionUID = 7446213057086374281L;

    private Document doc;

    public SASApiException(Document currentDoc) {
        super();
        this.doc = currentDoc;
    }

    public SASApiException(Document currentDoc, String message) {
        super(message);
        this.doc = currentDoc;
    }

    public SASApiException(Document currentDoc, String message, Throwable cause) {
        super(message, cause);
        this.doc = currentDoc;
    }

    public Document getBadDocument() {
        return this.doc;
    }
}
