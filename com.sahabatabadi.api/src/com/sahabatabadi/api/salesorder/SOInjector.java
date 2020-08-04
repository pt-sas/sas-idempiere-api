package com.sahabatabadi.api.salesorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.base.IGridTabImporter;
import org.adempiere.impexp.GridTabCSVImporter;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MLookup;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

public class SOInjector {
	public static final String IMPORT_MODE_MERGE = "M";
    public static final String IMPORT_MODE_UPDATE = "U";
    public static final String IMPORT_MODE_INSERT = "I";

    public static final int SALES_ORDER_MENU_ID = 129;
    public static final int SALES_ORDER_WINDOW_ID = 143;

    public static final String PLUGIN_PREFIX = "[SAS iDempiere API] ";
    public static final String TEMP_CSV_FILEPATH = "/tmp/sas_generated_so.csv";

    protected static CLogger log = CLogger.getCLogger(SOInjector.class);

    private static int lastReturnedWindowNo = 1000;
    
    public static String apiName(BizzySalesOrder bizzySo) {
        emulateLogin();

        retrievePrincipalDiscountFromDb(bizzySo);

        ArrayList<String> insertedDocNums = new ArrayList<>();

        ArrayList<BizzySalesOrderLine[]> groupedSoLines = splitSoLines(bizzySo.orderLines);
        for (BizzySalesOrderLine[] soLineGroup : groupedSoLines) {
            BizzySalesOrder splitBizzySo = new BizzySalesOrder(bizzySo);
            splitBizzySo.orderLines = soLineGroup;

            SASSalesOrder sasSo = new SASSalesOrder(splitBizzySo);
            createCsv(sasSo, TEMP_CSV_FILEPATH);

            // TODO introduce locks? What if two instances insert SO at the same time?
            boolean injectSuccess = injectSalesOrder(TEMP_CSV_FILEPATH, sasSo.documentNo);

            if (injectSuccess) {
                insertedDocNums.add(sasSo.documentNo);
            }
        }

        return insertedDocNums.toString();
    }

    private static void retrievePrincipalDiscountFromDb(BizzySalesOrder bizzySo) {
        for (BizzySalesOrderLine soLine : bizzySo.orderLines) {
            String principal = SOUtils.getProductPrincipal(soLine.productId);
            soLine.principalId = principal;
            soLine.discount = SOUtils.getProductDiscount(soLine.productId, bizzySo.bpHoldingNo, principal); // setting double as an int
        }
    }

    private static ArrayList<BizzySalesOrderLine[]> splitSoLines(BizzySalesOrderLine[] bizzySoLines) {
        // TODO beware comparison of Double, maybe better to get discountListId
        HashMap<String, HashMap<Double, ArrayList<BizzySalesOrderLine>>> principalGrouping = new HashMap<>();

        for (int i = 0; i < bizzySoLines.length; i++) {
            String principal = bizzySoLines[i].principalId;
            double discount = bizzySoLines[i].discount;

            if (!principalGrouping.containsKey(principal)) {
                principalGrouping.put(principal, new HashMap<Double, ArrayList<BizzySalesOrderLine>>());
            }

            HashMap<Double, ArrayList<BizzySalesOrderLine>> discountGrouping = principalGrouping.get(principal);

            if (!discountGrouping.containsKey(discount)) {
                discountGrouping.put(discount, new ArrayList<BizzySalesOrderLine>());
            }

            ArrayList<BizzySalesOrderLine> groupedLines = discountGrouping.get(discount);

            groupedLines.add(bizzySoLines[i]);
        }

        ArrayList<BizzySalesOrderLine[]> toReturn = new ArrayList<>();

        for (Map.Entry<String, HashMap<Double, ArrayList<BizzySalesOrderLine>>> mapElement : principalGrouping.entrySet()) {
            HashMap<Double, ArrayList<BizzySalesOrderLine>> discountGrouping = mapElement.getValue();

            for (Map.Entry<Double, ArrayList<BizzySalesOrderLine>> innerMapElement : discountGrouping.entrySet()) {
                ArrayList<BizzySalesOrderLine> groupedLines = innerMapElement.getValue();
                toReturn.add(groupedLines.toArray(new BizzySalesOrderLine[groupedLines.size()]));
            }
        }
        
        return toReturn;
    }

