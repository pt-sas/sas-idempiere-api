package com.sahabatabadi.api;

import java.util.logging.Level;
import org.compiere.util.CLogger;

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
            log.info("SAS SO Injector is starting");
        Activator.context = bundleContext;

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
            log.info("SAS SO Injector is stopping");
        Activator.context = null;
        
        if (rmiServer != null) {
            rmiServer.stopRmiServer();
            rmiServer = null;
        }
    }
}
