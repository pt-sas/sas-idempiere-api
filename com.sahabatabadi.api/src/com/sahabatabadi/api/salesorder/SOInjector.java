package com.sahabatabadi.api.salesorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import com.sahabatabadi.api.SASApiInjectable;

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
            BizzySalesOrder splitBizzySo = new BizzySalesOrder(bizzySo);
            splitBizzySo.orderLines = soLineGroup;

            SASSalesOrder sasSo = new SASSalesOrder(splitBizzySo);
            boolean injectSuccess = injectSalesOrder(sasSo, sasSo.documentNo);

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

    private static int getNextWindowNo() {
        SOInjector.lastReturnedWindowNo += 1;
        return SOInjector.lastReturnedWindowNo;
    }

    private boolean injectSalesOrder(BizzySalesOrder bizzySo, String documentNo) {
        // org.adempiere.webui.panel.action.FileImportAction::importFile()
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
            // TODO replace with refactored importer
            IGridTabImporter importer = new GridTabCSVImporter();
            File outFile = importer.fileImport(headerTab, childs, bizzySo);

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

    // TODO check CSV output log from rwsTmpResult, retrace base code

    private boolean isError = false;

    private Trx trx;
    private String trxName;
    private PO masterRecord;

    private static final String ORDER_LINE_TABLE_NAME = "C_OrderLine";

    private void importSo(GridTab headerTab, List<GridTab> childs, BizzySalesOrder bizzySo) {
        try {
            isError = false;
            trx = null;
            trxName = null;
            masterRecord = null;

            // process header 
            manageMasterTrx(headerTab);
            createTrx(headerTab);

            processRecord(headerTab, false, childs, bizzySo); // isDetail = false

            // process detail
            GridTab orderLineTab = null;
            for (GridTab child : childs) {
                if (ORDER_LINE_TABLE_NAME.equals(child.getTableName())) {
                    orderLineTab = child;
                    break;
                }
            }

            for (BizzySalesOrderLine orderLine : bizzySo.orderLines) {
                processRecord(orderLineTab, true, childs, orderLine);
            }

            manageMasterTrx(headerTab);
        } catch (IOException | Exception ex) {
            // throw new AdempiereException(ex); // TODO print error logs 
        } finally {
            headerTab.getTableModel().setImportingMode(false, null);
            for (GridTab detail : childs) {
                detail.getTableModel().setImportingMode(false, null);
            }
            headerTab.dataRefreshAll();
        }
    }

    // gridTab is either the header tab or the child tab
    private void processRecord(GridTab gridTab, boolean isDetail, List<GridTab> childs, SASApiInjectable so) {
        String logMsg = null;
        try {
            // Assign children to master trx
            if (isDetail) {
                gridTab.getTableModel().setImportingMode(true, trxName);
            }

            if (!gridTab.getTableModel().isOpen()) {
                gridTab.getTableModel().open(0);
            }

            boolean dataNewSuccess = gridTab.dataNew(false);
            if (dataNewSuccess) { // SASFLAG creating new record
                gridTab.navigateCurrent();

                if (!isDetail) {
                    for (GridTab child : childs) {
                        child.query(false);
                    }
                }

                logMsg = processRow(gridTab, masterRecord, trx, so);
            } else {
                logMsg = "[" + gridTab.getName() + "]" + "- Was not able to create a new record!";
            }

            if (logMsg != null) {
                isError = true;
            }

            if (!isError) {
                boolean dataSaveSuccess = gridTab.dataSave(false);
                if (dataSaveSuccess) {
                    PO po = gridTab.getTableModel().getPO(gridTab.getCurrentRow());

                    // Keep master record for details validation
                    if (!isDetail)
                        masterRecord = po;

                    logMsg = Msg.getMsg(Env.getCtx(), "Inserted") + " " + po.toString();
                } else {
                    ValueNamePair ppE = CLogger.retrieveWarning();
                    if (ppE == null)
                        ppE = CLogger.retrieveError();

                    String info = (ppE != null) ? info = ppE.getName() : "";
                    logMsg = Msg.getMsg(Env.getCtx(), "Error") + " " + Msg.getMsg(Env.getCtx(), "SaveError") + " (" + info + ")";

                    gridTab.dataIgnore();

                    // Problem in the master record
                    if (!isDetail && masterRecord == null) {
                        break;
                    }

                    // Problem in the detail record
                    if (isDetail && masterRecord != null) {
                        break;
                    }
                }

            } else {
                gridTab.dataIgnore();

                // Problem in the master record
                if (!isDetail && masterRecord == null) {
                    break;
                }

                // Problem in the detail record
                if (isDetail && masterRecord != null) {
                    break;
                }
            }
        } catch (Exception e) {
            gridTab.dataIgnore();

            isError = true;
        }
    }

    /*
     * private String proccessRow(GridTab gridTab, List<String> header, Map<String,
     * Object> map, int startindx, int endindx, PO masterRecord, Trx trx) {
     */
    // TODO put back code related to field.isParentValue().
    private String processRow(GridTab gridTab, PO masterRecord, Trx trx, SASApiInjectable so) {
        // One field is guaranteed to be parent when insering child tab
        // when putting header, masterRecord is null
        String logMsg = null;
        List<String> parentColumns = new ArrayList<String>();

        for (Field soField : so.getClass().getDeclaredFields()) { // TODO test reflection
            Object value = soField.get(so); 
            if(value == null) {
                continue;
            }

            String columnName = soField.getName();
            boolean isKeyColumn = columnName.indexOf("/") > 0;
            boolean isForeign = columnName.indexOf("[") > 0 && columnName.indexOf("]") > 0;
            boolean isDetail = columnName.indexOf(">") > 0;

            String foreignColumn = null;
            if (isForeign) {
                foreignColumn = columnName.substring(columnName.indexOf("[") + 1, columnName.indexOf("]"));
            }
            columnName = getColumnName(isKeyColumn, isForeign, isDetail, columnName);

            Object setValue = null;
            GridField field = gridTab.getField(columnName);
            if (field.isParentValue()) {
                if ("(null)".equals(value.toString())) {
                    logMsg = "Parent not specified";
                    break;
                }

                if (isForeign && masterRecord != null) {
                    if (masterRecord.get_Value(foreignColumn).toString().equals(value)) {
                        logMsg = gridTab.setValue(field, masterRecord.get_ID());

                        if (!logMsg.equals("")) {
                            break;
                        }
                    } else {
                        if (value != null) {
                            logMsg = columnName + " - " + "DiffParentValue";
                            break;
                        }
                    }
                } else if (isForeign && masterRecord == null && gridTab.getTabLevel() > 0) {
                    Object master = gridTab.getParentTab().getValue(foreignColumn);
                    if (master != null && value != null && !master.toString().equals(value)) {
                        logMsg = columnName + " - " + "DiffParentValue";
                        break;
                    }
                } else if (masterRecord == null && isDetail) {
                    MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
                    String foreignTable = column.getReferenceTableName();
                    String idS = null;
                    int id = -1;

                    if ("AD_Ref_List".equals(foreignTable)) {
                        idS = resolveForeignList(column, foreignColumn, value, trx);
                    } else {
                        id = resolveForeign(foreignTable, foreignColumn, value, trx);
                    }

                    if (idS == null && id < 0) {
                        return Msg.getMsg(Env.getCtx(), "ForeignNotResolved", new Object[] { header.get(i), value });
                    }

                    if (id >= 0) {
                        logMsg = gridTab.setValue(field, id);
                    } else if (idS != null) {
                        logMsg = gridTab.setValue(field, idS);
                    }

                    if (logMsg != null && logMsg.equals("")) {
                        logMsg = null;
                    } else {
                        break;
                    }
                }

                if (logMsg != null && logMsg.equals("")) {
                    logMsg = null;
                }

                parentColumns.add(columnName);
                continue;
            }

            if (!field.isDisplayed(true)) {
                continue;
            }

            if ("(null)".equals(value.toString().trim())) {
                logMsg = gridTab.setValue(field,null);	
                if(logMsg.equals("")) {
                    logMsg= null;
                } else {
                    break;
                }
            } else if (isForeign) {
                MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
                String foreignTable = column.getReferenceTableName();

                if ("AD_Ref_List".equals(foreignTable)) {
                    String idS = resolveForeignList(column, foreignColumn, value,trx);
                    if(idS == null)	{
                        return Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[]{header.get(i),value});
                    }
                    
                    setValue = idS;
                } else {
                    int foreignID = resolveForeign(foreignTable, foreignColumn, value, trx); 

                    if (foreignID < 0) {
                        return Msg.getMsg(Env.getCtx(), "ForeignNotResolved", new Object[] { header.get(i), value });
                    }

                    setValue = foreignID;
                }
            } else {
                if (value != null) {
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    }

                    // TODO ensure field.getDisplayType() != DisplayType.Payment, != DisplayType.Button
                    setValue = value;
                }
            }

            if (setValue != null) {
                Object oldValue = gridTab.getValue(field);
                if (isValueChanged(oldValue, setValue)) {
                    logMsg = gridTab.setValue(field, setValue);
                } else {
                    logMsg = "";
                }
            }

            if (logMsg != null && logMsg.equals("")) {
                logMsg = null;
            } else {
                break; // if log message contains error
            }
        }

        boolean checkParentKey = parentColumns.size() != gridTab.getParentColumnNames().size();
        if (isThereRow && logMsg == null && masterRecord != null && checkParentKey) {
            for (String linkColumn : gridTab.getParentColumnNames()) {
                String columnName = linkColumn;
                Object setValue = masterRecord.get_Value(linkColumn);
                // resolve missing key
                if (setValue == null) {
                    columnName = null;
                    for (int j = startindx; j < endindx + 1; j++) {
                        if (header.get(j).contains(linkColumn)) {
                            columnName = header.get(j);
                            setValue = map.get(columnName);
                            break;
                        }
                    }
                    if (columnName != null) {
                        String foreignColumn = null;
                        boolean isForeing = columnName.indexOf("[") > 0 && columnName.indexOf("]") > 0;
                        if (isForeing)
                            foreignColumn = columnName.substring(columnName.indexOf("[") + 1, columnName.indexOf("]"));

                        columnName = getColumnName(false, isForeing, true, columnName);
                        MColumn column = MColumn.get(Env.getCtx(), gridTab.getField(columnName).getAD_Column_ID());
                        if (isForeing) {
                            String foreignTable = column.getReferenceTableName();
                            if ("AD_Ref_List".equals(foreignTable)) {
                                String idS = resolveForeignList(column, foreignColumn, setValue, trx);
                                if (idS == null)
                                    return Msg.getMsg(Env.getCtx(), "ForeignNotResolved",
                                            new Object[] { columnName, setValue });

                                setValue = idS;
                            } else {
                                int id = resolveForeign(foreignTable, foreignColumn, setValue, trx);
                                if (id < 0)
                                    return Msg.getMsg(Env.getCtx(), "ForeignNotResolved",
                                            new Object[] { columnName, setValue });

                                setValue = id;
                            }
                        }
                    } else {
                        logMsg = "Key: " + linkColumn + " " + Msg.getMsg(Env.getCtx(), "NotFound");
                        break;
                    }
                }
                logMsg = gridTab.setValue(linkColumn, setValue);
                if (logMsg.equals(""))
                    logMsg = null;
                else
                    continue;
            }
        }

        return logMsg;
    }

    private boolean isValueChanged(Object oldValue, Object value) {
        if (isNotNullAndIsEmpty(oldValue)) {
            oldValue = null;
        }

        if (isNotNullAndIsEmpty(value)) {
            value = null;
        }

        boolean bChanged = (oldValue == null && value != null) || (oldValue != null && value == null);

        if (!bChanged && oldValue != null) {
            if (oldValue.getClass().equals(value.getClass())) {
                if (oldValue instanceof Comparable<?>) {
                    bChanged = (((Comparable<Object>) oldValue).compareTo(value) != 0);
                } else {
                    bChanged = !oldValue.equals(value);
                }
            } else if (value != null) {
                bChanged = !(oldValue.toString().equals(value.toString()));
            }
        }
        return bChanged;
    }

    private boolean isNotNullAndIsEmpty(Object value) {
        if (value != null && (value instanceof String) && value.toString().equals("")) {
            return true;
        } else {
            return false;
        }
    }

    private void manageMasterTrx(GridTab gridTab) {
        if (trx == null) {
            return;
        }

        if (isError) {
            gridTab.dataDelete();
            trx.rollback();
            setError(false);
        } else {
            try {
                trx.commit(true);
            } catch (SQLException e) {
                isError = true;
                rowsTmpResult.set(0, rowsTmpResult.get(0).replace(quoteChar + "\n", e.getLocalizedMessage() + quoteChar + "\n"));
                gridTab.dataDelete();
                trx.rollback();
            }
        }

        trx.close();
        trx = null;
    }

    private void createTrx(GridTab gridTab) {
        trxName = "Import_" + gridTab.getTableName() + "_" + UUID.randomUUID();
        gridTab.getTableModel().setImportingMode(true, trxName);
        trx = Trx.get(trxName, true);
        masterRecord = null;
        rowsTmpResult.clear();
    } 

    private String getColumnName(boolean isKey, boolean isForeign, boolean isDetail, String headName) {
        if (isKey) {
            if (headName.indexOf("/") > 0) {
                if (headName.endsWith("K")) {
                    headName = headName.substring(0, headName.length() - 2);
                } else {
                    throw new Exception("Missing key"); // TODO
                }
            }
        }

        if (isForeign) {
            headName = headName.substring(0, headName.indexOf("["));
        }

        if (isDetail) {
            headName = headName.substring(headName.indexOf(">") + 1, headName.length());
            if (headName.indexOf(">") > 0) {
                headName = headName.substring(headName.indexOf(">") + 1, headName.length());
            }
        }

        return headName;
    }
}
