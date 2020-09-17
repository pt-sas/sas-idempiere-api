package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

/**
 * POJO class to represent Sales Order Header data from Bizzy.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class BizzySalesOrder implements Serializable {
    private static final long serialVersionUID = 20200915L;

    /**
     * Represents the branch associated with the sales order. Value has to be either
     * {@code 'A'}, {@code 'B'}, {@code 'C'}, {@code 'D'}, or {@code 'M'}.
     */
    public char soff_code;

    /**
     * Raw description of the sales order. Can be left empty.
     */
    public String description;

    /**
     * Date the sales order was created.
     */
    public Date dateOrdered;

    /**
     * Five-digit business partner (BP) or customer number, as an int. For example,
     * for BP "PIONEER ELEKTRIC" with BP number "03806", the value is {@code 3806}.
     */
    public String bpHoldingCode;

    /**
     * ID of the business partner's (BP) or customer's location from iDempiere's
     * {@code C_BPartner_Location} table.
     */
    public String bpLocationCode;

    /**
     * Bizzy source of this order. Value has to be either {@code 'B'} (representing
     * BFF), or {@code 'S'} (representing TokoSmart).
     */
    public char orderSource;

    /**
     * Array of {@link BizzySalesOrderLine} object associated with this header.
     */
    public BizzySalesOrderLine[] orderLines;

    /**
     * Empty constructor
     */
    public BizzySalesOrder() {}

    /**
     * Copy constructor
     * 
     * @param bizzySo Bizzy SO object to copy from
     */
    public BizzySalesOrder(BizzySalesOrder bizzySo) {
        this.soff_code = bizzySo.soff_code;
        this.description = bizzySo.description;
        this.dateOrdered = bizzySo.dateOrdered;
        this.bpHoldingCode = bizzySo.bpHoldingCode;
        this.bpLocationCode = bizzySo.bpLocationCode;
        this.orderSource = bizzySo.orderSource;
        this.orderLines = bizzySo.orderLines;
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
        sb.append("soff_code\t: " + this.soff_code + "\n");
        sb.append("description\t: " + this.description + "\n");
        sb.append("dateOrdered\t: " + this.dateOrdered + "\n");
        sb.append("bpHoldingCode\t: " + this.bpHoldingCode + "\n");
        sb.append("bpLocationCode\t: " + this.bpLocationCode + "\n");
        sb.append("orderSource\t: " + this.orderSource + "\n");

        sb.append("orderLine: [");
        for (BizzySalesOrderLine line : this.orderLines) {
            sb.append(line.toString());
        }
        sb.append("]");

        return sb.toString();
    }
}
