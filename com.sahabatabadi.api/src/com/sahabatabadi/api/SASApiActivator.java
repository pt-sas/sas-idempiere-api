package com.sahabatabadi.api;

import java.util.logging.Level;

import org.compiere.util.CLogger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.RMIService;

/**
 * Custom Activator class for SAS iDempiere API plugin
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SASApiActivator implements BundleActivator {
    private static BundleContext context;
    private static RMIService rmiServer;

    protected CLogger log = CLogger.getCLogger(getClass());

    static BundleContext getContext() {
        return context;
    }

    /**
     * Starts the SAS iDempiere API plugin and requisite services.
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception {
        if (log.isLoggable(Level.INFO))
            log.info("SAS iDempiere API is starting");
        SASApiActivator.context = bundleContext;

        if (rmiServer == null) {
            rmiServer = new RMIService();
        }

        rmiServer.start();
        
        LoginEmulator.emulateLogin();
        ThreadPoolManager.reinitialize();
    }

    /*
     * Stops the SAS iDempiere API plugin and requisite services, 
     * and then cleans up associated resources.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        if (log.isLoggable(Level.INFO))
            log.info("SAS iDempiere API is stopping");
        SASApiActivator.context = null;
        
        if (rmiServer != null) {
            rmiServer.stop();
            rmiServer = null;
        }
        
        ThreadPoolManager.stop();
    }
}
