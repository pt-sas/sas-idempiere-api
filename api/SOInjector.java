import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.IGridTabImporter;
import org.adempiere.impexp.GridTabCSVImporter;
import org.adempiere.webui.AdempiereIdGenerator;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.IADTabbox;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.component.DesktopTabpanel;
import org.compiere.model.GridTab;
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

    public static final String PLUGIN_PREFIX = "[SAS SO Injector] ";

    protected static CLogger log = CLogger.getCLogger(SOInjector.class);

    public static boolean dummyTrigger() {
        BizzySalesOrder bizzySo = new BizzySalesOrder();

        bizzySo.soff_code = 'A';
        bizzySo.description = "OSGi Testing v1";
        bizzySo.dateOrdered = new Date(System.currentTimeMillis());
        bizzySo.bpHoldingNo = 3806;
        bizzySo.bpLocationName = "PIONEER ELECTRIC- Kenari Mas [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]";

        bizzySo.orderLines = new BizzySalesOrderLine[5];
        bizzySo.orderLines[0] = new BizzySalesOrderLine();
        bizzySo.orderLines[0].productId = "AB0301485";
        bizzySo.orderLines[0].quantity = 20;
        bizzySo.orderLines[1] = new BizzySalesOrderLine();
        bizzySo.orderLines[1].productId = "AB0301440";
        bizzySo.orderLines[1].quantity = 40;
        bizzySo.orderLines[2] = new BizzySalesOrderLine();
        bizzySo.orderLines[2].productId = "AB0301430";
        bizzySo.orderLines[2].quantity = 60;
        bizzySo.orderLines[3] = new BizzySalesOrderLine();
        bizzySo.orderLines[3].productId = "AB0301420";
        bizzySo.orderLines[3].quantity = 80;
        bizzySo.orderLines[4] = new BizzySalesOrderLine();
        bizzySo.orderLines[4].productId = "AB0301635";
        bizzySo.orderLines[4].quantity = 100;

        return apiName(bizzySo);
    }

    public static boolean apiName(BizzySalesOrder bizzySo) {
        SASSalesOrder sasSo = new SASSalesOrder(bizzySo);

        String csvInputFilepath = "/tmp/sas_generated_so.csv";
        createCsv(sasSo, csvInputFilepath);
        return injectSalesOrder(csvInputFilepath);
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

    private static boolean injectSalesOrder(String csvInputFilePath) {
        final ADWindow adWindow = new ADWindow(Env.getCtx(), SALES_ORDER_WINDOW_ID);

        final DesktopTabpanel tabPanel = new DesktopTabpanel();
        String id = AdempiereIdGenerator.escapeId(adWindow.getTitle());
        tabPanel.setId(id + "_" + adWindow.getADWindowContent().getWindowNo());

        adWindow.createPart(tabPanel);

        return importFile(adWindow.getADWindowContent(), csvInputFilePath);
    }

    private static boolean importFile(AbstractADWindowContent panel, String csvInputFilePath) {
        IGridTabImporter importer = new GridTabCSVImporter();

        Charset charset = Charset.forName("UTF-8");

        String iMode = IMPORT_MODE_INSERT;

        IADTabbox adTab = panel.getADTab();
        int selected = adTab.getSelectedIndex(); // == 0
        int tabLevel = panel.getActiveGridTab().getTabLevel(); // == 0
        Set<String> tables = new HashSet<String>();
        List<GridTab> childs = new ArrayList<GridTab>();
        List<GridTab> includedList = panel.getActiveGridTab().getIncludedTabs(); // empty
        for (GridTab included : includedList) {
            String tableName = included.getTableName();
            if (tables.contains(tableName))
                continue;
            tables.add(tableName);
            childs.add(included);
        }
        for (int i = selected + 1; i < adTab.getTabCount(); i++) {
            IADTabpanel adTabPanel = adTab.getADTabpanel(i);
            if (adTabPanel.getGridTab().isSortTab())
                continue;
            if (adTabPanel.getGridTab().getTabLevel() <= tabLevel)
                break;
            String tableName = adTabPanel.getGridTab().getTableName();
            if (tables.contains(tableName))
                continue;
            tables.add(tableName);
            childs.add(adTabPanel.getGridTab());
        }

        try {
            InputStream m_file_istream = new FileInputStream(csvInputFilePath);

            File outFile = importer.fileImport(panel.getActiveGridTab(), childs, m_file_istream, charset, iMode);
            // TODO refactor the importer

            if (log.isLoggable(Level.INFO))
                log.info(PLUGIN_PREFIX + "The output log filepath is: " + outFile.getAbsolutePath());

            // TODO if it's error, return false;

            return true;
        } catch (FileNotFoundException e) {
            log.severe(PLUGIN_PREFIX + "File not found!");
            return false;
        }
    }
}