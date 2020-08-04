package com.sahabatabadi.api.salesorder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SOUtils {
    public static final Map<Character, String> orgMap;
    public static final Map<String, Integer> orgIdMap;
    public static final Map<String, String> orgTrxMap;
    public static final Map<String, Integer> orgTrxIdMap;
    public static final Map<Character, String> warehouseMap;
    public static final Map<String, String> docTypeMap;
    public static final Map<String, Integer> docTypeIdMap;

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


    public static String getOrgTrx(String bpHoldingId, String principal) {
        if (principal.equals("Philips")) {
            String retValue = null;
            String orgTrxQuery = "SELECT org.name\n" + "FROM C_BPartner bp, SAS_BPRule r, AD_Org org\n"
                    + "WHERE bp.value = ? " + "    AND bp.c_bpartner_id = r.c_bpartner_id "
                    + "    AND r.ad_orgtrx_id = org.ad_org_id "
                    + "    AND (org.name LIKE 'TR%' OR org.name LIKE 'TGR')\n";

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

    public static boolean getBPLocationIsTax(String bpLocation) {
        String retValue = null;
        String isTaxQuery = 
            "SELECT istax\n" + 
            "FROM C_BPartner_Location\n" + 
            "WHERE name LIKE ?\n";

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

    public static String prependZeros(int no, int totalLength) {
        String noString = Integer.toString(no);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalLength - noString.length(); i++) {
            sb.append("0");
        }
        sb.append(noString);
        return sb.toString();
    }
}