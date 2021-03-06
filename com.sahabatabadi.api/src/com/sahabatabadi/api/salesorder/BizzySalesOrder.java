package com.sahabatabadi.api.salesorder;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO class to represent Sales Order Header data from Bizzy.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class BizzySalesOrder implements Serializable {
    private static final long serialVersionUID = 20200728L;

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
    public int bpHoldingNo;

    /**
     * Exact name of the business partner's (BP) or customer's location. Value has
     * to exactly match the name in iDempiere's {@code C_BPartner_Location} table.
     * Example: {@code "PIONIR ELEKTRIK INDONESIA [Kenari Mas Jl. Kramat Raya Lt.
     * Dasar Blok C No. 3-5]"}
     */
    public String bpLocationName;

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
    public BizzySalesOrder() {
        bpHoldingNo = -1;
    }

    /**
     * Copy constructor
     * 
     * @param bizzySo Bizzy SO object to copy from
     */
    public BizzySalesOrder(BizzySalesOrder bizzySo) {
        this.soff_code = bizzySo.soff_code;
        this.description = bizzySo.description;
        this.dateOrdered = bizzySo.dateOrdered;
        this.bpHoldingNo = bizzySo.bpHoldingNo;
        this.bpLocationName = bizzySo.bpLocationName;
        this.orderSource = bizzySo.orderSource;
        this.orderLines = bizzySo.orderLines;
    }
}
