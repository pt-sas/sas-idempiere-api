package com.sahabatabadi.api.salesorder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.IModelFactory;
import org.adempiere.base.Service;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class SASSalesOrder {
	public static final int BP_ID_LENGTH = 5;

    public String org;                  // AD_Org_ID[Name]: Sunter
    public String documentNo;           // DocumentNo: ATR1-OPN-1001-0018
    public String description;          // Description: PK 8% + PROMNAS
    public String docType;              // C_DocTypeTarget_ID[Name]: OPN (Order Penjualan Non tax)
    public String dateOrdered;          // DateOrdered: 2020-01-02
    public String datePromised;         // DatePromised: 2020-01-02
    public String bpHoldingId;          // C_BPartner_ID[Value]
    public String invoiceBpHoldingId;   // Bill_BPartner_ID[Value]
    public String bpLocation;           // C_BPartner_Location_[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
    public String invoiceBpLocation;    // Bill_Location_ID[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
    public String bpContact;            // AD_User_ID[Name]: Yudi Bunadi Tjhie
    public String invoiceBpContact;     // Bill_User_ID[Name]: Yudi Bunadi Tjhie
    // public char deliveryRule;           // DeliveryRule
    // public char priorityRule;           // PriorityRule
    public String warehouse;            // M_Warehouse_ID[Value]: Sunter F1-2
    // public char deliveryViaRule;        // DeliveryViaRule
    // public char freightCostRule;        // FreightCostRule
    // public char invoiceRule;            // InvoiceRule
    // public String pricelist;            // M_PriceList_ID[Name]: SALES-IDR
    // public String paymentTerm;          // C_PaymentTerm_ID[Value]: 30 days
    // public String project;              // C_Project_ID[Value]: Retail
    public String orgTrx;               // AD_OrgTrx_ID[Name]: TR1

    public SASSalesOrderLine[] orderLines;

    protected CLogger log = CLogger.getCLogger(getClass());

    private HashMap<Character, String> orgMap = new HashMap<>();
    private HashMap<Character, String> orgTrxMap = new HashMap<>();
    private HashMap<Character, String> warehouseMap = new HashMap<>();

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private int latestLineNumber = 0;

    // huge caveat: this class expects the bizzySo argument to at least have one order line, and is split based on principal and discount
    public SASSalesOrder(BizzySalesOrder bizzySo) {
        initializeMaps();

        this.org = orgMap.get(bizzySo.soff_code);
        this.description = bizzySo.description;
        this.dateOrdered = formatter.format(bizzySo.dateOrdered);
        this.bpHoldingId = prependZeros(bizzySo.bpHoldingNo, BP_ID_LENGTH);
        this.bpLocation = bizzySo.bpLocationName;

        this.docType = "OPN (Order Penjualan Non tax)"; // TODO;
        this.datePromised = this.dateOrdered;
        this.warehouse = this.warehouseMap.get(bizzySo.soff_code);

        char principal = 'S'; // TODO; 
        
        if (principal == 'S') {
            this.orgTrx = getSignifyOrgTrx(this.bpHoldingId);
        } else {
            this.orgTrx = orgTrxMap.get(principal);
        }

        int docTypeId = 550265; // TODO docType TBD

        // org.compiere.model.PO::saveNew()
        PO po = getMOrderPO(this.orgIdMap.get(this.org), this.orgTrxIdMap.get(this.orgTrx), bizzySo.dateOrdered);
        this.documentNo = DB.getDocumentNo(docTypeId, null, false, po);

        orderLines = new SASSalesOrderLine[bizzySo.orderLines.length];
        for (int i = 0; i < orderLines.length; i++) {
            orderLines[i] = new SASSalesOrderLine(bizzySo.orderLines[i], this);
        }
    }

    protected int getNextLineNumber() {
        latestLineNumber += 10;
        return latestLineNumber;
    }

    /* TODO need to test query in operation */
    private String getSignifyOrgTrx(String bpHoldingId) {
        String retValue = null;
        String orgTrxQuery = 
            "SELECT org.name\n" + 
            "FROM C_BPartner bp, SAS_BPRule r, AD_Org org\n" + 
            "WHERE bp.value = ? " + 
            "    AND bp.c_bpartner_id = r.c_bpartner_id " +
            "    AND r.ad_orgtrx_id = org.ad_org_id " + 
            "    AND org.name LIKE 'TR%'\n";
            
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

        if (retValue == null)
            log.fine("-");
        else if (log.isLoggable(Level.FINE))
            log.fine(retValue.toString());

        return retValue;
    }

    private PO getMOrderPO(int orgId, int orgTrxId, Date dateOrdered) {
        // TODO hardcoded parameters, retrace 
        int Record_ID = 0;
        String trxName = null;
        String tableName = "C_Order";

        /* org.compiere.model.MTable::getPO(int, String) */
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

        /* prefix: @AD_Org_ID<Value>@@AD_OrgTrx_ID<Name>@-OPT-@DateOrdered<yyMM>@- */
        po.set_ValueNoCheck("AD_Org_ID", orgId);
        po.set_ValueNoCheck("AD_OrgTrx_ID", orgTrxId);
        po.set_ValueNoCheck("DateOrdered", new Timestamp(dateOrdered.getTime()));

        return po;
    }

    private String prependZeros(int no, int totalLength) {
        String noString = Integer.toString(no);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalLength - noString.length(); i++) {
            sb.append("0");
        }
        sb.append(noString);
        return sb.toString();
    }

    private void initializeMaps() {
        orgMap.put('A', "Sunter");
        orgMap.put('B', "Tebet");
        orgMap.put('C', "Glodok");
        orgMap.put('D', "Kenari");
        orgMap.put('M', "Tangerang");

        orgTrxMap.put('P', "PAN");
        orgTrxMap.put('L', "LEG");
        orgTrxMap.put('C', "SCH");
        orgTrxMap.put('U', "SUP");

        warehouseMap.put('A', "Sunter F1-2");
        warehouseMap.put('B', "Tebet");
        warehouseMap.put('C', "Glodok");
        warehouseMap.put('D', "Kenari");
        warehouseMap.put('M', "Tangerang");
    }
}
