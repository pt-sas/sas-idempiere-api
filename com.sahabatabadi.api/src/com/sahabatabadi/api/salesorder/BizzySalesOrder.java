package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.util.Date;

public class BizzySalesOrder implements Serializable {
	private static final long serialVersionUID = 20200709L;

    public char soff_code;
    public String description;
    public Date dateOrdered;
    public int bpHoldingNo;
    public String bpLocationName;

    public char orderSource;

    public BizzySalesOrderLine[] orderLines;
}
