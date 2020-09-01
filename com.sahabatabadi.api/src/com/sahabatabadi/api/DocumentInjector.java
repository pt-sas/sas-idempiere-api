package com.sahabatabadi.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.adempiere.base.Core;
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
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;

public class DocumentInjector {
    public static final String PLUGIN_PREFIX = "[SAS iDempiere API] ";

    private static int lastReturnedWindowNo = 1000;

    protected CLogger log = CLogger.getCLogger(getClass());

    private final int windowId;
    private final int menuId;

    private boolean errorHeaderCreated;

    private List<GridTab> childs;
    private GridTab headerTab;

    private boolean isError;
    private Trx trx;
    private String trxName;
    private PO masterRecord;

    public DocumentInjector(int windowId, int menuId) {
        this.windowId = windowId;
        this.menuId = menuId;
    }

    public boolean injectDocument(DocHeader headerObj) {
        errorHeaderCreated = false;
        isError = false;
        trx = null;
        trxName = null;
        masterRecord = null;

        if (!checkDocumentValid(headerObj)) {
            return false;
        }

        initGridTab();

        try {
            createTrx(headerTab);

            processRecord(headerTab, false, headerObj); // throws SASApiException upon failure

            // process detail
            String childTableName = headerObj.getLines()[0].getTableName();
            GridTab orderLineTab = null;
            for (GridTab child : childs) {
                if (childTableName.equals(child.getTableName())) {
                    orderLineTab = child;
                    break;
                }
            }

            for (ApiInjectable orderLine : headerObj.getLines()) {
            	try {
            		processRecord(orderLineTab, true, orderLine);
            	} catch (SASApiException e) {
            		insertErrorLog(e);
            	}
            }
        } catch (SASApiException e) {
            insertErrorLog(e);
            return false;
        } finally {
            closeTrx(headerTab);

            headerTab.getTableModel().setImportingMode(false, null);
            for (GridTab child : childs) {
                child.getTableModel().setImportingMode(false, null);
            }
            headerTab.dataRefreshAll();
        }

        return true;
    }

    private boolean checkDocumentValid(DocHeader headerObj) {
        if (headerObj.getDocumentNo() == null) {
            return false;
        }

        ApiInjectable[] lines = headerObj.getLines();
        if (lines.length < 1) {
            return false;
        }

        for (ApiInjectable line : lines) {
            if (line.getDocumentNo() == null) {
                return false;
            }
        }

        return true;
    }

    private void initGridTab() {
        // org.adempiere.webui.panel.action.FileImportAction::importFile()
        final int windowNo = getNextWindowNo();

        // org.adempiere.webui.apps.AEnv::getMWindowVO(int, int, int)
        GridWindowVO gWindowVO = GridWindowVO.create(Env.getCtx(), windowNo, this.windowId, this.menuId);

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initComponents()
        GridWindow gridWindow = new GridWindow(gWindowVO, true);

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initPanel(MQuery query)
        Env.setContext(Env.getCtx(), windowNo, "IsSOTrx", gridWindow.isSOTrx());

        // org.adempiere.webui.adwindow.AbstractADWindowContent::initTab(MQuery, int)
        gridWindow.initTab(0);
        this.headerTab = gridWindow.getTab(0);
        new GridTabHolder(headerTab);

        // org.adempiere.webui.panel.action.FileImportAction::importFile()
        Set<String> tables = new HashSet<String>();
        this.childs = new ArrayList<GridTab>();
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
    }

    private static int getNextWindowNo() {
        DocumentInjector.lastReturnedWindowNo += 1;
        return DocumentInjector.lastReturnedWindowNo;
    }

    // gridTab is either the header tab or the child tab
    private void processRecord(GridTab gridTab, boolean isDetail, ApiInjectable so) throws SASApiException {
        try {
            if (isDetail) {
                gridTab.getTableModel().setImportingMode(true, trxName);
            }

            if (!gridTab.getTableModel().isOpen()) {
                gridTab.getTableModel().open(0);
            }

            boolean dataNewSuccess = gridTab.dataNew(false);
            if (!dataNewSuccess) {
                throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                        String.format("Failed to create a new record in GridTab [%s]", gridTab.getName()));
            }

            gridTab.navigateCurrent();
            if (!isDetail) {
                for (GridTab child : childs) {
                    child.query(false);
                }
            }

            processRow(gridTab, trx, so); // throws SASApiException upon failure

            boolean dataSaveSuccess = gridTab.dataSave(false);
            if (!dataSaveSuccess) {
                ValueNamePair ppE = CLogger.retrieveWarning();
                if (ppE == null) {
                    ppE = CLogger.retrieveError();
                }

                String info = (ppE != null) ? info = ppE.getName() : "";

                throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                        String.format("Failed to save record in GridTab [%s]\n%s", gridTab.getName(), info));
            }

            PO po = gridTab.getTableModel().getPO(gridTab.getCurrentRow());

            if (!isDetail) {
                masterRecord = po;
            }

