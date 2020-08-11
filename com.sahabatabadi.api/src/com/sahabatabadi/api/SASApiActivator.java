package com.sahabatabadi.api;

import java.util.logging.Level;

import org.compiere.util.CLogger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.RMIServer;

public class SASApiActivator implements BundleActivator {
    private static BundleContext context;
    private static RMIServer rmiServer;

    protected CLogger log = CLogger.getCLogger(getClass());

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
        SASApiActivator.context = bundleContext;

        LoginEmulator.emulateLogin();

        if (rmiServer == null) {
            rmiServer = new RMIServer();
        }

        rmiServer.startRmiServer();
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
        SASApiActivator.context = null;
        
        if (rmiServer != null) {
            rmiServer.stopRmiServer();
            rmiServer = null;
        }
    }
}
