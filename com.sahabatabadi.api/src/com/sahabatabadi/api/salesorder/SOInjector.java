package com.sahabatabadi.api.salesorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

public class SOInjector {
	public static final String IMPORT_MODE_MERGE = "M";
    public static final String IMPORT_MODE_UPDATE = "U";
    public static final String IMPORT_MODE_INSERT = "I";

    public static final int SALES_ORDER_MENU_ID = 129;
    public static final int SALES_ORDER_WINDOW_ID = 143;

    public static final String PLUGIN_PREFIX = "[SAS iDempiere API] ";
    public static final String TEMP_CSV_FILEPATH = "/tmp/sas_generated_so_";

    protected static CLogger log = CLogger.getCLogger(SOInjector.class);

    private static int lastReturnedWindowNo = 1000;
    
    public String apiName(BizzySalesOrder bizzySo) {
        for (BizzySalesOrderLine soLine : bizzySo.orderLines) {
            String principal = SOUtils.getProductPrincipal(soLine.productId);
            soLine.principalId = principal;
            soLine.discount = SOUtils.getProductDiscount(soLine.productId, bizzySo.bpHoldingNo, principal); // setting double as an int
        }

        ArrayList<String> insertedDocNums = new ArrayList<>();

        ArrayList<BizzySalesOrderLine[]> groupedSoLines = splitSoLines(bizzySo.orderLines);
        for (BizzySalesOrderLine[] soLineGroup : groupedSoLines) {
            String currentCsvFilepath = TEMP_CSV_FILEPATH + System.currentTimeMillis() + ".csv";

            BizzySalesOrder splitBizzySo = new BizzySalesOrder(bizzySo);
            splitBizzySo.orderLines = soLineGroup;

            SASSalesOrder sasSo = new SASSalesOrder(splitBizzySo);
            createCsv(sasSo, currentCsvFilepath);

            boolean injectSuccess = injectSalesOrder(currentCsvFilepath, sasSo.documentNo);

            if (injectSuccess) {
                insertedDocNums.add(sasSo.documentNo);
            }
        }

        return insertedDocNums.toString();
    }

    private ArrayList<BizzySalesOrderLine[]> splitSoLines(BizzySalesOrderLine[] bizzySoLines) {
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

    private static void createCsv(SASSalesOrder sasSo, String filepath) {
        final String[] header = new String[] {
            "AD_Org_ID[Name]", 
            "DocumentNo/K", 
            "Description", 
            "C_DocTypeTarget_ID[Name]", 
            "DateOrdered", 
            "DatePromised", 
            "C_BPartner_ID[Value]", 
            "Bill_BPartner_ID[Value]",
            "C_BPartner_Location_ID[Name]", 
            "Bill_Location_ID[Name]",
            "M_Warehouse_ID[Value]", 
            "AD_OrgTrx_ID[Name]", 
            "C_OrderLine>Line", 
            "C_OrderLine>M_Product_ID[Value]", 
            "C_OrderLine>QtyEntered", 
            "C_OrderLine>C_Order_ID[DocumentNo]/K", 
            "C_OrderLine>DatePromised"
        };

        final CellProcessor[] processors = new CellProcessor[] { 
            new Optional(), // 0: AD_Org_ID[Name]
            new Optional(), // 1: DocumentNo/K
            new Optional(), // 2: Description
            new Optional(), // 3: C_DocTypeTarget_ID[Name]
            new Optional(), // 4: DateOrdered
            new Optional(), // 5: DatePromised
            new Optional(), // 6: C_BPartner_ID[Value]
            new Optional(), // 7: Bill_BPartner_ID[Value]
            new Optional(), // 8: C_BPartner_Location_ID[Name]
            new Optional(), // 9: Bill_Location_ID[Name]
            new Optional(), // 10: M_Warehouse_ID[Value]
            new Optional(), // 11: AD_OrgTrx_ID[Name]
            new Optional(), // 12: C_OrderLine>Line
            new Optional(), // 13: C_OrderLine>M_Product_ID[Value]
            new Optional(), // 14: C_OrderLine>QtyEntered
            new Optional(), // 15: C_OrderLine>C_Order_ID[DocumentNo]/K
            new Optional()  // 16: C_OrderLine>DatePromised
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
        headerMap.put(header[7], sasSo.invoiceBpHoldingId);
        headerMap.put(header[8], sasSo.bpLocation);
        headerMap.put(header[9], sasSo.invoiceBpLocation);
        headerMap.put(header[10], sasSo.warehouse);
        headerMap.put(header[11], sasSo.orgTrx);
        mapArr.add(headerMap);

        for (SASSalesOrderLine line : sasSo.orderLines) {
            final HashMap<String, Object> lineMap = new HashMap<String, Object>();
            lineMap.put(header[12], line.lineNo);
            lineMap.put(header[13], line.productId);
            lineMap.put(header[14], line.quantity);
            lineMap.put(header[15], line.documentNo);
            lineMap.put(header[16], line.datePromised);
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

    private boolean injectSalesOrder(String csvInputFilePath, String documentNo) {
        // org.adempiere.webui.panel.action.FileImportAction::importFile()
        IGridTabImporter importer = new GridTabCSVImporter();

        Charset charset = Charset.forName("UTF-8");

        String iMode = IMPORT_MODE_INSERT;

        final int windowNo = getNextWindowNo();

        // org.adempiere.webui.apps.AEnv::getMWindowVO(int, int, int)
        GridWindowVO gWindowVO = GridWindowVO.create(Env.getCtx(), windowNo, SALES_ORDER_WINDOW_ID, SALES_ORDER_MENU_ID);

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

            if (outFile.getName().endsWith("err.csv")) {
                return false;
            }

            if (checkLogCsvHasError(outFile)) {
                return false;
            }

            if (log.isLoggable(Level.INFO)) 
                log.info(PLUGIN_PREFIX + "The document number is: " + documentNo);

            return true;
        } catch (FileNotFoundException e) {
            log.severe(PLUGIN_PREFIX + "File not found!");
            return false;
        }
    }

    private boolean checkLogCsvHasError(File outCsv) {
        ICsvMapReader mapReader = null;
        try {
            mapReader = new CsvMapReader(new FileReader(outCsv), CsvPreference.STANDARD_PREFERENCE);

            // the header columns are used as the keys to the Map
            final String[] header = mapReader.getHeader(true);
            final CellProcessor[] processors = new CellProcessor[header.length];
            for (int i = 0; i < processors.length; i++) {
                processors[i] = null;
            }

            boolean hasError = false;
            Map<String, Object> customerMap;
            while ((customerMap = mapReader.read(header, processors)) != null) {
                String rowLog = (String) customerMap.get("_LOG_");
                if (!rowLog.startsWith("Inserted", rowLog.indexOf('>') + 3)) {
                    if (log.isLoggable(Level.WARNING))
                        log.warning(PLUGIN_PREFIX + "Inject Error: " + rowLog);
                        
                    hasError = true;
                }
            }
            
            mapReader.close();

            return hasError;
        } catch (IOException e) {
			e.printStackTrace();
			return true;
		}
    }

    class GridTabHolder implements DataStatusListener {
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
