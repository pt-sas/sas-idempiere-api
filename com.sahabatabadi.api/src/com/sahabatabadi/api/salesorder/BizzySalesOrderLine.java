package com.sahabatabadi.api.salesorder;

import java.io.Serializable;

public class BizzySalesOrderLine implements Serializable {
	private static final long serialVersionUID = 20200730L;

    public String productId;
    public int quantity;

    public String principalId;
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
     */
    public BizzySalesOrderLine(BizzySalesOrderLine bizzySoLine) {
        this.productId = bizzySoLine.productId;
        this.quantity = bizzySoLine.quantity;
        this.principalId = bizzySoLine.principalId;
        this.discount = bizzySoLine.discount;
    }
}
