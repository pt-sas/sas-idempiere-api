package com.sahabatabadi.api;

import java.sql.Timestamp;
import java.util.logging.Level;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.RMIServer;

public class Activator implements BundleActivator {
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

    private static void emulateLogin() {
        // TODO emulate proper login
        Env.setContext(Env.getCtx(), "#AD_User_Name", "Fajar-170203");
        Env.setContext(Env.getCtx(), "#AD_User_ID", 2211127);
        Env.setContext(Env.getCtx(), "#SalesRep_ID", 2211127);
        Env.setContext(Env.getCtx(), "#AD_Role_ID", 1000110);
        Env.setContext(Env.getCtx(), "#AD_Role_Name", "Role SLS Admin");
        // Env.setContext(Env.getCtx(), "#AD_User_Name", "Api-01");
        // Env.setContext(Env.getCtx(), "#AD_User_ID", 2214213);
        // Env.setContext(Env.getCtx(), "#SalesRep_ID", 2214213);
        // Env.setContext(Env.getCtx(), "#AD_Role_ID", 1000002);
        // Env.setContext(Env.getCtx(), "#AD_Role_Name", "awn Admin");
        Env.setContext(Env.getCtx(), "#User_Level", " CO"); // Format 'SCO'
        Env.setContext(Env.getCtx(), Env.AD_ORG_ID, 1000001);
        Env.setContext(Env.getCtx(), Env.AD_ORG_NAME, "Sunter");
        Env.setContext(Env.getCtx(), Env.M_WAREHOUSE_ID, 1000000);
        Env.setContext(Env.getCtx(), "#Date", new Timestamp(System.currentTimeMillis()));
        Env.setContext(Env.getCtx(), "#ShowAcct", "N");
        Env.setContext(Env.getCtx(), "#ShowTrl", "Y");
        Env.setContext(Env.getCtx(), "#ShowAdvanced", "N");
        Env.setContext(Env.getCtx(), "#YYYY", "Y");
        Env.setContext(Env.getCtx(), "#StdPrecision", 2);
        Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", 1000001);
        Env.setContext(Env.getCtx(), "$C_Currency_ID", 303);
        Env.setContext(Env.getCtx(), "$HasAlias", "Y");
        Env.setContext(Env.getCtx(), "#C_Country_ID", 100);
        Env.setContext(Env.getCtx(), Env.LANGUAGE, "en_US");
        Env.setContext(Env.getCtx(), "#AD_Client_ID", 1000001);
        Env.setContext(Env.getCtx(), "#AD_Client_Name", "sas");
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
