package com.sahabatabadi.api.salesorder;

import java.io.Serializable;

/**
 * POJO class to represent Sales Order Line data from Bizzy. Used in
 * {@link BizzySalesOrder}.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class BizzySalesOrderLine implements Serializable {
    private static final long serialVersionUID = 20200730L;

    /**
     * Product ID of the product being ordered. Has to match the entries in
     * iDempiere's {@code M_Product} table. Example: {@code "AB0301485"}.
     */
    public String productId;

    /**
     * Quantity of the product being ordered.
     */
    public int quantity;

    /**
     * Principal or brand of the product being ordered. Can be left empty. If
     * filled, has to match the entries in iDempiere's {@code M_Product} table.
     * Example: {@code "Philips"}.
     */
    public String principalId;

    /**
     * Discount amount in percent of the product being ordered. Can be left empty.
     * Example: {@code 7.51} (%).
     */
    public double discount;

    /**
     * Empty constructor
     */
    public BizzySalesOrderLine() {
        this.quantity = -1;
        this.discount = -1;
    }

    /**
     * Copy constructor
     * 
     * @param bizzySoLine Bizzy SO Line object to copy from
     */
    public BizzySalesOrderLine(BizzySalesOrderLine bizzySoLine) {
        this.productId = bizzySoLine.productId;
        this.quantity = bizzySoLine.quantity;
        this.principalId = bizzySoLine.principalId;
        this.discount = bizzySoLine.discount;
    }
}
