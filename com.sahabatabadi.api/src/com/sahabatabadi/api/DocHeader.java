package com.sahabatabadi.api;

/**
 * Interface to denote object is a header and is able to be injected via SAS
 * iDempiere API.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public interface DocHeader extends Document {
    /**
     * Gets the lines associated with this header object.
     * 
     * @return Line objects as an array
     */
    public DocLine[] getLines();
}
