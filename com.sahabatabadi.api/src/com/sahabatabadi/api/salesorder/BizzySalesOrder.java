package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.util.Date;

public class BizzySalesOrder implements Serializable {
	private static final long serialVersionUID = 20200728L;

    public char soff_code;
    public String description;
    public Date dateOrdered;
    public int bpHoldingNo;
    public String bpLocationName;

    public char orderSource;

    public BizzySalesOrderLine[] orderLines;

    /**
     * Empty constructor
     */
    public BizzySalesOrder() {
        bpHoldingNo = -1;
    }

    /**
     * Copy constructor
     */
    public BizzySalesOrder(BizzySalesOrder oldBizzySo) {
        this.soff_code      = oldBizzySo.soff_code;
        this.description    = oldBizzySo.description;
        this.dateOrdered    = oldBizzySo.dateOrdered;
        this.bpHoldingNo    = oldBizzySo.bpHoldingNo;
        this.bpLocationName = oldBizzySo.bpLocationName;
        this.orderSource    = oldBizzySo.orderSource;
        this.orderLines     = oldBizzySo.orderLines;
    }
}
