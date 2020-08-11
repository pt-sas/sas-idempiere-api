package com.sahabatabadi.api.salesorder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.adempiere.base.IModelFactory;
import org.adempiere.base.Service;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * Class to represent required information to inject a sales order header into
 * SAS iDempiere. Has methods to convert external SO classes to SAS SO. Avoid
 * editing fields manually; rather, use the constructors to convert external SO
 * into SAS SO.
 */
public class SASSalesOrder {
    /**
     * Length of BP codes.
     */
    public static final int BP_ID_LENGTH = 5;

    /**
     * Increment between line numbers in an unprocessed SO. 
     */
    public static final int LINE_NUMBER_INCREMENT = 10;

    /**
     * Org's full name. Has to match entries in the {@code name} field in
     * iDempiere's {@code AD_Org} table, where where the {@code isorgtrxdim} field
     * is {@code 'N'}.
     */
    public String org;

    /**
     * Document number of the SO. Has to exactly match predefined format in
     * iDempiere. 
     * 
     * @see org.compiere.util.DB#getDocumentNo
     */
    public String documentNo;

    /**
     * Raw description of the SO header. Can be left empty.
     */
    public String description;

    /**
     * Exact name of this SO's target document type. Has to match entries in the
     * {@code name} field in iDempiere's {@code C_DocType} table.
     */
    public String docType;

    /**
     * Date this SO is created. Has to be in the format {@code "yyyy-MM-dd"}.
     */
    public String dateOrdered;

    /**
     * Has to exactly match {@link #dateOrdered}.
     */
    public String datePromised;

    /**
     * Five-digit BP code. Has to match entries in the {@code value} field in
     * iDempiere's {@code C_BPartner} table.
     */
    public String bpCode;

    /**
     * Five-digit BP code to be invoiced. Typically has the same value as
     * {@link #bpCode}. Has to match entries in the {@code value} field in
     * iDempiere's {@code C_BPartner} table.
     */
    public String invoiceBpCode;

    /**
     * Full name of the BP location. Has to match entries in the {@code name} field
     * in iDempiere's {@code C_BPartner_Location} table.
     */
    public String bpLocation;

    /**
     * Full name of the BP location to be invoiced. Typically has the same value as
     * {@link #bpLocation}. Has to match entries in the {@code name} field in
     * iDempiere's {@code C_BPartner_Location} table.
     */
    public String invoiceBpLocation;

    /**
     * Name of the default warehouse of the associated {@link #org}. Has to match
     * entries in the {@code name} field in iDempiere's {@code M_Warehouse} table.
     */
    public String warehouse;

    /**
     * Org Trx's full name. Has to match entries in the {@code name} field in
     * iDempiere's {@code AD_Org} table, where the {@code isorgtrxdim} field is
     * {@code 'Y'}.
     */
    public String orgTrx;

    /**
     * Array of {@link SASSalesOrderLine} object associated with this header object.
     */
    public SASSalesOrderLine[] orderLines;

    protected CLogger log = CLogger.getCLogger(getClass());

    /**
     * Represents the date format in SAS iDempiere SO template.
     */
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Last / largest line number associated with this header.
     */
    private int latestLineNumber = 0;

    /**
     * Default constructor.
     * 
     * This class requires the specified bizzySo to have at least one SO line, and
     * also requires all SO lines to have identical product principal and product
     * discount. In other words, the Bizzy SO headers have to be grouped by
     * principal and discount prior to being passed in to this constructor.
     * 
     * @param bizzySo Bizzy SO object to convert to SAS SO object.
     * 
     * @see org.compiere.model.PO#saveNew()
     */
    public SASSalesOrder(BizzySalesOrder bizzySo) {
        this.org = SOUtils.orgMap.get(bizzySo.soff_code);
        this.description = bizzySo.description;
        this.dateOrdered = formatter.format(bizzySo.dateOrdered);
        this.datePromised = this.dateOrdered;
        this.bpCode = SOUtils.prependZeros(bizzySo.bpHoldingNo, BP_ID_LENGTH);
        this.invoiceBpCode = this.bpCode;
        this.bpLocation = bizzySo.bpLocationName;
        this.invoiceBpLocation = this.bpLocation;
        this.warehouse = SOUtils.warehouseMap.get(bizzySo.soff_code);

        StringBuilder sb = new StringBuilder("O");
        sb.append(bizzySo.orderSource);
        sb.append(SOUtils.getBPLocationIsTax(bizzySo.bpLocationName) ? "T" : "N");
        this.docType = SOUtils.docTypeMap.get(sb.toString());

        String principal = bizzySo.orderLines[0].principalId;
        this.orgTrx = SOUtils.getOrgTrx(this.bpCode, principal);

        // org.compiere.model.PO#saveNew()
        PO po = SOUtils.getMOrderPO(SOUtils.orgIdMap.get(this.org), SOUtils.orgTrxIdMap.get(this.orgTrx), bizzySo.dateOrdered);
        this.documentNo = DB.getDocumentNo(SOUtils.docTypeIdMap.get(this.docType), null, false, po);

        this.orderLines = new SASSalesOrderLine[bizzySo.orderLines.length];
        for (int i = 0; i < orderLines.length; i++) {
            this.orderLines[i] = new SASSalesOrderLine(bizzySo.orderLines[i], this);
        }
    }

    /**
     * Calculates the line number for the next SO line to be associated with this
     * header. The line numbers are incremented by {@value #LINE_NUMBER_INCREMENT}.
     * 
     * @return the line number of the next SAS SO line.
     */
    protected int getNextLineNumber() {
        this.latestLineNumber += LINE_NUMBER_INCREMENT;
        return this.latestLineNumber;
    }
}
