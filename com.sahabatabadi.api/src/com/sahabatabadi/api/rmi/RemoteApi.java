package com.sahabatabadi.api.rmi;

import com.sahabatabadi.api.salesorder.BizzySalesOrder;
import com.sahabatabadi.api.salesorder.SOInjector;

public class RemoteApi implements IRemoteApi {
	public String injectSo(BizzySalesOrder bizzySo) {
        return SOInjector.apiName(bizzySo); // TODO introduce locks? What if two instances insert SO at the same time?
    }
}
