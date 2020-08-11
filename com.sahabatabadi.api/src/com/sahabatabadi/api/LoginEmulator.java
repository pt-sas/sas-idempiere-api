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

/**
 * Class with static methods to emulate iDempiere login
 */
public class LoginEmulator {
	public static final String P_ROLE = "Role";
    public static final String P_CLIENT = "Client";
    public static final String P_ORG = "Organization";
    public static final String P_WAREHOUSE = "Warehouse";
    public static final String[] PROPERTIES = new String[] {P_ROLE, P_CLIENT, P_ORG, P_WAREHOUSE};

    public static final String USER_ID = "Api-01";
    public static final String USER_PASSWORD = "12345";

    /**
     * Emulates login to iDempiere with username {@value #USER_ID} and password
     * {@value #USER_PASSWORD}. Client, role, org, and warehouse follows the value
     * set in the "User Preference" window in iDempiere.
     */
    public static boolean emulateLogin() {
        // org.adempiere.webui.panel.LoginPanel::validateLogin()
        Login login = new Login(Env.getCtx());

        KeyNamePair[] clientsKNPairs = login.getClients(USER_ID, USER_PASSWORD);

        // org.adempiere.webui.panel.RolePanel::init()
        Env.setContext(Env.getCtx(), "#AD_Client_ID", clientsKNPairs[0].getID());
        MUser user = MUser.get(Env.getCtx(), USER_ID);
        Properties preference = loadUserPreference(user.get_ID());

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
        KeyNamePair[] roleKNPairs = login.getRoles(USER_ID, clientKNPair);
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
            return false;
        }

        return true;
    }

    /**
     * Loads the login preferences from the User Preference window in iDempiere
     * 
     * @param userId User ID of the user whose preference should be loaded.
     * @return User Preference of the user with the specified user ID.
     */
    public static Properties loadUserPreference(int userId) {
        // org.adempiere.webui.util.UserPreference::loadPreference(int)
        Properties props = new Properties();
        
        String propQueryStr = "NVL(AD_User_ID,0) = ? AND Attribute = ? AND AD_Window_ID Is NULL AND AD_Process_ID IS NULL AND PreferenceFor = 'W'";
        Query query = new Query(Env.getCtx(), I_AD_Preference.Table_Name, propQueryStr, null);

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
