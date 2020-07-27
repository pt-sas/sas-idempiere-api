package com.sahabatabadi.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.logging.Level;
import org.compiere.util.CLogger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.IRemoteApi;
import com.sahabatabadi.api.rmi.RemoteApi;

public class Activator implements BundleActivator {
    private static BundleContext context;
    private static Remote stub;
    private static Registry registry;

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

        startRmiServer();
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
    }

    private static void startRmiServer() {
        if (log.isLoggable(Level.INFO))
            log.info("Starting RMI Server");

        try {
            RemoteApi server = new RemoteApi();
            stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.createRegistry(1579);
            registry.rebind(IRemoteApi.BINDING_NAME, stub);

            if (log.isLoggable(Level.INFO))
                log.info("Server ready");
        } catch (RemoteException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Server exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private static void stopRmiServer() {
        if (log.isLoggable(Level.INFO))
            log.info("Stopping server");
        try { /* TODO ensure this method doesn't crash */
            registry.unbind(IRemoteApi.BINDING_NAME);

            /* TODO possible starvation issue */
            while (UnicastRemoteObject.unexportObject(registry, false)) {
                Thread.sleep(500);
            }

            while (UnicastRemoteObject.unexportObject(stub, false)) {
                Thread.sleep(500);
            }
        } catch (RemoteException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Server exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
