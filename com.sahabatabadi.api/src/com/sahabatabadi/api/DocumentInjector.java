package com.sahabatabadi.api;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;

public class DocumentInjector {
	public static final String PLUGIN_PREFIX = "[SAS iDempiere API] ";
	private static final String ORDER_LINE_TABLE_NAME = "C_OrderLine";
	
	protected CLogger log = CLogger.getCLogger(getClass());

	private boolean isError = false;
	private Trx trx;
    private String trxName;
    private PO masterRecord;

    public void injectDocument(GridTab headerTab, List<GridTab> childs, SASApiHeader headerObj) {
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
