package com.sahabatabadi.api.rmi;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.logging.Level;
import org.compiere.util.CLogger;

public class RMIService {
    public static final int RMI_REGISTRY_PORT = 1579;
    
    private RemoteApi server;
    private Remote stub;
    private Registry registry;

    protected static CLogger log = CLogger.getCLogger(RMIService.class);

    public void start() {
        if (log.isLoggable(Level.INFO))
            log.info("Starting RMI registry service");

        try {
            server = new RemoteApi();
            stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            registry = LocateRegistry.createRegistry(RMI_REGISTRY_PORT);
            registry.rebind(IRemoteApi.BINDING_NAME, stub);

            if (log.isLoggable(Level.INFO))
                log.info("Registry ready");
        } catch (RemoteException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("RMI server exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (log.isLoggable(Level.INFO))
            log.info("Stopping RMI registry service");
        try {
            registry.unbind(IRemoteApi.BINDING_NAME);
        } catch (RemoteException | NotBoundException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("RMI server exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}