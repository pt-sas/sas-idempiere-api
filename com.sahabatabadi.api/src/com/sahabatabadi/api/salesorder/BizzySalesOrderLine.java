package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * POJO class to represent Sales Order Line data from Bizzy. Used in
 * {@link BizzySalesOrder}.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class BizzySalesOrderLine implements Serializable {
    private static final long serialVersionUID = 20200915L;

    /**
     * Product code of the product being ordered. Has to match the entries in
     * iDempiere's {@code M_Product} table. Example: {@code "AB0301485"}.
     */
    public String productCode;

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
        this.productCode = bizzySoLine.productCode;
        this.quantity = bizzySoLine.quantity;
        this.principalId = bizzySoLine.principalId;
        this.discount = bizzySoLine.discount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field soField : this.getClass().getDeclaredFields()) {
            if (!Modifier.isPublic(soField.getModifiers())) {
                continue; // non-public fields
            }

            Object value = null;
            try {
                value = soField.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return this.toStringNoReflection();
            }

            if (value == null) {
                continue;
            }

            sb.append(soField.getName() + ": " + value + "\n\n");
        }

        return sb.toString();
    }

    /**
     * Helper method to compute toString without using Reflection as a failsafe.
     * 
     * @return contents of the SO line
     */
    private String toStringNoReflection() {
        StringBuilder sb = new StringBuilder();
        sb.append("productCode: " + this.productCode + "\n\n");
        sb.append("quantity: " + this.quantity + "\n\n");
        return sb.toString();
    }
}
