package com.sahabatabadi.api;

import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Login;
import org.compiere.util.Util;

public class LoginEmulator {
	public static final String P_ROLE = "Role";
    public static final String P_CLIENT = "Client";
    public static final String P_ORG = "Organization";
    public static final String P_WAREHOUSE = "Warehouse";
    private static final String[] PROPERTIES = new String[] {P_ROLE, P_CLIENT, P_ORG, P_WAREHOUSE};
    
    public static void emulateLogin() {
        // org.adempiere.webui.panel.LoginPanel::validateLogin()
        Login login = new Login(Env.getCtx());
        String userId = "Api-01";
        String userPassword = "12345";

        KeyNamePair[] clientsKNPairs = login.getClients(userId, userPassword);

        // org.adempiere.webui.panel.RolePanel::init()
        Env.setContext(Env.getCtx(), "#AD_Client_ID", clientsKNPairs[0].getID());
        MUser user = MUser.get(Env.getCtx(), userId);
        Properties preference = loadPreference(user.get_ID());

        // org.adempiere.webui.panel.RolePanel::initComponents()
        KeyNamePair clientKNPair = null;
        String initDefaultClient = preference.getProperty(P_CLIENT, "");
        for (int i = 0; i < clientsKNPairs.length; i++) {
            if (clientsKNPairs[i].getID().equals(initDefaultClient)) {
                clientKNPair = clientsKNPairs[i];
                break;
            }
        }

        // org.adempiere.webui.panel.RolePanel::setUserID()
        Env.setContext(Env.getCtx(), "#AD_Client_ID", clientKNPair.getID());
        if (user != null) {
            Env.setContext(Env.getCtx(), "#AD_User_ID", user.getAD_User_ID());
            Env.setContext(Env.getCtx(), "#AD_User_Name", user.getName());
            Env.setContext(Env.getCtx(), "#SalesRep_ID", user.getAD_User_ID());
        }

        // org.adempiere.webui.panel.RolePanel::updateRoleList()
        KeyNamePair roleKNPair = null;
        KeyNamePair[] roleKNPairs = login.getRoles(userId, clientKNPair);
        String initDefaultRole = preference.getProperty(P_ROLE, "");
        for (int i = 0; i < roleKNPairs.length; i++) {
            if (roleKNPairs[i].getID().equals(initDefaultRole)) {
                roleKNPair = roleKNPairs[i];
                break;
            }
        }

        // org.adempiere.webui.panel.RolePanel::updateOrganisationList()
        KeyNamePair organisationKNPair = null;
        KeyNamePair[] orgKNPairs = login.getOrgs(roleKNPair);
        String initDefaultOrg = preference.getProperty(P_ORG, "");
        for (int i = 0; i < orgKNPairs.length; i++) {
            if (orgKNPairs[i].getID().equals(initDefaultOrg)) {
                organisationKNPair = orgKNPairs[i];
                break;
            }
        }

        // org.adempiere.webui.panel.RolePanel::updateWarehouseList()
        KeyNamePair warehouseKNPair = null;
        KeyNamePair[] warehouseKNPairs = login.getWarehouses(organisationKNPair);
        String initDefaultWarehouse = preference.getProperty(P_WAREHOUSE, "");
        for (int i = 0; i < warehouseKNPairs.length; i++) {
            if (warehouseKNPairs[i].getID().equals(initDefaultWarehouse)) {
                warehouseKNPair = warehouseKNPairs[i];
                break;
            }
        }

        // org.adempiere.webui.panel.RolePanel::validateRoles()
        Timestamp date = new Timestamp(System.currentTimeMillis());
        String msg = login.loadPreferences(organisationKNPair, warehouseKNPair, date, null);
        if (Util.isEmpty(msg)) {
            msg = login.validateLogin(organisationKNPair);
        }

        if (!Util.isEmpty(msg)) {
            Env.getCtx().clear();
            return;
        }
    }

    public static Properties loadPreference(int userId) {
        // org.adempiere.webui.util.UserPreference::loadPreference(int)
        Properties props = new Properties();
        
        Query query = new Query(Env.getCtx(), I_AD_Preference.Table_Name,
                "NVL(AD_User_ID,0) = ? AND Attribute = ? AND AD_Window_ID Is NULL AND AD_Process_ID IS NULL AND PreferenceFor = 'W'",
                null);

        for (int i = 0; i < PROPERTIES.length; i++) {
            String attribute = PROPERTIES[i];
            String value = "";

            MPreference preference = query.setParameters(new Object[] { userId, attribute }).firstOnly();
            if (preference != null && preference.getValue() != null) {
                value = preference.getValue();
            }

            props.setProperty(attribute, value);
        }

        return props;
    }
}
