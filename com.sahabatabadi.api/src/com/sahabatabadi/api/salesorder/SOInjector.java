package com.sahabatabadi.api.salesorder;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;

import com.sahabatabadi.api.SASApiHeader;
import com.sahabatabadi.api.SASApiInjectable;

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
            boolean injectSuccess = injectSalesOrder(sasSo);

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

    // TODO have to ensure KEY is not null
    private boolean injectSalesOrder(SASApiHeader document) {
        // org.adempiere.webui.panel.action.FileImportAction::importFile()
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

        injectDocument(headerTab, childs, document);

        if (log.isLoggable(Level.INFO) && (document instanceof SASSalesOrder)) // TODO redesign to remove type 
            log.info(PLUGIN_PREFIX + "The document number is: " + ((SASSalesOrder) document).documentNo);

        return true;
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

    // TODO check CSV output log from rowsTmpResult, retrace base code

    private boolean isError = false;

    private Trx trx;
    private String trxName;
    private PO masterRecord;

    private static final String ORDER_LINE_TABLE_NAME = "C_OrderLine";

    private void injectDocument(GridTab headerTab, List<GridTab> childs, SASApiHeader headerObj) {
        try {
            isError = false;
            trx = null;
            trxName = null;
            masterRecord = null;

            createTrx(headerTab);

            boolean headerRecordProcessed = processRecord(headerTab, false, childs, headerObj);
            if (headerRecordProcessed) {
                // process detail
                GridTab orderLineTab = null;
                for (GridTab child : childs) {
                    if (ORDER_LINE_TABLE_NAME.equals(child.getTableName())) { // TODO find a way to refactor, make extendable
                        orderLineTab = child;
                        break;
                    }
                }

                for (SASApiInjectable orderLine : headerObj.getLines()) {
                    processRecord(orderLineTab, true, childs, orderLine);
                }
            }
            
            closeTrx(headerTab);
        } finally {
            headerTab.getTableModel().setImportingMode(false, null);
            for (GridTab child : childs) {
                child.getTableModel().setImportingMode(false, null);
            }
            headerTab.dataRefreshAll();
        }
    }

    // gridTab is either the header tab or the child tab
    private boolean processRecord(GridTab gridTab, boolean isDetail, List<GridTab> childs, SASApiInjectable so) {
        try {
            if (isDetail) {
                gridTab.getTableModel().setImportingMode(true, trxName);
            }

            if (!gridTab.getTableModel().isOpen()) {
                gridTab.getTableModel().open(0);
            }

            boolean dataNewSuccess = gridTab.dataNew(false);
            if (!dataNewSuccess) {
                String errMsg = "[" + gridTab.getName() + "] - Was not able to create a new record!";
            	throw new AdempiereException(errMsg);
            }
            
            gridTab.navigateCurrent();
            if (!isDetail) {
                for (GridTab child : childs) {
                    child.query(false);
                }
            }

            boolean isRowProcessed = processRow(gridTab, masterRecord, trx, so); 
            if (!isRowProcessed) {
                throw new AdempiereException();
            }

            boolean dataSaveSuccess = gridTab.dataSave(false);
            if (!dataSaveSuccess) {
                ValueNamePair ppE = CLogger.retrieveWarning();
                if (ppE == null) {
                    ppE = CLogger.retrieveError();
                }

                String info = (ppE != null) ? info = ppE.getName() : "";

                String errMsg = String.format("%s %s (%s)", Msg.getMsg(Env.getCtx(), "Error"),
                        Msg.getMsg(Env.getCtx(), "SaveError"), info);
                throw new AdempiereException(errMsg);
            }

            PO po = gridTab.getTableModel().getPO(gridTab.getCurrentRow());

            if (!isDetail) {
                masterRecord = po;
            }

            
            if (log.isLoggable(Level.INFO)) 
                log.info(PLUGIN_PREFIX + Msg.getMsg(Env.getCtx(), "Inserted") + " " + po.toString());
            return true;
        } catch (AdempiereException e) {
            gridTab.dataIgnore();
            isError = true;

            if (e.getMessage() != null) {
                insertErrorLog(e.getMessage());
            }
            return false;
        }
    }

    private boolean processRow(GridTab gridTab, PO masterRecord, Trx trx, SASApiInjectable so) {
        // One field is guaranteed to be parent when insering child tab
        // when putting header, masterRecord is null
        List<String> parentColumns = new ArrayList<String>();
        try {
            for (Field soField : so.getClass().getDeclaredFields()) {
                Object value = null;
                try {
                    value = soField.get(so);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    String errMsg = "Java Reflection error when accessing " + soField.getName() + "field in " + so.getClass();
                    throw new AdempiereException(errMsg);
                }

                if(value == null) {
                    continue;
                }

                String columnName = so.getColumnName(soField.getName()); 
                if (columnName == null) {
                    continue; // non-SO fields, e.g. constants, logger, etc.
                }

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
                    if (isForeign && masterRecord != null) {
                        if (masterRecord.get_Value(foreignColumn).toString().equals(value)) {
                            String errMsg = gridTab.setValue(field, masterRecord.get_ID()); // LOGMSG
                            if (!errMsg.equals("")) {
                                throw new AdempiereException(errMsg);
                            }
                        } else if (value != null) {
                            String errMsg = soField.getName() + " - " + Msg.getMsg(Env.getCtx(), "DiffParentValue",
                                    new Object[] { masterRecord.get_Value(foreignColumn).toString(), value }); // LOGMSG
                            throw new AdempiereException(errMsg);
                        }
                    } else if (isForeign && masterRecord == null && gridTab.getTabLevel() > 0) {
                        Object master = gridTab.getParentTab().getValue(foreignColumn);
                        if (master != null && value != null && !master.toString().equals(value)) {
                            String errMsg = soField.getName() + " - " + Msg.getMsg(Env.getCtx(), "DiffParentValue",
                                    new Object[] { master.toString(), value }); // LOGMSG
                            throw new AdempiereException(errMsg);
                        }
                    } else if (masterRecord == null && isDetail) {
                        MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
                        String foreignTable = column.getReferenceTableName();
                        String idS = null;
                        int id = -1;

                        String logMsg = null;
                        if ("AD_Ref_List".equals(foreignTable)) {
                            idS = resolveForeignList(column, foreignColumn, value, trx);
                            if (idS == null) {
                                String errMsg = Msg.getMsg(Env.getCtx(), "ForeignNotResolved", new Object[] { columnName, value });
                                throw new AdempiereException(errMsg);
                            }

                            logMsg = gridTab.setValue(field, idS); // LOGMSG
                        } else {
                            id = resolveForeign(foreignTable, foreignColumn, value, trx);
                            if (id < 0) {
                                String errMsg = Msg.getMsg(Env.getCtx(), "ForeignNotResolved", new Object[] { columnName, value });
                                throw new AdempiereException(errMsg);
                            }

                            logMsg = gridTab.setValue(field, id); // LOGMSG
                        }

                        if (logMsg == null || !logMsg.equals("")) {
                            throw new AdempiereException();
                        }
                    }

                    parentColumns.add(columnName);
                    continue;
                }

                if (!field.isDisplayed(true)) {
                    continue;
                }

                if (isForeign) {
                    MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
                    String foreignTable = column.getReferenceTableName();

                    if ("AD_Ref_List".equals(foreignTable)) {
                        String idS = resolveForeignList(column, foreignColumn, value,trx);
                        if(idS == null)	{
                            String errMsg = Msg.getMsg(Env.getCtx(),"ForeignNotResolved",new Object[] {soField.getName(), value}); // LOGMSG
                            throw new AdempiereException(errMsg);
                        }
                        
                        setValue = idS;
                    } else {
                        int foreignID = resolveForeign(foreignTable, foreignColumn, value, trx); 

                        if (foreignID < 0) {
                            String errMsg = Msg.getMsg(Env.getCtx(), "ForeignNotResolved", new Object[] {soField.getName(), value}); // LOGMSG
                            throw new AdempiereException(errMsg);
                        }

                        setValue = foreignID;

                        if (field.isParentValue()) {
                            int actualID = (Integer) field.getValue();
                            if (actualID != foreignID) {
                                String errMsg = Msg.getMsg(Env.getCtx(), "ParentCannotChange", new Object[] {soField.getName()}); // LOGMSG
                                throw new AdempiereException(errMsg);
                            }
                        }
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
                        String errMsg = gridTab.setValue(field, setValue); // LOGMSG

                        if (!errMsg.equals("")) {
                            throw new AdempiereException(errMsg);
                        }
                    }
                }
            }
        } catch (AdempiereException e) {
            if (e.getMessage() != null) {
                insertErrorLog(e.getMessage());
            }

            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
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

    private void closeTrx(GridTab gridTab) {
        if (trx == null) {
            return;
        }

        if (isError) {
            gridTab.dataDelete();
            trx.rollback();
            isError = false;
        } else {
            try {
                trx.commit(true);
            } catch (SQLException e) {
                isError = true;
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
    } 

    private String getColumnName(boolean isKey, boolean isForeign, boolean isDetail, String headName) {
        if (isKey) {
            if (headName.indexOf("/") > 0) {
                if (headName.endsWith("K")) {
                    headName = headName.substring(0, headName.length() - 2);
                } else {
                    insertErrorLog(Msg.getMsg(Env.getCtx(), "ColumnKey") + " " + headName); // missing Key
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

    private String resolveForeignList(MColumn column, String foreignColumn, Object value, Trx trx) {
        String idS = null;
        String trxName = (trx != null ? trx.getTrxName() : null);
        StringBuilder select = new StringBuilder("SELECT Value FROM AD_Ref_List WHERE ").append(foreignColumn)
                .append("=? AND AD_Reference_ID=? AND IsActive='Y'");
        idS = DB.getSQLValueStringEx(trxName, select.toString(), value, column.getAD_Reference_Value_ID());
        return idS;
    }

    private int resolveForeign(String foreignTable, String foreignColumn, Object value, Trx trx) {
        int id = -1;
        String trxName = (trx != null ? trx.getTrxName() : null);
        StringBuilder select = new StringBuilder("SELECT ").append(foreignTable)
                .append("_ID FROM ").append(foreignTable)
                .append(" WHERE ").append(foreignColumn)
                .append("=? AND IsActive='Y' AND AD_Client_ID=?");
        id = DB.getSQLValueEx(trxName, select.toString(), value, Env.getAD_Client_ID(Env.getCtx()));
        if (id == -1 && !"AD_Client".equals(foreignTable)) {
            MTable ft = MTable.get(Env.getCtx(), foreignTable);
            String accessLevel = ft.getAccessLevel();
            if (MTable.ACCESSLEVEL_All.equals(accessLevel) || MTable.ACCESSLEVEL_SystemOnly.equals(accessLevel)
                    || MTable.ACCESSLEVEL_SystemPlusClient.equals(accessLevel)) {
                // try System client if the table has System access
                id = DB.getSQLValueEx(trxName, select.toString(), value, 0);
            }
        }
        return id;
    }

    private void insertErrorLog(String errorLog) {
        System.err.println(PLUGIN_PREFIX + errorLog); // TODO insert error line to DB
    }
}
