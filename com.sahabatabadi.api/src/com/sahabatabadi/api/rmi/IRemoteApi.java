package com.sahabatabadi.api.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.sahabatabadi.api.salesorder.BizzySalesOrder;

public interface IRemoteApi extends Remote {
	public boolean injectSo(BizzySalesOrder bizzySo) throws RemoteException;  
}
