package com.sahabatabadi.api;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sahabatabadi.api.rmi.RemoteApi;

public class Activator implements BundleActivator {
    private static BundleContext context;
    private static Remote stub;

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
        System.out.println("SAS SO Injector is starting");
        Activator.context = bundleContext;

        startRmiServer();
    }

    private static void startRmiServer() {
        System.out.println("Starting RMI Server");

        try {
            RemoteApi server = new RemoteApi();
            stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(1579);
            registry.rebind("SASiDempiereRemoteApi", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        System.out.println("SAS SO Injector is stopping");
        Activator.context = null;
    }
}
