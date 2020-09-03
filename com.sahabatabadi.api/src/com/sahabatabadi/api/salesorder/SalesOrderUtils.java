package com.sahabatabadi.api.salesorder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.IModelFactory;
import org.adempiere.base.Service;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * Utility class to query the iDempiere database and store simple mappings.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SalesOrderUtils {
    protected static CLogger log = CLogger.getCLogger(SalesOrderUtils.class);

    /**
     * Mapping between Org character (A/B/C/D/M) and Org name.
     */
    public static final Map<Character, String> orgMap;

    /**
     * Mapping between Org name and {@code ad_org_id} field in iDempiere's
     * {@code AD_Org} table.
     */
    public static final Map<String, Integer> orgIdMap;

    /**
     * Mapping between an Org Trx's full name and short name (e.g. "TR1", "PAN")
     */
    public static final Map<String, String> orgTrxMap;

    /**
     * Mapping between an Org Trx's short name (e.g. "TR1", "PAN") and
     * {@code ad_org_id} field in iDempiere's {@code AD_Org} table.
     */
    public static final Map<String, Integer> orgTrxIdMap;

    /**
     * Mapping between Org character (A/B/C/D/M) and the Org's default warehouse
     * name.
     */
    public static final Map<Character, String> warehouseMap;

    /**
     * Mapping between document type's 3-letter initial with document type's full
     * name
     */
    public static final Map<String, String> docTypeMap;

    /**
     * Mapping between document type's full name with {@code c_doctype_id} field in
     * iDempiere's {@code C_DocType} table.
     */
    public static final Map<String, Integer> docTypeIdMap;

    /**
     * Initializes the mappings.
     */
    static {
        HashMap<Character, String> tempOrgMap = new HashMap<>();
        tempOrgMap.put('A', "Sunter");
        tempOrgMap.put('B', "Tebet");
        tempOrgMap.put('C', "Glodok");
        tempOrgMap.put('D', "Kenari");
        tempOrgMap.put('M', "Tangerang");
        orgMap = Collections.unmodifiableMap(tempOrgMap);

        HashMap<String, Integer> tempOrgIdMap = new HashMap<>();
        tempOrgIdMap.put("Sunter", 1000001);
        tempOrgIdMap.put("Tebet", 1000002);
        tempOrgIdMap.put("Glodok", 1000003);
        tempOrgIdMap.put("Kenari", 1000004);
        tempOrgIdMap.put("Tangerang", 2200019);
        orgIdMap = Collections.unmodifiableMap(tempOrgIdMap);

        HashMap<String, String> tempOrgTrxMap = new HashMap<>();
        tempOrgTrxMap.put("Panasonic", "PAN");
        tempOrgTrxMap.put("Legrand", "LEG");
        tempOrgTrxMap.put("Schneider", "SCH");
        tempOrgTrxMap.put("Supreme", "SUP");
        orgTrxMap = Collections.unmodifiableMap(tempOrgTrxMap);

        HashMap<String, Integer> tempOrgTrxIdMap = new HashMap<>();
        tempOrgTrxIdMap.put("TR1", 1000006);
        tempOrgTrxIdMap.put("TR2", 1000008);
        tempOrgTrxIdMap.put("TR3", 2200020);
        tempOrgTrxIdMap.put("TR4", 1000010);
        tempOrgTrxIdMap.put("TR5", 2200021);
        tempOrgTrxIdMap.put("TGR", 2200022);
        tempOrgTrxIdMap.put("PAN", 1000011);
        tempOrgTrxIdMap.put("LEG", 1000012);
        tempOrgTrxIdMap.put("SCH", 1000023);
        tempOrgTrxIdMap.put("SUP", 1000025);
        orgTrxIdMap = Collections.unmodifiableMap(tempOrgTrxIdMap);

        HashMap<Character, String> tempWarehouseMap = new HashMap<>();
        tempWarehouseMap.put('A', "Sunter F1-2");
        tempWarehouseMap.put('B', "Tebet");
        tempWarehouseMap.put('C', "Glodok");
        tempWarehouseMap.put('D', "Kenari");
        tempWarehouseMap.put('M', "Tangerang");
        warehouseMap = Collections.unmodifiableMap(tempWarehouseMap);

        HashMap<String, String> tempDocTypeMap = new HashMap<>();
        tempDocTypeMap.put("OBN", "OBN (Online BFF Non tax)");
        tempDocTypeMap.put("OBT", "OBT (Online BFF Tax)");
        tempDocTypeMap.put("OSN", "OSN (Online Toko Smart Non tax)");
        tempDocTypeMap.put("OST", "OST (Online Toko Smart Tax)");
        docTypeMap = Collections.unmodifiableMap(tempDocTypeMap);

        HashMap<String, Integer> tempDocTypeIdMap = new HashMap<>();
        tempDocTypeIdMap.put("OBN (Online BFF Non tax)", 2200042);
        tempDocTypeIdMap.put("OBT (Online BFF Tax)", 2200043);
        tempDocTypeIdMap.put("OSN (Online Toko Smart Non tax)", 2200044);
        tempDocTypeIdMap.put("OST (Online Toko Smart Tax)", 2200045);
        docTypeIdMap = Collections.unmodifiableMap(tempDocTypeIdMap);
    }

    /**
     * Queries the database for an Org Trx given a BP number and principal.
     * 
     * Philips/Signify has different OrgTrx-es for each region, while other
     * principals only have one OrgTrx.
     * 
     * @param bpHoldingId Five-digit BP number. For example, for "PIONEER ELEKTRIC",
     *                    the value is {@code 03806}.
     * @param principal   The full principal name. Has to match the entries in
     *                    iDempiere's {@code M_Product} table. Example:
     *                    {@code "Philips"}.
     * @return Full Org Trx name of the principal, from the {@code name} field in
     *         the {@code AD_Org table}.
     */
    public static String getOrgTrx(String bpHoldingId, String principal) {
        if (principal.equals("Philips")) {
            String retValue = null;
            String orgTrxQuery = new StringBuilder()
                .append("SELECT org.name\n") 
                .append("FROM C_BPartner bp, SAS_BPRule r, AD_Org org\n") 
                .append("WHERE bp.value = ? ") 
                .append("    AND bp.c_bpartner_id = r.c_bpartner_id ") 
                .append("    AND r.ad_orgtrx_id = org.ad_org_id ") 
                .append("    AND (org.name LIKE 'TR%' OR org.name LIKE 'TGR');")
                .toString();

            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = DB.prepareStatement(orgTrxQuery, null);
                pstmt.setString(1, bpHoldingId);
                rs = pstmt.executeQuery();
                if (rs.next())
                    retValue = rs.getString(1);
            } catch (Exception e) {
                log.log(Level.SEVERE, orgTrxQuery, e);
            } finally {
                DB.close(rs, pstmt);
                rs = null;
                pstmt = null;
            }

            return retValue;
        } else {
            return orgTrxMap.get(principal);
        }
    }

    /**
     * Queries the database the tax status of the specified BP location.
     * 
     * @param bpLocation Exact name of the business partner's (BP) or customer's
     *                   location. Value has to exactly match the name in
     *                   iDempiere's {@code C_BPartner_Location} table. Example:
     *                   {@code "PIONIR ELEKTRIK INDONESIA [Kenari Mas Jl. Kramat Raya Lt.
     *                   Dasar Blok C No. 3-5]"}
     * @return True if the BP location is a tax location, false otherwise.
     */
    public static boolean getBPLocationIsTax(String bpLocation) {
        String retValue = null;
        String isTaxQuery = new StringBuilder()
            .append("SELECT istax\n") 
            .append("FROM C_BPartner_Location\n") 
            .append("WHERE name LIKE ?;")
            .toString();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(isTaxQuery, null);
            pstmt.setString(1, bpLocation);
            rs = pstmt.executeQuery();
            if (rs.next())
                retValue = rs.getString(1);
        } catch (Exception e) {
            log.log(Level.SEVERE, isTaxQuery, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }

        if (retValue == null)
            log.fine("-");
        else if (log.isLoggable(Level.FINE))
            log.fine(retValue.toString());

        if (retValue.equals("Y")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Queries the database for the product's principal/brand given a product ID.
     * 
     * @param productId Product ID of the product being queried. Has to match the
     *                  entries in iDempiere's {@code M_Product} table. Example:
     *                  {@code "AB0301485"}.
     * @return Full name of the product's principal/brand, from the {@code name}
     *         field in the {@code M_Product_Category} table.
     */
    public static String getProductPrincipal(String productId) {
        String principal = null;
        String principalQuery = new StringBuilder()
            .append("SELECT c.name\n") 
            .append("FROM M_Product p, M_Product_Category c\n") 
            .append("WHERE p.value = ?\n") 
            .append("    AND p.m_product_category_id = c.m_product_category_id;")
            .toString();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(principalQuery, null);
            pstmt.setString(1, productId);
            rs = pstmt.executeQuery();
            if (rs.next())
                principal = rs.getString(1);
        } catch (Exception e) {
            log.log(Level.SEVERE, principalQuery, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
        return principal;
    }

    /**
     * Queries the database for the product's discount given the product ID, the BP
     * number, and the product principal.
     * 
     * @param productId   Product ID of the product being queried. Has to match the
     *                    entries in iDempiere's {@code M_Product} table. Example:
     *                    {@code "AB0301485"}.
     * @param bpHoldingNo Five-digit BP number. For example, for "PIONEER ELEKTRIC",
     *                    the value is {@code 03806}.
     * @param principal   The full principal name. Has to match the entries in
     *                    iDempiere's {@code M_Product} table. Example:
     *                    {@code "Philips"}.
     * @return The product's discount list ID, from the {@code sas_discountlist_id}
     *         field in the {@code M_DiscountSchemaBreak} table.
     */
    public static int getProductDiscount(String productId, int bpHoldingNo, String principal) {
        int discount = -1;
        String discountQuery = new StringBuilder()
            .append("SELECT brk.sas_discountlist_id\n") 
            .append("FROM M_Product p, \n") 
            .append("    C_BPartner bp, \n") 
            .append("    SAS_BPRule r, \n") 
            .append("    AD_Org o, \n") 
            .append("    M_DiscountSchemaBreak brk\n") 
            // .append("    SAS_DiscountSchemaBreakLine discl\n") 
            .append("WHERE p.value = ? AND bp.value = ? AND o.name = ? \n") // kode product, bp number, org trx
            .append("    AND bp.c_bpartner_id = r.c_bpartner_id \n") 
            .append("    AND r.ad_orgtrx_id = o.ad_org_id \n") 
            .append("    AND brk.group1 = p.group1 \n") 
            .append("    AND brk.m_discountschema_id = r.m_discountschema_id;")
            // .append("    AND brk.sas_discountlist_id = discl.sas_discountlist_id;")
            .toString();
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(discountQuery, null);
            pstmt.setString(1, productId);
            String bpCode = SalesOrderUtils.prependZeros(bpHoldingNo, SASSalesOrder.BP_ID_LENGTH);
            pstmt.setString(2, bpCode);
            pstmt.setString(3, SalesOrderUtils.getOrgTrx(bpCode, principal));
            rs = pstmt.executeQuery();
            if (rs.next())
                discount = rs.getInt(1);
        } catch (Exception e) {
            log.log(Level.SEVERE, discountQuery, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }
        return discount;
    }

    /**
     * Prepends zeros to the specified integer until the number reaches the
     * specified length.
     * 
     * @param no          The integer / number to be prepended with zeros.
     * @param totalLength The desired length of the number.
     * @return The number prepended with zeros.
     */
    public static String prependZeros(int no, int totalLength) {
        String noString = Integer.toString(no);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalLength - noString.length(); i++) {
            sb.append("0");
        }
        sb.append(noString);
        return sb.toString();
    }

    /**
     * Helper method to return a Sales Order PO object to be used to generate a new
     * document number.
     * 
     * @param orgId       Org ID of the current SO header. Has to match entries in
     *                    the {@code ad_org_id} field in iDempiere's {@code AD_Org}
     *                    table, where the {@code isorgtrxdim} field is {@code 'N'}.
     * @param orgTrxId    Org Trx ID of the current SO header. Has to match entries
     *                    in the {@code ad_org_id} field in iDempiere's
     *                    {@code AD_Org} table, where the {@code isorgtrxdim} field
     *                    is {@code 'Y'}.
     * @param dateOrdered Date the SO is created, typically the current system
     *                    clock.
     * @return a Sales Order PO object containing the arguments.
     * 
     * @see org.compiere.model.MOrder
     * @see org.compiere.model.GridTable#dataSavePO(int)
     * @see org.compiere.model.MTable#getPO(int, String)
     */
    public static PO getMOrderPO(int orgId, int orgTrxId, Date dateOrdered) {
        // org.compiere.model.GridTable#dataSavePO(int)
        int Record_ID = 0; // 0 represents new PO
        String trxName = null;

        // org.compiere.model.MTable#getPO(int, String)
        String tableName = "C_Order";
        PO po = null;

        List<IModelFactory> factoryList = Service.locator().list(IModelFactory.class).getServices();
        if (factoryList != null) {
            for (IModelFactory factory : factoryList) {
                po = factory.getPO(tableName, Record_ID, trxName);
                if (po != null) {
                    if (po.get_ID() != Record_ID && Record_ID > 0)
                        po = null;
                    else
                        break;
                }
            }
        }

        po.set_ValueNoCheck("AD_Org_ID", orgId);
        po.set_ValueNoCheck("AD_OrgTrx_ID", orgTrxId);
        po.set_ValueNoCheck("DateOrdered", new Timestamp(dateOrdered.getTime()));

        return po;
    }
}