package com.sahabatabadi.api.salesorder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.sahabatabadi.api.DocHeader;
import com.sahabatabadi.api.Document;

/**
 * Class to represent required information to inject a sales order header into
 * SAS iDempiere. Has methods to convert external SO classes to SAS SO. Avoid
 * editing fields manually; rather, use the constructors to convert external SO
 * into SAS SO.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASSalesOrder implements Document, DocHeader {
    // TODO add the rest of the fields, can be null. Convert primitives to Object.
    /**
     * Length of BP codes.
     */
    public static final int BP_ID_LENGTH = 5;

    /**
     * Increment between line numbers in an unprocessed SO. 
     */
    public static final int LINE_NUMBER_INCREMENT = 10;

    /**
     * Table name in iDempiere associated with this line object.
     */
    public static final String TABLE_NAME = "C_Order";

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
    public Date dateOrdered;

    /**
     * Has to exactly match {@link #dateOrdered}.
     */
    public Date datePromised;

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
     * Last / largest line number associated with this header.
     */
    private int latestLineNumber = 0;

    /**
     * Mapping between this class's field / instance variable names and iDempiere
     * column names. The column names follow iDempiere Template format
     * specifications.
     */
    public static Map<String, String> fieldColumnMap;

    static {
        HashMap<String, String> tempFieldColumnMap = new HashMap<>();
        tempFieldColumnMap.put("org", "AD_Org_ID[Name]");
        tempFieldColumnMap.put("documentNo", "DocumentNo/K");
        tempFieldColumnMap.put("description", "Description");
        tempFieldColumnMap.put("docType", "C_DocTypeTarget_ID[Name]");
        tempFieldColumnMap.put("dateOrdered", "DateOrdered");
        tempFieldColumnMap.put("datePromised", "DatePromised");
        tempFieldColumnMap.put("bpCode", "C_BPartner_ID[Value]");
        tempFieldColumnMap.put("invoiceBpCode", "Bill_BPartner_ID[Value]");
        tempFieldColumnMap.put("bpLocation", "C_BPartner_Location_ID[Name]");
        tempFieldColumnMap.put("invoiceBpLocation", "Bill_Location_ID[Name]");
        tempFieldColumnMap.put("warehouse", "M_Warehouse_ID[Value]");
        tempFieldColumnMap.put("orgTrx", "AD_OrgTrx_ID[Name]");
        fieldColumnMap = Collections.unmodifiableMap(tempFieldColumnMap);
    }

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
        this.org = SalesOrderUtils.orgMap.get(bizzySo.soff_code);
        this.description = bizzySo.description;
        this.dateOrdered = bizzySo.dateOrdered;
        this.datePromised = this.dateOrdered;
        this.bpCode = SalesOrderUtils.prependZeros(bizzySo.bpHoldingNo, BP_ID_LENGTH);
        this.invoiceBpCode = this.bpCode;
        this.bpLocation = bizzySo.bpLocationName;
        this.invoiceBpLocation = this.bpLocation;
        this.warehouse = SalesOrderUtils.warehouseMap.get(bizzySo.soff_code);

        StringBuilder sb = new StringBuilder("O");
        sb.append(bizzySo.orderSource);
        sb.append(SalesOrderUtils.getBPLocationIsTax(bizzySo.bpLocationName) ? "T" : "N");
        this.docType = SalesOrderUtils.docTypeMap.get(sb.toString());

        String principal = bizzySo.orderLines[0].principalId;
        this.orgTrx = SalesOrderUtils.getOrgTrx(this.bpCode, principal);

        // org.compiere.model.PO#saveNew()
        PO po = SalesOrderUtils.getMOrderPO(SalesOrderUtils.orgIdMap.get(this.org), SalesOrderUtils.orgTrxIdMap.get(this.orgTrx), bizzySo.dateOrdered);
        this.documentNo = DB.getDocumentNo(SalesOrderUtils.docTypeIdMap.get(this.docType), null, false, po);

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
    public Document[] getLines() {
        return this.orderLines;
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

            sb.append(columnName + ": " + value + "\n\n");
        }

        return sb.toString();
    }

    /**
     * Helper method to compute toString without using Reflection as a failsafe.
     * 
     * @return contents of the SO header
     */
    private String toStringNoReflection() {
        StringBuilder sb = new StringBuilder();
        sb.append(fieldColumnMap.get("org") + this.org + "\n\n");
        sb.append(fieldColumnMap.get("documentNo") + this.documentNo + "\n\n");
        sb.append(fieldColumnMap.get("description") + this.description + "\n\n");
        sb.append(fieldColumnMap.get("docType") + this.docType + "\n\n");
        sb.append(fieldColumnMap.get("dateOrdered") + this.dateOrdered + "\n\n");
        sb.append(fieldColumnMap.get("datePromised") + this.datePromised + "\n\n");
        sb.append(fieldColumnMap.get("bpCode") + this.bpCode + "\n\n");
        sb.append(fieldColumnMap.get("invoiceBpCode") + this.invoiceBpCode + "\n\n");
        sb.append(fieldColumnMap.get("bpLocation") + this.bpLocation + "\n\n");
        sb.append(fieldColumnMap.get("invoiceBpLocation") + this.invoiceBpLocation + "\n\n");
        sb.append(fieldColumnMap.get("warehouse") + this.warehouse + "\n\n");
        sb.append(fieldColumnMap.get("orgTrx") + this.orgTrx + "\n\n");
        return sb.toString();
    }
}