    private static void emulateLogin() {
        // TODO emulate proper login
        Env.setContext(Env.getCtx(), "#AD_User_Name", "Fajar-170203");
        Env.setContext(Env.getCtx(), "#AD_User_ID", 2211127);
        Env.setContext(Env.getCtx(), "#SalesRep_ID", 2211127);
        Env.setContext(Env.getCtx(), "#AD_Role_ID", 1000110);
        Env.setContext(Env.getCtx(), "#AD_Role_Name", "Role SLS Admin");
        // Env.setContext(Env.getCtx(), "#AD_User_Name", "Api-01");
        // Env.setContext(Env.getCtx(), "#AD_User_ID", 2214213);
        // Env.setContext(Env.getCtx(), "#SalesRep_ID", 2214213);
        // Env.setContext(Env.getCtx(), "#AD_Role_ID", 1000002);
        // Env.setContext(Env.getCtx(), "#AD_Role_Name", "awn Admin");
        Env.setContext(Env.getCtx(), "#User_Level", " CO"); // Format 'SCO'
        Env.setContext(Env.getCtx(), Env.AD_ORG_ID, 1000001);
        Env.setContext(Env.getCtx(), Env.AD_ORG_NAME, "Sunter");
        Env.setContext(Env.getCtx(), Env.M_WAREHOUSE_ID, 1000000);
        Env.setContext(Env.getCtx(), "#Date", new Timestamp(System.currentTimeMillis()));
        Env.setContext(Env.getCtx(), "#ShowAcct", "N");
        Env.setContext(Env.getCtx(), "#ShowTrl", "Y");
        Env.setContext(Env.getCtx(), "#ShowAdvanced", "N");
        Env.setContext(Env.getCtx(), "#YYYY", "Y");
        Env.setContext(Env.getCtx(), "#StdPrecision", 2);
        Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", 1000001);
        Env.setContext(Env.getCtx(), "$C_Currency_ID", 303);
        Env.setContext(Env.getCtx(), "$HasAlias", "Y");
        Env.setContext(Env.getCtx(), "#C_Country_ID", 100);
        Env.setContext(Env.getCtx(), Env.LANGUAGE, "en_US");
        Env.setContext(Env.getCtx(), "#AD_Client_ID", 1000001);
        Env.setContext(Env.getCtx(), "#AD_Client_Name", "sas");
    }

