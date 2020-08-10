package com.sahabatabadi.api;

import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Language;
import org.compiere.util.Login;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.RMIServer;

public class Activator implements BundleActivator {
	public static final String P_ROLE = "Role";
    public static final String P_CLIENT = "Client";
    public static final String P_ORG = "Organization";
    public static final String P_WAREHOUSE = "Warehouse";
    private static final String[] PROPERTIES = new String[] {P_ROLE, P_CLIENT, P_ORG, P_WAREHOUSE};

    private static BundleContext context;
    private static RMIServer rmiServer;

    protected static CLogger log = CLogger.getCLogger(Activator.class);

    static BundleContext getContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception {
        if (log.isLoggable(Level.INFO))
            log.info("SAS iDempiere API is starting");
        Activator.context = bundleContext;

        emulateLogin();

        if (rmiServer == null) {
            rmiServer = new RMIServer();
        }

        rmiServer.startRmiServer();
    }

    /*
    private static void emulateLogin() {
        // org.adempiere.webui.panel.RolePanel::setUserID()
        Env.setContext(Env.getCtx(), "#AD_User_Name", "Api-01");
        Env.setContext(Env.getCtx(), "#AD_User_ID", 2214213);
        Env.setContext(Env.getCtx(), "#SalesRep_ID", 2214213);

        // Login::getRoles
        Env.setContext(Env.getCtx(), "#AD_Client_ID", 1000001);
        Env.setContext(Env.getCtx(), "#AD_Client_Name", "sas");

        // Login::getOrgs(KeyNamePair)
        Env.setContext(Env.getCtx(), "#AD_Role_ID", 1000002);
        Env.setContext(Env.getCtx(), "#AD_Role_Name", "awn Admin");
        Env.setContext(Env.getCtx(), "#User_Level", " CO"); // Format 'SCO'

        // Login::loadPreferences(KeyNamePair, KeyNamePair, Timestamp, String)
        Env.setContext(Env.getCtx(), Env.AD_ORG_ID, 1000001);
        Env.setContext(Env.getCtx(), Env.AD_ORG_NAME, "Sunter");
        Env.setContext(Env.getCtx(), Env.M_WAREHOUSE_ID, 1000000);
        Env.setContext(Env.getCtx(), "#Date", new Timestamp(System.currentTimeMillis()));
    }
    */

    /**
     * validates user name and password when logging in
     *
     **/
    public void emulateLogin() {
        // org.adempiere.webui.panel.LoginPanel::validateLogin()
        Login login = new Login(Env.getCtx());
        String userId = "Api-01";
        String userPassword = "12345";

        KeyNamePair[] clientsKNPairs = login.getClients(userId, userPassword);

        // org.adempiere.webui.panel.RolePanel::init()
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
        KeyNamePair warehouseKNPair;
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

    public Properties loadPreference(int userId) {
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        if (log.isLoggable(Level.INFO))
            log.info("SAS iDempiere API is stopping");
        Activator.context = null;
        
        if (rmiServer != null) {
            rmiServer.stopRmiServer();
            rmiServer = null;
        }
    }
}
