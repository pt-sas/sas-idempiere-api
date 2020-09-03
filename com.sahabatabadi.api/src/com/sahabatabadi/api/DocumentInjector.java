package com.sahabatabadi.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

/**
 * A class to inject {@link DocHeader} into iDempiere.
 */
public class DocumentInjector {
    /**
     * iDempiere Menu name for the API Error Log window
     */
    private final String API_ERROR_LOG_MENU_NAME = "API Error Log";

    /**
     * Prefix for logger messages
     */
    private final String PLUGIN_PREFIX = "[SAS iDempiere API] ";

    protected CLogger log = CLogger.getCLogger(getClass());

    /**
     * Window number last returned by {@link #getNextWindowNo()}.
     */
    private static int lastReturnedWindowNo = 1000;

    /**
     * Menu ID associated with the injecting iDempiere Window
     */
    private final int menuId;

    /**
     * Window ID associated with the injecting iDempiere Window
     */
    private final int windowId;

    /**
     * GridTab of the header record
     */
    private GridTab headerTab;

    /**
     * List of GridTab of the line records
     */
    private List<GridTab> childs;

    /**
     * Whether the header/master record for the error log has been created
     */
    private boolean errorHeaderCreated;

    /**
     * Whether the injector failed to inject the documents
     */
    private boolean isError;

    /**
     * Transaction object representing the insertion operation
     */
    private Trx trx;
    
    /**
     * Name of the {@link #trx} object
     */
    private String trxName;

    /**
     * Persistent Object representing the header/master record of the document being
     * injected
     */
    private PO masterRecord;

    /**
     * Creates a DocumentInjector instance associated with a specific iDempiere
     * Window. The Window determines the resulting record type. For example,
     * injecting documents through the Sales Order window will create SO documents.
     * 
     * @param windowId menu ID associated with the injecting iDempiere Window
     * @param menuId   window ID associated with the injecting iDempiere Window
     */
    public DocumentInjector(int windowId, int menuId) {
        this.windowId = windowId;
        this.menuId = menuId;
    }

