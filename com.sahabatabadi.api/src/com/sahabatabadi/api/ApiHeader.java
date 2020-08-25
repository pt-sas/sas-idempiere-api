package com.sahabatabadi.api;

/**
 * Interface to denote object is a header and is able to be injected via 
 * SAS iDempiere API.
 */
public interface ApiHeader extends ApiInjectable {
	/**
	 * Gets the lines associated with this header object.
	 * 
	 * @return Line objects as an array 
	 */
	public ApiInjectable[] getLines();
}
