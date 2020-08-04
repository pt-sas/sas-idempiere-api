package com.sahabatabadi.api.rmi;

import com.sahabatabadi.api.salesorder.BizzySalesOrder;
import com.sahabatabadi.api.salesorder.SOInjector;

public class RemoteApi implements IRemoteApi {
	public String injectSo(BizzySalesOrder bizzySo) {
        SOInjector injector = new SOInjector();
        return injector.apiName(bizzySo);
    }
}
