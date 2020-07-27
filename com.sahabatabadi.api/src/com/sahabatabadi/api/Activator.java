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
    private static Registry registry;

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

    private static void startRmiServer() {
        System.out.println("Starting RMI Server");

        try {
            RemoteApi server = new RemoteApi();
            stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.createRegistry(1579);
            registry.rebind("SASiDempiereRemoteApi", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void stopRmiServer() {
        System.out.println("Stopping server");
        try { /* TODO ensure this method doesn't crash */
            registry.unbind("SASiDempiereRemoteApi");
            while (UnicastRemoteObject.unexportObject(registry, false)) {
                Thread.sleep(500);
            }

            while (UnicastRemoteObject.unexportObject(stub, false)) {
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