    private static void createCsv(SASSalesOrder sasSo, String filepath) {
        // TODO add Invoice partner & BP contacts headers
        final String[] header = new String[] {
            "AD_Org_ID[Name]", 
            "DocumentNo/K", 
            "Description", 
            "C_DocTypeTarget_ID[Name]", 
            "DateOrdered", 
            "DatePromised", 
            "C_BPartner_ID[Value]", 
            "C_BPartner_Location_ID[Name]", 
            "M_Warehouse_ID[Value]", 
            "AD_OrgTrx_ID[Name]", 
            "C_OrderLine>Line", 
            "C_OrderLine>M_Product_ID[Value]", 
            "C_OrderLine>QtyEntered", 
            "C_OrderLine>C_Order_ID[DocumentNo]/K", 
            "C_OrderLine>DatePromised"
        };

        final CellProcessor[] processors = new CellProcessor[] { 
            new Optional(), // AD_Org_ID[Name]
            new Optional(), // DocumentNo/K
            new Optional(), // Description
            new Optional(), // C_DocTypeTarget_ID[Name]
            new Optional(), // DateOrdered
            new Optional(), // DatePromised
            new Optional(), // C_BPartner_ID[Value]
            new Optional(), // C_BPartner_Location_ID[Name]
            new Optional(), // M_Warehouse_ID[Value]
            new Optional(), // AD_OrgTrx_ID[Name]
            new Optional(), // C_OrderLine>Line
            new Optional(), // C_OrderLine>M_Product_ID[Value]
            new Optional(), // C_OrderLine>QtyEntered
            new Optional(), // C_OrderLine>C_Order_ID[DocumentNo]/K
            new Optional()  // C_OrderLine>DatePromised
        };

        List<HashMap<String, Object>> mapArr = new ArrayList<>();

        // create the customer Maps (using the header elements for the column keys)
        final HashMap<String, Object> headerMap = new HashMap<String, Object>();
        headerMap.put(header[0], sasSo.org);
        headerMap.put(header[1], sasSo.documentNo);
        headerMap.put(header[2], sasSo.description);
        headerMap.put(header[3], sasSo.docType);
        headerMap.put(header[4], sasSo.dateOrdered);
        headerMap.put(header[5], sasSo.datePromised);
        headerMap.put(header[6], sasSo.bpHoldingId);
        headerMap.put(header[7], sasSo.bpLocation);
        headerMap.put(header[8], sasSo.warehouse);
        headerMap.put(header[9], sasSo.orgTrx);
        mapArr.add(headerMap);

        for (SASSalesOrderLine line : sasSo.orderLines) {
            final HashMap<String, Object> lineMap = new HashMap<String, Object>();
            lineMap.put(header[10], line.lineNo);
            lineMap.put(header[11], line.productId);
            lineMap.put(header[12], line.quantity);
            lineMap.put(header[13], line.documentNo);
            lineMap.put(header[14], line.datePromised);
            mapArr.add(lineMap);
        }

        ICsvMapWriter mapWriter = null;
        try {
            mapWriter = new CsvMapWriter(new FileWriter(filepath), CsvPreference.STANDARD_PREFERENCE);

            // write the header
            mapWriter.writeHeader(header);

            // write the customer maps
            for (HashMap<String, Object> map : mapArr) {
                mapWriter.write(map, header, processors);
            }

            mapWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getNextWindowNo() {
        SOInjector.lastReturnedWindowNo += 1;
        return SOInjector.lastReturnedWindowNo;
    }

    private static boolean injectSalesOrder(String csvInputFilePath, String documentNo) {
        // org.adempiere.webui.panel.action.FileImportAction::importFile()
        IGridTabImporter importer = new GridTabCSVImporter();

        Charset charset = Charset.forName("UTF-8");

        String iMode = IMPORT_MODE_INSERT;

        final int windowNo = getNextWindowNo();

        // org.adempiere.webui.apps.AEnv::getMWindowVO(int, int, int)
        GridWindowVO gWindowVO = GridWindowVO.create(Env.getCtx(), windowNo, SALES_ORDER_WINDOW_ID, 0);

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initComponents()
        GridWindow gridWindow = new GridWindow(gWindowVO, true);

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initPanel(MQuery query)
        Env.setContext(Env.getCtx(), windowNo, "IsSOTrx", gridWindow.isSOTrx());

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initTab(MQuery, int)
        gridWindow.initTab(0);
        GridTab headerTab = gridWindow.getTab(0);
        new GridTabHolder(headerTab);

        // org.adempiere.webui.panel.action.FileImportAction::importFile()
        Set<String> tables = new HashSet<String>();
        List<GridTab> childs = new ArrayList<GridTab>();
        for (int i = 1; i < gridWindow.getTabCount(); i++) {
            // org.adempiere.webui.adwindow.AbstractADWindowContent::initTab(MQuery, int)
            gridWindow.initTab(i);
            GridTab gTab = gridWindow.getTab(i);
            new GridTabHolder(gTab);

            String tableName = gTab.getTableName();
            if (tables.contains(tableName))
                continue;

            tables.add(tableName);
            childs.add(gTab);
        }

        try {
            InputStream m_file_istream = new FileInputStream(csvInputFilePath);

            File outFile = importer.fileImport(headerTab, childs, m_file_istream, charset, iMode);
            // TODO refactor the importer

            if (log.isLoggable(Level.INFO)) 
                log.info(PLUGIN_PREFIX + "The document number is: " + documentNo);

            // TODO if it's error, return false;
            return true;
        } catch (FileNotFoundException e) {
            log.severe(PLUGIN_PREFIX + "File not found!");
            return false;
        }
    }

    static class GridTabHolder implements DataStatusListener {
        private GridTab gridTab;

        public GridTabHolder(GridTab gTab) {
            this.gridTab = gTab;
            gridTab.addDataStatusListener(this);
        }

        public void dataStatusChanged(DataStatusEvent e) {
            // org.adempiere.webui.adwindow.ADTabpanel::dataStatusChanged(DataStatusEvent)
            int col = e.getChangedColumn();
            // Process Callout
            GridField mField = gridTab.getField(col);
            if (mField != null && (mField.getCallout().length() > 0
                    || (Core.findCallout(gridTab.getTableName(), mField.getColumnName())).size() > 0
                    || gridTab.hasDependants(mField.getColumnName()))) {
                String msg = gridTab.processFieldChange(mField); // Dependencies & Callout
                if (msg.length() > 0) {
                    if (log.isLoggable(Level.WARNING))
                        log.warning(PLUGIN_PREFIX + "Data status error: " + msg);
                }

                // Refresh the list on dependant fields
                for (GridField dependentField : gridTab.getDependantFields(mField.getColumnName())) {
                    // if the field has a lookup
                    if (dependentField != null && dependentField.getLookup() instanceof MLookup) {
                        MLookup mLookup = (MLookup) dependentField.getLookup();
                        // if the lookup is dynamic (i.e. contains this columnName as variable)
                        if (mLookup.getValidation().indexOf("@" + mField.getColumnName() + "@") != -1) {
                            mLookup.refresh();
                        }
                    }
                } // for all dependent fields
            }
        }
    }
}
