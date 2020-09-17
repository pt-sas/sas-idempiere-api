package com.sahabatabadi.api.salesorder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.sahabatabadi.api.DocHeader;
import com.sahabatabadi.api.DocLine;
import com.sahabatabadi.api.rmi.MasterDataNotFoundException;

/**
 * Class to represent required information to inject a sales order header into
 * SAS iDempiere. Has methods to convert external SO classes to SAS SO.
 * 
 * <p>
 * Avoid editing fields manually; rather, use the constructors to convert
 * external SO into SAS SO.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASSalesOrder implements DocHeader {
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

    // private String poReference;

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

    // private String bpContact;
    // private String invoiceBpContact;
    // private String deliveryRule;
    // private String priorityRule;

    /**
     * Name of the default warehouse of the associated {@link #org}. Has to match
     * entries in the {@code name} field in iDempiere's {@code M_Warehouse} table.
     */
    public String warehouse;

    // private String dropshipBpCode;
    // private String dropshipBpLocation;
    // private String dropshipBpContact;
    // private String deliveryViaRule;
    // private String shipperName;
    // private String freightCostRule;
    // private String freightCategory;
    // private BigDecimal freightAmount;
    // private boolean isPriviledgedRate;
    // private String invoiceRule;
    // private String pricelist;
    // private String currencyConversionType;
    // private String chargeType;
    // private boolean chargeAmount;
    // private String paymentTerm;
    // private String promotionCode;
    // private String project;
    // private String campaign;

    /**
     * Org Trx's full name. Has to match entries in the {@code name} field in
     * iDempiere's {@code AD_Org} table, where the {@code isorgtrxdim} field is
     * {@code 'Y'}.
     */
    public String orgTrx;

    // private String costCenter;
    // private String cashplanLine;
    // private boolean isCustWalkIn;
    // private String flnStatus;
    // private String picklistNote;
    // private String shipmentNote;
    // private String invoiceNote;
    // private String notaClaimNo;
    // private Date notaClaimDate;
    // private String reasonClaim;

    /**
     * Array of {@link SASSalesOrderLine} object associated with this header object.
     */
    public SASSalesOrderLine[] orderLines;

    protected static CLogger log = CLogger.getCLogger(SASSalesOrder.class);

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
        // tempFieldColumnMap.put("poReference", "POReference");
        tempFieldColumnMap.put("description", "Description");
        tempFieldColumnMap.put("docType", "C_DocTypeTarget_ID[Name]");
        tempFieldColumnMap.put("dateOrdered", "DateOrdered");
        tempFieldColumnMap.put("datePromised", "DatePromised");
        tempFieldColumnMap.put("bpCode", "C_BPartner_ID[Value]");
        tempFieldColumnMap.put("invoiceBpCode", "Bill_BPartner_ID[Value]");
        tempFieldColumnMap.put("bpLocation", "C_BPartner_Location_ID[Name]");
        tempFieldColumnMap.put("invoiceBpLocation", "Bill_Location_ID[Name]");
        // tempFieldColumnMap.put("bpContact", "AD_User_ID[Name]");
        // tempFieldColumnMap.put("invoiceBpContact", "Bill_User_ID[Name]");
        // tempFieldColumnMap.put("deliveryRule", "DeliveryRule");
        // tempFieldColumnMap.put("priorityRule", "PriorityRule");
        tempFieldColumnMap.put("warehouse", "M_Warehouse_ID[Value]");
        // tempFieldColumnMap.put("dropshipBpCode", "DropShip_BPartner_ID[Value]");
        // tempFieldColumnMap.put("dropshipBpLocation", "DropShip_Location_ID[Name]");
        // tempFieldColumnMap.put("dropshipBpContact", "DropShip_User_ID[Name]");
        // tempFieldColumnMap.put("deliveryViaRule", "DeliveryViaRule");
        // tempFieldColumnMap.put("shipperName", "M_Shipper_ID[Name]");
        // tempFieldColumnMap.put("freightCostRule", "FreightCostRule");
        // tempFieldColumnMap.put("freightCategory", "M_FreightCategory_ID[Value]");
        // tempFieldColumnMap.put("freightAmount", "FreightAmt");
        // tempFieldColumnMap.put("isPriviledgedRate", "IsPriviledgedRate");
        // tempFieldColumnMap.put("invoiceRule", "InvoiceRule");
        // tempFieldColumnMap.put("pricelist", "M_PriceList_ID[Name]");
        // tempFieldColumnMap.put("currencyConversionType", "C_ConversionType_ID[Value]");
        // tempFieldColumnMap.put("chargeType", "C_Charge_ID[Name]");
        // tempFieldColumnMap.put("chargeAmount", "ChargeAmt");
        // tempFieldColumnMap.put("paymentTerm", "C_PaymentTerm_ID[Value]");
        // tempFieldColumnMap.put("promotionCode", "PromotionCode");
        // tempFieldColumnMap.put("project", "C_Project_ID[Value]");
        // tempFieldColumnMap.put("campaign", "C_Campaign_ID[Value]");
        tempFieldColumnMap.put("orgTrx", "AD_OrgTrx_ID[Name]");
        // tempFieldColumnMap.put("costCenter", "User1_ID[Value]");
        // tempFieldColumnMap.put("cashplanLine", "C_CashPlanLine_ID[Name]");
        // tempFieldColumnMap.put("isCustWalkIn", "IsTest");
        // tempFieldColumnMap.put("flnStatus", "FLNStatus");
        // tempFieldColumnMap.put("picklistNote", "PickListNote");
        // tempFieldColumnMap.put("shipmentNote", "ShipmentNote");
        // tempFieldColumnMap.put("invoiceNote", "InvoiceNote");
        // tempFieldColumnMap.put("notaClaimNo", "NCNo");
        // tempFieldColumnMap.put("notaClaimDate", "NCDate");
        // tempFieldColumnMap.put("reasonClaim", "reason");
        fieldColumnMap = Collections.unmodifiableMap(tempFieldColumnMap);
    }

    /**
     * Default constructor, always validate.
     * 
     * <p>
     * This class requires the specified bizzySo to have at least one SO line, and
     * also requires all SO lines to have identical product principal and product
     * discount. In other words, the Bizzy SO headers have to be grouped by
     * principal and discount prior to being passed in to this constructor.
     * 
     * <p>
     * Furthermore, this constructor always validates whether the bizzy SO content
     * conforms to iDempiere requirements.
     * 
     * @param bizzySo Bizzy SO object to convert to SAS SO object.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the bizzySo argument is not found i.e.
     *                                     constructor encountered another exception
     * 
     * @see org.compiere.model.PO#saveNew()
     */
    public SASSalesOrder(BizzySalesOrder bizzySo) throws MasterDataNotFoundException {
        this(bizzySo, false);
    }

    /**
     * Validation constructor.
     * 
     * <p>
     * This class requires the specified bizzySo to have at least one SO line, and
     * also requires all SO lines to have identical product principal and product
     * discount. In other words, the Bizzy SO headers have to be grouped by
     * principal and discount prior to being passed in to this constructor.
     * 
     * <p>
     * Furthermore, this constructor validates whether the bizzy SO content conforms
     * to iDempiere requirements if the skipValidation argument is false.
     * 
     * @param bizzySo        Bizzy SO object to convert to SAS SO object.
     * @param skipValidation If true, skip validation.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the bizzySo argument is not found i.e.
     *                                     constructor encountered another exception
     * 
     * @see org.compiere.model.PO#saveNew()
     */
    public SASSalesOrder(BizzySalesOrder bizzySo, boolean skipValidation) throws MasterDataNotFoundException {
        try {
        	if (!skipValidation) {
                validateBizzySoData(bizzySo);
            }
        	
            this.org = SalesOrderUtils.orgMap.get(bizzySo.soff_code);

            this.description = bizzySo.description;

            this.dateOrdered = bizzySo.dateOrdered;
            this.datePromised = this.dateOrdered;

            this.bpCode = bizzySo.bpHoldingCode;
            this.invoiceBpCode = this.bpCode;

            this.bpLocation = bizzySo.bpLocationCode;
            this.invoiceBpLocation = this.bpLocation;

            this.warehouse = SalesOrderUtils.warehouseMap.get(bizzySo.soff_code);

            StringBuilder sb = new StringBuilder("O");
            sb.append(bizzySo.orderSource);
            sb.append(SalesOrderUtils.getBPLocationIsTax(bizzySo.bpLocationCode) ? "T" : "N");
            this.docType = SalesOrderUtils.docTypeMap.get(sb.toString());

            String principal = bizzySo.orderLines[0].principalId;
            this.orgTrx = SalesOrderUtils.getOrgTrx(this.bpCode, principal);

            // org.compiere.model.PO#saveNew()
            PO po = SalesOrderUtils.getMOrderPO(SalesOrderUtils.orgIdMap.get(this.org),
                    SalesOrderUtils.orgTrxIdMap.get(this.orgTrx), bizzySo.dateOrdered);
            this.documentNo = DB.getDocumentNo(SalesOrderUtils.docTypeIdMap.get(this.docType), null, false, po);
            
            this.orderLines = new SASSalesOrderLine[bizzySo.orderLines.length];
            for (int i = 0; i < orderLines.length; i++) {
                this.orderLines[i] = new SASSalesOrderLine(bizzySo.orderLines[i], this, skipValidation);
            }
        } catch (MasterDataNotFoundException e) {
        	throw e;
        } catch (Exception e) {
            // TODO insert general exception to API error log?
        }
    }

    /**
     * Validates whether all fields inside the bizzy SO is properly filled.
     * 
     * @param bizzySo Bizzy SO object to convert to validate.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the bizzySo argument is not found i.e.
     *                                     constructor encountered another exception
     */
    public static void validateBizzySoData(BizzySalesOrder bizzySo) throws MasterDataNotFoundException {
        try {
            if (SalesOrderUtils.orgMap.get(bizzySo.soff_code) == null)
                throw new MasterDataNotFoundException("Incorrect SOFF Code, Org cannot be found! ", null);

            // description can be left empty, skip check

            if (bizzySo.dateOrdered == null)
                throw new MasterDataNotFoundException("Date Ordered is empty!");

            if (bizzySo.bpHoldingCode == null)
                throw new MasterDataNotFoundException("BP Holding Code is empty!");
            if (!SalesOrderUtils.checkBpCode(bizzySo.bpHoldingCode))
                throw new MasterDataNotFoundException("Incorrect BP Holding Code, BP cannot be found!");

            if (bizzySo.bpLocationCode == null)
                throw new MasterDataNotFoundException("BP Holding Code is empty!");
            // if (!SalesOrderUtils.checkBpCode(bizzySo.bpLocationCode))
            //     throw new MasterDataNotFoundException("Incorrect BP Location Code, BP Location cannot be found!");
            // TODO later change bp location code to use C_BPartner_Location_ID

        if (bizzySo.orderSource != 'B' && bizzySo.orderSource != 'S')
            throw new MasterDataNotFoundException("Incorrect Order Source, has to either be 'B' or 'S'! ");
        } catch (MasterDataNotFoundException e) {
            String errMsg = String.format("Master data error in SAS SO header: %s\nBizzy SO header content: \n%s\n.",
                    e.getMessage(), bizzySo.toString());
            if (log.isLoggable(Level.WARNING))
                log.warning(errMsg);
            throw new MasterDataNotFoundException(errMsg);
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
    public DocLine[] getLines() {
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
