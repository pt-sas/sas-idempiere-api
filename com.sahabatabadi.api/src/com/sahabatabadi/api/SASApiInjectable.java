package com.sahabatabadi.api;

/**
 * Interface to denote objects able to be injected via SAS iDempiere API.
 */
public interface SASApiInjectable {
    /**
     * Gets iDempiere column name given the field / instance variable name. The
     * iDempiere column name follows iDempiere Template format.
     * 
     * @param fieldName name of the field / instance variable
     * @return name of the iDempiere column according to iDempiere Template format
     *         specifications
     */
    public String getColumnName(String fieldName);
}
