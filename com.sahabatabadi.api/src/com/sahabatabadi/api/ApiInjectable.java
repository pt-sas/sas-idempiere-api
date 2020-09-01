package com.sahabatabadi.api;

/**
 * Interface to denote objects able to be injected via SAS iDempiere API.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public interface ApiInjectable {
    /**
     * Gets iDempiere column name given the field / instance variable name. The
     * iDempiere column name follows iDempiere Template format.
     * 
     * @param fieldName name of the field / instance variable
     * @return name of the iDempiere column according to iDempiere Template format
     *         specifications
     */
    public String getColumnName(String fieldName);

    /**
     * Gets the table name in iDempiere associated with the object.
     * 
     * @return Table name in iDempiere
     */
    public String getTableName();

    /**
     * Gets the document number (primary key) of the object.
     * 
     * @return Document number of the object
     */
    public String getDocumentNo();
}
