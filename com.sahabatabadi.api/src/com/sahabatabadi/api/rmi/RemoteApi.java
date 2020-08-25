package com.sahabatabadi.api.rmi;

import com.sahabatabadi.api.salesorder.BizzySalesOrder;
import com.sahabatabadi.api.salesorder.SalesOrderInjector;

public class RemoteApi implements IRemoteApi {
	public String injectSo(BizzySalesOrder bizzySo) {
        SalesOrderInjector injector = new SalesOrderInjector();
        return injector.injectSalesOrder(bizzySo);
    }
}
