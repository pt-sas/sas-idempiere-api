package com.sahabatabadi.api.salesorder;

import com.sahabatabadi.api.SASApiInjectable;

/**
 * Class to represent required information to inject a sales order line into SAS
 * iDempiere. Has methods to convert external SO line classes to SAS SO line.
 * Avoid editing fields manually; rather, use the constructors to convert
 * external SO line into SAS SO line.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASSalesOrderLine implements SASApiInjectable {
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
    public int quantity;

    /**
     * Has to exactly match {@link SASSalesOrder#documentNo} in the header.
     */
    public String documentNo;

    /**
     * Has to exactly match {@link SASSalesOrder#datePromised} in the header.
     */
    public String datePromised;

    /**
     * SO header associated with this SO line.
     */
    private SASSalesOrder header;

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
        this.quantity = orderLine.quantity;

        /* calculating values */
        this.lineNo = header.getNextLineNumber();
        this.documentNo = this.header.documentNo;
        this.datePromised = this.header.datePromised;
    }
}
