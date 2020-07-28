package com.sahabatabadi.api.salesorder;

import java.io.Serializable;

public class BizzySalesOrderLine implements Serializable {
	private static final long serialVersionUID = 20200728L;

    public String productId;
    public int quantity;

    public char principalId;
    public int discount;
}
