package com.sahabatabadi.api.rmi;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.logging.Level;
import org.compiere.util.CLogger;

protected static CLogger log = CLogger.getCLogger(RMIServer.class);

public class RMIServer {
    public static final int RMI_REGISTRY_PORT = 1579;
    
    private Remote stub;
    private Registry registry;

    public void startRmiServer() {
        if (log.isLoggable(Level.INFO))
            log.info("Starting RMI Server");

        try {
            RemoteApi server = new RemoteApi();
            stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.createRegistry(RMI_REGISTRY_PORT);
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

    public void stopRmiServer() {
        if (log.isLoggable(Level.INFO))
            log.info("Stopping RMI server");
        try {
            registry.unbind(IRemoteApi.BINDING_NAME);
        } catch (RemoteException | NotBoundException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Server exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}