            if (log.isLoggable(Level.INFO))
                log.info(PLUGIN_PREFIX + "Inserted " + po.toString());
        } catch (SASApiException e) {
            gridTab.dataIgnore();
            isError = true;

            throw e;
        }
    }

    private void processRow(GridTab gridTab, Trx trx, ApiInjectable so) throws SASApiException {
        // One field is guaranteed to be parent when insering child tab
        // when putting header, masterRecord is null
        List<String> parentColumns = new ArrayList<String>();
        try {
            for (Field soField : so.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(soField.getModifiers())) {
                    continue; // non-public fields
                }

                Object value = null;
                try {
                    value = soField.get(so);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                            String.format("Java Reflection error when accessing field [%s] in class [%s]!\n%s",
                                    soField.getName(), so.getClass(), e.getMessage()));
                }

                if (value == null) {
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
                        Object masterKey = masterRecord.get_Value(foreignColumn);
                        if (masterKey != null && masterKey.toString().equals(value)) {
                            String errMsg = gridTab.setValue(field, masterRecord.get_ID());
                            if (!errMsg.equals("")) {
                                throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                        String.format("Error setting value [%s] in field [%s]. Error message: %s",
                                                masterRecord.get_ID(), field.getColumnName(), errMsg));
                            }
                        } else if (value != null) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                    String.format(
                                            "Header and detail have different key values! Header value: [%s], detail value: [%s]",
                                            masterRecord.get_Value(foreignColumn).toString(), value));
                        }
                    } else if (isForeign && masterRecord == null && gridTab.getTabLevel() > 0) {
                        Object master = gridTab.getParentTab().getValue(foreignColumn);
                        if (master != null && value != null && !master.toString().equals(value)) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                    String.format(
                                            "Header and detail have different key values! Header value: [%s], detail value: [%s]",
                                            master.toString(), value));
                        }
                    }

                    if (masterRecord == null && isDetail) {
                        MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
                        String foreignTable = column.getReferenceTableName();

                        String logMsg = null;
                        if ("AD_Ref_List".equals(foreignTable)) {
                            String idS = resolveForeignList(column, foreignColumn, value, trx);
                            if (idS == null) {
                                throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                        String.format(
                                                "Failed to resolve record ID for value [%s] in column [%s] of table [%s]",
                                                value, foreignColumn, foreignTable));
                            }

                            logMsg = gridTab.setValue(field, idS);
                        } else {
                            int id = resolveForeign(foreignTable, foreignColumn, value, trx);
                            if (id < 0) {
                                throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                        String.format(
                                                "Failed to resolve record ID for value [%s] in column [%s] of table [%s]",
                                                value, foreignColumn, foreignTable));
                            }

                            logMsg = gridTab.setValue(field, id);
                        }

                        if (logMsg == null || !logMsg.equals("")) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                    String.format("Error setting value [%s] in field [%s]. Error message: %s", setValue,
                                            field.getColumnName(), logMsg));
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
                        String idS = resolveForeignList(column, foreignColumn, value, trx);
                        if (idS == null) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                    String.format(
                                            "Failed to resolve record ID for value [%s] in column [%s] of table [%s]",
                                            value, foreignColumn, foreignTable));
                        }

                        setValue = idS;
                    } else {
                        int foreignID = resolveForeign(foreignTable, foreignColumn, value, trx);

                        if (foreignID < 0) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(),
                                    String.format(
                                            "Failed to resolve record ID for value [%s] in column [%s] of table [%s]",
                                            value, foreignColumn, foreignTable));
                        }

                        setValue = foreignID;
                    }
                } else {
                    if (value != null) {
                        if (value instanceof java.util.Date) {
                            value = new Timestamp(((java.util.Date) value).getTime());
                        }

                        setValue = value;
                    }
                }

                if (setValue != null) {
                    Object oldValue = gridTab.getValue(field);
                    if (isValueChanged(oldValue, setValue)) {
                        String errMsg = gridTab.setValue(field, setValue);

                        if (!errMsg.equals("")) {
                            throw new SASApiException(so, gridTab.getAD_Window_ID(), gridTab.getAD_Tab_ID(), errMsg);
                        }
                    }
                }
            }
        } catch (SASApiException e) {
            throw e;
        }
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
            if (headName.indexOf("/") > 0 && headName.endsWith("K")) {
                headName = headName.substring(0, headName.length() - 2);
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
        StringBuilder select = new StringBuilder("SELECT ").append(foreignTable).append("_ID FROM ")
                .append(foreignTable).append(" WHERE ").append(foreignColumn)
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

    private void insertErrorLog(SASApiException apiException) {
        ApiInjectable so = apiException.getBadDocument();
        int windowId = apiException.getWindowId();
        int tabId = apiException.getTabId();
        String errorLog = apiException.getMessage();

        if (log.isLoggable(Level.WARNING))
            log.warning(String.format(
                    "Failed to insert document %s in table %s. Error message: %s. Document content: %s",
                     so.getDocumentNo(), so.getTableName(), errorLog, so.toString()));

        try {
            if (so instanceof DocHeader) {
                insertErrorLogHelper(so, windowId, tabId, false, errorLog);

                DocHeader header = (DocHeader) so;
                ApiInjectable[] lines = header.getLines();
                GridTab childCache = null;
                for (ApiInjectable line : lines) {
                    if (childCache == null || !line.getTableName().equals(childCache)) {
                        for (GridTab child : childs) {
                            if (line.getTableName().equals(child.getTableName())) {
                                childCache = child;
                                break;
                            }
                        }
                    }

                    insertErrorLogHelper(line, childCache.getAD_Window_ID(), childCache.getAD_Tab_ID(), true,
                            "Caused by error in header record.");
                }
            } else if (so instanceof DocLine) {
                if (!errorHeaderCreated) {
                    DocLine line = (DocLine) so;
                    insertErrorLogHelper(line.getHeader(), headerTab.getAD_Window_ID(), headerTab.getAD_Tab_ID(), false,
                            "Caused by error in line/detail record.");
                    errorHeaderCreated = true;
                }

                insertErrorLogHelper(so, windowId, tabId, true, errorLog);
            } else {
                insertErrorLogHelper(so, windowId, tabId, false, errorLog);
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Exception when inserting into error log! " + e.getMessage() + " ");

            if (so != null) {
                sb.append("Offending document is " + so.toString() + ". ");
            }

            if (errorLog != null) {
                sb.append("Error log is " + errorLog + ". ");
            }

            if (log.isLoggable(Level.SEVERE))
                log.severe(sb.toString());
        }
    }

    private void insertErrorLogHelper(ApiInjectable so, int windowId, int tabId, boolean isDetail, String errorLog) throws Exception {
        String documentNo = so.getDocumentNo();
        String tableName = so.getTableName();

        // TODO replace with foreign resolve!
        int ERROR_LOG_WINDOW_ID = 2200001;
        int ERROR_LOG_MENU_ID = 2200138;

        final int windowNo = getNextWindowNo();
        GridWindowVO errorLogWindowVO = GridWindowVO.create(Env.getCtx(), windowNo, ERROR_LOG_WINDOW_ID,
                ERROR_LOG_MENU_ID);
        GridWindow errorLogWindow = new GridWindow(errorLogWindowVO, true);
        Env.setContext(Env.getCtx(), windowNo, "IsSOTrx", errorLogWindow.isSOTrx());

        GridTab errorTab = null;
        for (int i = 0; i < errorLogWindow.getTabCount(); i++) {
            errorLogWindow.initTab(i);
            GridTab gTab = errorLogWindow.getTab(i);
            
            boolean headerFound = !isDetail && "Header".equalsIgnoreCase(gTab.getName());
            boolean lineFound = isDetail && "Line".equalsIgnoreCase(gTab.getName());
            if (headerFound || lineFound) {
            	new GridTabHolder(gTab);
            	errorTab = gTab;
            	break;
            } 
        }

        if (errorTab == null) {
            throw new Exception("Could not find Error Log tab!");
        }

        if (!errorTab.getTableModel().isOpen()) {
            errorTab.getTableModel().open(0);
        }

        if (!errorTab.dataNew(false)) {
            throw new Exception("Failed to create new Error Log record");
        }

        errorTab.navigateCurrent();

        if (!"".equals(errorTab.setValue(errorTab.getField("Document_No"), documentNo))) {
            throw new Exception("Unable to set Document No in Error Log record");
        }
        
        if (!"".equals(errorTab.setValue(errorTab.getField("AD_Window_ID"), windowId))) {
            throw new Exception("Unable to set window ID in Error Log record");
        }
        
        if (!"".equals(errorTab.setValue(errorTab.getField("AD_Tab_ID"), tabId))) {
            throw new Exception("Unable to set tab ID in Error Log record");
        }

        if (!"".equals(errorTab.setValue(errorTab.getField("Error_Msg"), errorLog))) {
            throw new Exception("Unable to set error message in Error Log record");
        }

        if (!"".equals(errorTab.setValue(errorTab.getField("Raw_Content"), so.toString()))) {
            throw new Exception("Unable to set raw document content in Error Log record");
        }

        if (isDetail) {
            GridField errorHeaderIdField = errorTab.getField("SAS_API_ErrorLog_Header_ID");
            String errorHeaderTableName = MColumn.get(Env.getCtx(), errorHeaderIdField.getAD_Column_ID()).getReferenceTableName();
            int errorHeaderId = resolveForeign(errorHeaderTableName, "Document_No", documentNo, null);
            if (errorHeaderId < 0) {
                throw new Exception("Unable to resolve Table in Error Log record");
            }

            if (!"".equals(errorTab.setValue(errorHeaderIdField, errorHeaderId))) {
                throw new Exception("Unable to set Table in Error Log record");
            }
        }

        if (!errorTab.dataSave(false)) {
            throw new Exception("Failed to save new Error Log record");
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
                        log.warning("Callout error in field [" + mField.getColumnName() + "]: " + msg);
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
