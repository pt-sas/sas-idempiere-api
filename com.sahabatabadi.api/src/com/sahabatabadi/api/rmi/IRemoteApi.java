package com.sahabatabadi.api.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.sahabatabadi.api.salesorder.BizzySalesOrder;

public interface IRemoteApi extends Remote {
    public static final String BINDING_NAME = "SASiDempiereRemoteApi";
	public static final int RMI_REGISTRY_PORT = 11579;
    
	public String injectSo(BizzySalesOrder bizzySo) throws RemoteException;  
}
