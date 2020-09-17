package com.sahabatabadi.api.salesorder;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.sahabatabadi.api.DocHeader;
import com.sahabatabadi.api.DocLine;
import com.sahabatabadi.api.rmi.MasterDataNotFoundException;

/**
 * Class to represent required information to inject a sales order line into SAS
 * iDempiere. Has methods to convert external SO line classes to SAS SO line.
 * Used in {@link SASSalesOrder}
 * 
 * <p>
 * Avoid editing fields manually; rather, use the constructors to convert
 * external SO line into SAS SO line.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASSalesOrderLine implements DocLine {
    /**
     * Table name in iDempiere associated with this line object.
     */
    public static final String TABLE_NAME = "C_OrderLine";

    /**
     * Line number of the SO. Typically incremented by
     * {@link SASSalesOrder#LINE_NUMBER_INCREMENT} between different SO lines.
     */
    public int lineNo;

    /**
     * Product ID of the product being ordered. Has to match the entries in the
     * {@code value} field in iDempiere's {@code M_Product} table.
     */
    public String productId;

    // private String chargeType;

    /**
     * Quantity of the product being ordered.
     */
    public BigDecimal quantity;

    // private String uom;
    // private BigDecimal priceEntered;
    // private String taxScheme;
    // private String description;
    // private BigDecimal listPrice;
    // private BigDecimal discountPercent;
    // private BigDecimal freightAmt;
    // private String shipperName;
    // private String project;
    // private String activity;
    // private String campaign;
    // private String orgTrx;
    // private String costCenter;
    // private boolean processed;

    /**
     * Has to exactly match {@link SASSalesOrder#documentNo} in the header.
     */
    public String documentNo;

    /**
     * Has to exactly match {@link SASSalesOrder#datePromised} in the header.
     */
    public Date datePromised;

    // private String attributeSetInstance;
    // private String resourceAssignment;
    // private String sourceOrderLine;
    // private String discountScheme;
    // private boolean affectPromo;

    /**
     * SO header associated with this SO line.
     */
    private SASSalesOrder header;

    protected static CLogger log = CLogger.getCLogger(SASSalesOrderLine.class);

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
        // tempFieldColumnMap.put("chargeType", "C_OrderLine>C_Charge_ID[Name]");
        tempFieldColumnMap.put("quantity", "C_OrderLine>QtyEntered");
        // tempFieldColumnMap.put("uom", "C_OrderLine>C_UOM_ID[Name]");
        // tempFieldColumnMap.put("priceEntered", "C_OrderLine>PriceEntered");
        // tempFieldColumnMap.put("taxScheme", "C_OrderLine>C_Tax_ID[Name]");
        // tempFieldColumnMap.put("description", "C_OrderLine>Description");
        // tempFieldColumnMap.put("listPrice", "C_OrderLine>PriceList");
        // tempFieldColumnMap.put("discountPercent", "C_OrderLine>Discount");
        // tempFieldColumnMap.put("freightAmt", "C_OrderLine>FreightAmt");
        // tempFieldColumnMap.put("shipperName", "C_OrderLine>M_Shipper_ID[Name]");
        // tempFieldColumnMap.put("project", "C_OrderLine>C_Project_ID[Value]");
        // tempFieldColumnMap.put("activity", "C_OrderLine>C_Activity_ID[Value]");
        // tempFieldColumnMap.put("campaign", "C_OrderLine>C_Campaign_ID[Value]");
        // tempFieldColumnMap.put("orgTrx", "C_OrderLine>AD_OrgTrx_ID[Name]");
        // tempFieldColumnMap.put("costCenter", "C_OrderLine>User1_ID[Value]");
        // tempFieldColumnMap.put("processed", "C_OrderLine>Processed");
        tempFieldColumnMap.put("documentNo", "C_OrderLine>C_Order_ID[DocumentNo]/K");
        tempFieldColumnMap.put("datePromised", "C_OrderLine>DatePromised");
        // tempFieldColumnMap.put("attributeSetInstance", "C_OrderLine>M_AttributeSetInstance_ID");
        // tempFieldColumnMap.put("resourceAssignment", "C_OrderLine>S_ResourceAssignment_ID");
        // tempFieldColumnMap.put("sourceOrderLine", "C_OrderLine>Source_OrderLine_ID");
        // tempFieldColumnMap.put("discountScheme", "C_OrderLine>SAS_DiscountList_ID[Value]");
        // tempFieldColumnMap.put("affectPromo", "C_OrderLine>IsAffectPromo");
        fieldColumnMap = Collections.unmodifiableMap(tempFieldColumnMap);
    }

    /**
     * Default constructor.
     * 
     * @param orderLine Bizzy SO line object to convert to SAS SO line object.
     * @param header    SAS SO header object associated with this SO line.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the orderLine argument is not found i.e.
     *                                     constructor encountered another exception
     */
    public SASSalesOrderLine(BizzySalesOrderLine orderLine, SASSalesOrder header) throws MasterDataNotFoundException {
        this(orderLine, header, false);
    }

    /**
     * Validation constructor.
     * 
     * @param orderLine      Bizzy SO line object to convert to SAS SO line object.
     * @param header         SAS SO header object associated with this SO line.
     * @param skipValidation If true, skip validation.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the orderLine argument is not found i.e.
     *                                     constructor encountered another exception
     */
    public SASSalesOrderLine(BizzySalesOrderLine orderLine, SASSalesOrder header, boolean skipValidation) throws MasterDataNotFoundException {
        try {
            if (!skipValidation) {
                validateBizzySoLineData(orderLine);
            }

            /* parsing values from Bizzy SO Line */
            this.header = header;
            this.productId = orderLine.productCode;
            this.quantity = new BigDecimal(orderLine.quantity);

            /* calculating values */
            this.lineNo = header.getNextLineNumber();
            this.documentNo = this.header.documentNo;
            this.datePromised = this.header.datePromised;
        } catch (MasterDataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // TODO insert general exception to API error log?
        }
    }
    
    /**
     * Validates whether all fields inside the bizzy SO line is properly filled.
     * 
     * @param bizzySoLine Bizzy SO Line object to convert to validate.
     * @throws MasterDataNotFoundException thrown if the specified master data in
     *                                     the bizzySo argument is not found i.e.
     *                                     constructor encountered another exception
     */
    public static void validateBizzySoLineData(BizzySalesOrderLine bizzySoLine) throws MasterDataNotFoundException {
        try {
            if (bizzySoLine.productCode == null)
                throw new MasterDataNotFoundException("Product Code is empty!");
            if (!SalesOrderUtils.checkProductCode(bizzySoLine.productCode))
                throw new MasterDataNotFoundException("Incorrect Product Code, Product cannot be found!");

            if (bizzySoLine.quantity <= 0) 
                throw new MasterDataNotFoundException("Product quantity must be a positive value!");
        } catch (MasterDataNotFoundException e) {
            String errMsg = String.format(
                    "Master data error in SAS SO line: %s\nBizzy SO line content: \n%s",
                    e.getMessage(), bizzySoLine.toString());
            if (log.isLoggable(Level.WARNING))
                log.warning(errMsg);
            throw new MasterDataNotFoundException(errMsg);
        }
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
    public DocHeader getHeader() {
        return this.header;
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

            if (soField.getType().isArray()) {
                sb.append(soField.getName() + "\t: [");
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    Object arrayElement = Array.get(value, i);
                    sb.append("{\n").append(arrayElement.toString()).append("},\n");
                }
                sb.append("]");
            } else {
                sb.append(soField.getName() + "\t: " + value + "\n");
            }
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
        sb.append(fieldColumnMap.get("lineNo") + this.lineNo + "\n");
        sb.append(fieldColumnMap.get("productId") + this.productId + "\n");
        sb.append(fieldColumnMap.get("quantity") + this.quantity + "\n");
        sb.append(fieldColumnMap.get("documentNo") + this.documentNo + "\n");
        sb.append(fieldColumnMap.get("datePromised") + this.datePromised + "\n");
        return sb.toString();
    }
}
