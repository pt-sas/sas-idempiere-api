import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.adempiere.base.IModelFactory;
import org.adempiere.base.Service;
import org.compiere.model.PO;
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
    // public String invoiceBpHoldingId;   // Bill_BPartner_ID[Value]
    public String bpLocation;           // C_BPartner_Location_[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
    // public String invoiceBpLocation;    // Bill_Location_ID[Name]: MEGAH SARI- Rawasari [ Jl. Rawasari Selatan No. 10]
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
    public String orgTrx;               // AD_OrgTrx_ID[Name]: TR1 TODO urgent!!

    public SASSalesOrderLine[] orderLines;

    private HashMap<Character, String> orgMap = new HashMap<>();
    private HashMap<String, Integer> orgIdMap = new HashMap<>();
    private HashMap<Character, String> orgTrxMap = new HashMap<>();
    private HashMap<String, Integer> orgTrxIdMap = new HashMap<>();

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private int latestLineNumber = 0;

    public SASSalesOrder(BizzySalesOrder bizzySo) {
        // TODO may have to split this bizzy order into multiple SOs
        initializeMaps();

        this.org = orgMap.get(bizzySo.soff_code);
        this.description = bizzySo.description;
        this.dateOrdered = formatter.format(bizzySo.dateOrdered);
        this.bpHoldingId = prependZeros(bizzySo.bpHoldingNo, BP_ID_LENGTH);
        this.bpLocation = "PIONEER ELECTRIC- Kenari Mas [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]"; // TODO;

        this.docType = "OPN (Order Penjualan Non tax)"; // TODO;
        this.datePromised = this.dateOrdered;
        this.warehouse = "Sunter F1-2"; // TODO;
        this.orgTrx = "TR1"; // TODO;

        int docTypeId = 550265; // TODO docType TBD
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

    private PO getMOrderPO(int orgId, int orgTrxId, Date dateOrdered) {
        // TODO hardcoded parameters
        int Record_ID = 0;
        String trxName = null;
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

        // prefix: @AD_Org_ID<Value>@@AD_OrgTrx_ID<Name>@-OPT-@DateOrdered<yyMM>@-
        po.set_ValueNoCheck("AD_Org_ID", orgId);
        po.set_ValueNoCheck("AD_OrgTrx_ID", orgTrxId);
        po.set_ValueNoCheck("DateOrdered", dateOrdered);

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

        orgIdMap.put("Sunter", 1000001);
        orgIdMap.put("Tebet", 1000002);
        orgIdMap.put("Glodok", 1000003);
        orgIdMap.put("Kenari", 1000004);
        orgIdMap.put("Tangerang", 2200019);

        orgTrxMap.put('1', "TR1");
        orgTrxMap.put('2', "TR2");
        orgTrxMap.put('3', "TR3");
        orgTrxMap.put('4', "TR4");
        orgTrxMap.put('5', "TR5");
        orgTrxMap.put('R', "TGR");
        orgTrxMap.put('P', "PAN");
        orgTrxMap.put('L', "LEG");
        orgTrxMap.put('C', "SCH");
        orgTrxMap.put('U', "SUP");

        orgTrxIdMap.put("TR1", 1000006);
        orgTrxIdMap.put("TR2", 1000008);
        orgTrxIdMap.put("TR3", 2200020);
        orgTrxIdMap.put("TR4", 1000010);
        orgTrxIdMap.put("TR5", 2200021);
        orgTrxIdMap.put("TGR", 2200022);
        orgTrxIdMap.put("PAN", 1000011);
        orgTrxIdMap.put("LEG", 1000012);
        orgTrxIdMap.put("SCH", 1000023);
        orgTrxIdMap.put("SUP", 1000025);
    }
}

    /*
    private void poSetter(PO po, String columnName, Object value) {
        if (!po.set_ValueNoCheck(columnName, value)) {
            ValueNamePair lastError = CLogger.retrieveError();
            if (lastError != null) {
                String adMessage = lastError.getValue();
                String adMessageArgument = lastError.getName().trim();

                StringBuilder info = new StringBuilder(adMessageArgument);

                if (!adMessageArgument.endsWith(";"))
                    info.append(";");
                info.append(field.getHeader());

                fireDataStatusEEvent(adMessage, info.toString(), true);
            } else {
                fireDataStatusEEvent("Set value failed", field.getHeader(), true);
            }
            return SAVE_ERROR;
        }
    }
    */