    /**
     * Injects a document through the iDempiere window with ID {@link #windowId}
     * 
     * @param headerObj header of the document to be injected
     * @return true if document is successfully injected, false otherwise
     */
    public boolean injectDocument(DocHeader headerObj) {
        // org.adempiere.impexp.GridTabCSVImporter#fileImport
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

            GridTab detailTabCache = null;
            for (ApiInjectable lineRecord : headerObj.getLines()) {
                if (detailTabCache == null || !lineRecord.getTableName().equals(detailTabCache.getTableName())) {
                    for (GridTab child : childs) {
                        if (lineRecord.getTableName().equals(child.getTableName())) {
                            detailTabCache = child;
                            break;
                        }
                    }
                }

                try {
                    processRecord(detailTabCache, true, lineRecord);
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

    /**
     * Checks whether the document is valid to be injected. This includes checking
     * whether the header and all its lines have primary keys
     * 
     * @param headerObj header of the document to be injected
     * @return true if the header and all its lines are a valid, false otherwise
     */
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

    /**
     * Initializes the GridTab objects {@link #headerTab} and {@link #childs}
     * associated with iDempiere window with ID {@link #windowId}
     */
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

    /**
     * Gets the next available number for a new iDempiere Window object. This window
     * number is guaranteed to be unique; two different instances of
     * {@link DocumentInjector} calling this method is guaranteed to get a different
     * window number
     * 
     * @return next available unique window number
     */
    private static int getNextWindowNo() {
        // TODO has to be synchronized in multithreading
        DocumentInjector.lastReturnedWindowNo += 1;
        return DocumentInjector.lastReturnedWindowNo;
    }

    /**
     * Prepares the GridTab for insertion, inserts the so into the GridTab, then
     * commits the changes.
     * 
     * @param gridTab  GridTab of the {@link ApiInjectable} being injected. Can be
     *                 header or detail GridTab.
     * @param isDetail true if {@link ApiInjectable} being injected is a detail.
     * @param so       the {@link ApiInjectable} being injected.
     * @throws SASApiException
     */
    private void processRecord(GridTab gridTab, boolean isDetail, ApiInjectable so) throws SASApiException {
        // org.adempiere.impexp.GridTabCSVImporter#processRecord
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

    /**
     * Inserts the so into the prepared GridTab.
     * 
     * <p>
     * Additionally, when inserting header, {@link #masterRecord} is null.
     * 
     * @param gridTab
     * @param trx
     * @param so
     * @throws SASApiException
     */
    private void processRow(GridTab gridTab, Trx trx, ApiInjectable so) throws SASApiException {
        // org.adempiere.impexp.GridTabCSVImporter#proccessRow
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

    /**
     * Helper method to check whether the value has changed from the old value. In
     * other words, it checks if the two values are different.
     * 
     * @param oldValue old value of the database field
     * @param value    new value of the database field
     * @return true if the value has changed, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean isValueChanged(Object oldValue, Object value) {
        // org.compiere.model.GridTable#isValueChanged(Object, Object)
        if (oldValue != null && (oldValue instanceof String) && oldValue.toString().equals("")) {
            oldValue = null;
        }

        if (value != null && (value instanceof String) && value.toString().equals("")) {
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

    /**
     * Creates a Trx object related to the specified GridTab object to perform DB
     * insertion.
     * 
     * @param gridTab GridTab associated with inserting the created Trx object
     */
    private void createTrx(GridTab gridTab) {
        trxName = "Import_" + gridTab.getTableName() + "_" + UUID.randomUUID();
        gridTab.getTableModel().setImportingMode(true, trxName);
        trx = Trx.get(trxName, true);
        masterRecord = null;
    }

    /**
     * Closes the Trx object related to the specified GridTab object after DB
     * insertion.
     * 
     * @param gridTab GridTab associated with inserting the created Trx object
     */
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

    /**
     * Helper method ot clean the record name of the extra annotation, returning the
     * remaining column name.
     * 
     * @param isKey     true if the column is a key column
     * @param isForeign true if the column references a foreign table
     * @param isDetail  true if the column belongs in a detail record
     * @param headName  header name
     * @return column name after cleaning
     */
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

    /**
     * Helper method to resolve the ID of records in the AD_Ref_List table, given
     * value, the column, and the foreign column.
     * 
     * @param column        column name of the AD_Reference
     * @param foreignColumn column name of the value specified
     * @param value         value to be searched in the foreign table.
     * @param trx           Trx object representing the insertion operation
     * @return ID of the foreign record
     */
    private String resolveForeignList(MColumn column, String foreignColumn, Object value, Trx trx) {
        String idS = null;
        String trxName = (trx != null ? trx.getTrxName() : null);
        StringBuilder select = new StringBuilder("SELECT Value FROM AD_Ref_List WHERE ").append(foreignColumn)
                .append("=? AND AD_Reference_ID=? AND IsActive='Y'");
        idS = DB.getSQLValueStringEx(trxName, select.toString(), value, column.getAD_Reference_Value_ID());
        return idS;
    }

    /**
     * Helper method to resolve the ID of records in a foreign table. This method
     * searches for the value in the foreign column and foreign table, and returns
     * the ID of the record.
     * 
     * @param foreignTable  table where the record resides
     * @param foreignColumn column name of the value specified
     * @param value         value to be searched in the foreign table.
     * @param trx           Trx object representing the insertion operation
     * @return ID of the foreign record
     */
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

    /**
     * Inserts an entry to the error log table capturing the entire content of the
     * SO upon insertion failure.
     * 
     * <p> 
     * Should this method encounter another exception, the SO content will be
     * printed to the console instead.
     * 
     * @param apiException Exception object containing information about the failed
     *                     SO
     */
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

    /**
     * Helper method to insert a failed injectable object into the error log.
     *
     * @param so       Failed injectable object to be inserted to the error log
     *                 table.
     * @param windowId Window ID of the Tab associated with the failed injectable
     *                 objcet
     * @param tabId    Tab ID of the Tab associated with the failed injectable
     *                 object
     * @param isDetail true if the failed injectable object is a detail record
     * @param errorLog optional error message to be inserted along with the record
     *                 content
     * @throws Exception
     */
    private void insertErrorLogHelper(ApiInjectable so, int windowId, int tabId, boolean isDetail, String errorLog) throws Exception {
        String documentNo = so.getDocumentNo();

        String errorLogMenuQuery = new StringBuilder("SELECT AD_Menu_ID, AD_Window_ID ")
                .append("FROM AD_Menu ")
                .append("WHERE IsActive='Y' AND name LIKE ")
                .append("'").append(API_ERROR_LOG_MENU_NAME).append("'")
                .toString();

        int errorLogMenuId = -1; // value should be 2200138
        int errorLogWindowId = -1; // value should be 2200001

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(errorLogMenuQuery, null);
            rs = pstmt.executeQuery();
            if (rs.next())
                errorLogMenuId = rs.getInt(1);
                errorLogWindowId = rs.getInt(2);
        } catch (Exception e) {
            log.log(Level.SEVERE, errorLogMenuQuery, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }

        if (errorLogMenuId == -1 || errorLogWindowId == -1) {
        	throw new Exception("Could not find menu ID and/or window ID for error log window!");
        }

        final int windowNo = getNextWindowNo();
        GridWindowVO errorLogWindowVO = GridWindowVO.create(Env.getCtx(), windowNo, errorLogWindowId,
                errorLogMenuId);
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

    /**
     * Helper class to trigger callouts when data in the associated GridTab's
     * field changes.
     */
    class GridTabHolder implements DataStatusListener {
        private GridTab gridTab;

        /**
         * Default constructor. 
         * 
         * @param gTab GridTab object to monitor for data changes
         */
        public GridTabHolder(GridTab gTab) {
            this.gridTab = gTab;
            gridTab.addDataStatusListener(this);
        }

        /**
         * Called when data in the associated GridTab's field changes.
         * 
         * @param e Event object containing information about the trigger
         */
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
