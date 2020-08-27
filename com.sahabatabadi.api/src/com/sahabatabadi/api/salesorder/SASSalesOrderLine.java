package com.sahabatabadi.api.salesorder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sahabatabadi.api.Document;

/**
 * Class to represent required information to inject a sales order line into SAS
 * iDempiere. Has methods to convert external SO line classes to SAS SO line.
 * Avoid editing fields manually; rather, use the constructors to convert
 * external SO line into SAS SO line.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASSalesOrderLine implements Document {
    // TODO add the rest of the fields, can be null. Convert primitives to Object.
    /**
     * Table name in iDempiere associated with this line object.
     */
    public static final String TABLE_NAME = "C_OrderLine";

    /**
     * Line number of the SO. Typically incremented by
     * {@link SASSalesOrder#LINE_NUMBER_INCREMENT} between
     * different SO lines.
     */
    public int lineNo;
    
    /**
     * Product ID of the product being ordered. Has to match the entries in the
     * {@code value} field in iDempiere's {@code M_Product} table.
     */
    public String productId;

    /**
     * Quantity of the product being ordered.
     */
    public BigDecimal quantity;

    /**
     * Has to exactly match {@link SASSalesOrder#documentNo} in the header.
     */
    public String documentNo;

    /**
     * Has to exactly match {@link SASSalesOrder#datePromised} in the header.
     */
    public Date datePromised;

    /**
     * SO header associated with this SO line.
     */
    private SASSalesOrder header;

    /**
     * Mapping between this class's field / instance variable names and iDempiere
     * column names. The column names follow iDempiere Template format
     * specifications.
     */
    public static Map<String, String> fieldColumnMap;

    static {
        HashMap<String, String> tempFieldColumnMap = new HashMap<>();
        tempFieldColumnMap.put("lineNo", "C_OrderLine>Line");
        tempFieldColumnMap.put("productId", "C_OrderLine>M_Product_ID[Value]");
        tempFieldColumnMap.put("quantity", "C_OrderLine>QtyEntered");
        tempFieldColumnMap.put("documentNo", "C_OrderLine>C_Order_ID[DocumentNo]/K");
        tempFieldColumnMap.put("datePromised", "C_OrderLine>DatePromised");
        fieldColumnMap = Collections.unmodifiableMap(tempFieldColumnMap);
    }

    /**
     * Default constructor.
     * 
     * @param orderLine Bizzy SO line object to convert to SAS SO line object.
     * @param header    SAS SO header object associated with this SO line.
     */
    public SASSalesOrderLine(BizzySalesOrderLine orderLine, SASSalesOrder header) {
        /* parsing values from Bizzy SO Line */
        this.header = header;
        this.productId = orderLine.productId;
        this.quantity = new BigDecimal(orderLine.quantity);

        /* calculating values */
        this.lineNo = header.getNextLineNumber();
        this.documentNo = this.header.documentNo;
        this.datePromised = this.header.datePromised;
    }

    @Override
    public String getColumnName(String fieldName) {
        return fieldColumnMap.get(fieldName);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getDocumentNo() {
        return this.documentNo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field soField : this.getClass().getDeclaredFields()) {
            if (!Modifier.isPublic(soField.getModifiers())) {
                continue; // non-public fields
            }

            String columnName = this.getColumnName(soField.getName());
            if (columnName == null) {
                continue; // non-SO fields, e.g. constants, logger, etc.
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

            sb.append(columnName + ": " + value + "\n");
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
        sb.append("C_OrderLine>Line" + this.lineNo);
        sb.append("C_OrderLine>M_Product_ID[Value]" + this.productId);
        sb.append("C_OrderLine>QtyEntered" + this.quantity);
        sb.append("C_OrderLine>C_Order_ID[DocumentNo]/K" + this.documentNo);
        sb.append("C_OrderLine>DatePromised" + this.datePromised);
        return sb.toString();
    }
}
