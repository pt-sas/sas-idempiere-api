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

public class SASSalesOrder {
	public static final int BP_ID_LENGTH = 5;

    public String org;                  // AD_Org_ID[Name]: Sunter
    public String documentNo;           // DocumentNo/K: ATR1-OPN-1001-0018
    public String description;          // Description: PK 8% + PROMNAS
    public String docType;              // C_DocTypeTarget_ID[Name]: OPN (Order Penjualan Non tax)
    public String dateOrdered;          // DateOrdered: 2020-01-02
    public String datePromised;         // DatePromised: 2020-01-02
    public String bpHoldingId;          // C_BPartner_ID[Value]
    public String invoiceBpHoldingId;   // Bill_BPartner_ID[Value]
    public String bpLocation;           // C_BPartner_Location_[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
    public String invoiceBpLocation;    // Bill_Location_ID[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
    // public String bpContact;            // AD_User_ID[Name]: Yudi Bunadi Tjhie
    // public String invoiceBpContact;     // Bill_User_ID[Name]: Yudi Bunadi Tjhie
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

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private int latestLineNumber = 0;

    // huge caveat: this class expects the bizzySo argument to at least have one order line, and is split based on principal and discount
    public SASSalesOrder(BizzySalesOrder bizzySo) {
        this.org = SOUtils.orgMap.get(bizzySo.soff_code);
        this.description = bizzySo.description;
        this.dateOrdered = formatter.format(bizzySo.dateOrdered);
        this.datePromised = this.dateOrdered;
        this.bpHoldingId = SOUtils.prependZeros(bizzySo.bpHoldingNo, BP_ID_LENGTH);
        this.invoiceBpHoldingId = this.bpHoldingId;
        this.bpLocation = bizzySo.bpLocationName;
        this.invoiceBpLocation = this.bpLocation;
        this.warehouse = SOUtils.warehouseMap.get(bizzySo.soff_code);

        StringBuilder sb = new StringBuilder("O");
        sb.append(bizzySo.orderSource);
        sb.append(SOUtils.getBPLocationIsTax(bizzySo.bpLocationName) ? "T" : "N");
        this.docType = SOUtils.docTypeMap.get(sb.toString());

        String principal = bizzySo.orderLines[0].principalId;
        this.orgTrx = SOUtils.getOrgTrx(this.bpHoldingId, principal);

        // org.compiere.model.PO::saveNew()
        PO po = getMOrderPO(SOUtils.orgIdMap.get(this.org), SOUtils.orgTrxIdMap.get(this.orgTrx), bizzySo.dateOrdered);
        this.documentNo = DB.getDocumentNo(SOUtils.docTypeIdMap.get(this.docType), null, false, po);

        orderLines = new SASSalesOrderLine[bizzySo.orderLines.length];
        for (int i = 0; i < orderLines.length; i++) {
            orderLines[i] = new SASSalesOrderLine(bizzySo.orderLines[i], this);
        }
    }

    protected int getNextLineNumber() {
        latestLineNumber += 10;
        return latestLineNumber;
    }

    private PO getMOrderPO(int orgId, int orgTrxId, Date dateOrdered) {
        /* org.compiere.model.GridTable.dataSavePO(int) */
        int Record_ID = 0; // new PO
        String trxName = null; 

        /* org.compiere.model.MTable::getPO(int, String) */
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

        /* prefix: @AD_Org_ID<Value>@@AD_OrgTrx_ID<Name>@-OPT-@DateOrdered<yyMM>@- */
        po.set_ValueNoCheck("AD_Org_ID", orgId);
        po.set_ValueNoCheck("AD_OrgTrx_ID", orgTrxId);
        po.set_ValueNoCheck("DateOrdered", new Timestamp(dateOrdered.getTime()));

        return po;
    }
}
