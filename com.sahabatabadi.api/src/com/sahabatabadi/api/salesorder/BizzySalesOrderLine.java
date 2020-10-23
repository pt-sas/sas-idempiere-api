package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.lang.reflect.Array;
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
     * Product code of the product being ordered. Has to match the entries in the
     * {@code value} field in iDempiere's {@code M_Product} table.
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
    public String principal;

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
        this.principal = bizzySoLine.principal;
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

            if (soField.getType().isArray()) {
                sb.append(soField.getName() + "\t: [");
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    Object arrayElement = Array.get(value, i);
                    sb.append("{\n").append(arrayElement.toString()).append("},\n");
                }
                sb.append("]");
            } else {
                sb.append(soField.getName() + "\t: " + value + "\n");
            }
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
        sb.append("productCode\t: " + this.productCode + "\n");
        sb.append("quantity\t: " + this.quantity + "\n");
        return sb.toString();
    }
}
