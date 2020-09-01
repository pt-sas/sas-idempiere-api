package com.sahabatabadi.api;

/**
 * Interface to denote object is a line/detail and is able to be injected via SAS
 * iDempiere API.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public interface DocLine extends ApiInjectable {
	/**
	 * Gets the header associated with this line object
	 * 
	 * @return header object of this line
	 */
	public DocHeader getHeader();
}
