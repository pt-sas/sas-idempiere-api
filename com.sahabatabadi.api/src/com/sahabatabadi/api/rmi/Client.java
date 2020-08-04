package com.sahabatabadi.api.rmi; // change when deploying

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

import com.sahabatabadi.api.rmi.IRemoteApi;
import com.sahabatabadi.api.salesorder.BizzySalesOrder;
import com.sahabatabadi.api.salesorder.BizzySalesOrderLine;

public class Client {
    private Client() {}

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0]; 
        int port = (args.length < 1) ? null : Integer.parseInt(args[1]); 

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            IRemoteApi stub = (IRemoteApi) registry.lookup(IRemoteApi.BINDING_NAME);
            String response = stub.injectSo(createTestBizzySo());
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static BizzySalesOrder createTestBizzySo() {
        BizzySalesOrder bizzySo = new BizzySalesOrder();

        bizzySo.soff_code = 'A';
        bizzySo.description = "Testing principal and discount retrieval from DB";
        bizzySo.dateOrdered = new Date(System.currentTimeMillis());
        bizzySo.bpHoldingNo = 3806;
        // bizzySo.bpLocationName = "PIONEER ELECTRIC- Kenari Mas [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]"; // non-tax
        bizzySo.bpLocationName = "PIONIR ELEKTRIK INDONESIA [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]"; // tax
        bizzySo.orderSource = 'B';

        bizzySo.orderLines = new BizzySalesOrderLine[9];

        bizzySo.orderLines[0] = new BizzySalesOrderLine();
        bizzySo.orderLines[0].productId = "AB0301485";
        bizzySo.orderLines[0].quantity = 10;
        // bizzySo.orderLines[0].principalId = "Philips";
        // bizzySo.orderLines[0].discount = "8";
        
        bizzySo.orderLines[1] = new BizzySalesOrderLine();
        bizzySo.orderLines[1].productId = "AB0301440";
        bizzySo.orderLines[1].quantity = 20;
        // bizzySo.orderLines[1].principalId = "Philips";
        // bizzySo.orderLines[1].discount = "8";
        
        bizzySo.orderLines[2] = new BizzySalesOrderLine();
        bizzySo.orderLines[2].productId = "AB0301430";
        bizzySo.orderLines[2].quantity = 40;
        // bizzySo.orderLines[2].principalId = "Philips";
        // bizzySo.orderLines[2].discount = "8";
        
        bizzySo.orderLines[3] = new BizzySalesOrderLine();
        bizzySo.orderLines[3].productId = "BA0600010";
        bizzySo.orderLines[3].quantity = 20;
        // bizzySo.orderLines[3].principalId = "Panasonic";
        // bizzySo.orderLines[3].discount = "61.0";

        bizzySo.orderLines[4] = new BizzySalesOrderLine();
        bizzySo.orderLines[4].productId = "BA0100116";
        bizzySo.orderLines[4].quantity = 20;
        // bizzySo.orderLines[4].principalId = "Panasonic";
        // bizzySo.orderLines[4].discount = "51.0";

        bizzySo.orderLines[5] = new BizzySalesOrderLine();
        bizzySo.orderLines[5].productId = "FA0600085";
        bizzySo.orderLines[5].quantity = 20;
        // bizzySo.orderLines[5].principalId = "Schneider";
        // bizzySo.orderLines[5].discount = "36.3";
    
        bizzySo.orderLines[6] = new BizzySalesOrderLine();
        bizzySo.orderLines[6].productId = "FA0100080";
        bizzySo.orderLines[6].quantity = 20;
        // bizzySo.orderLines[6].principalId = "Schneider";
        // bizzySo.orderLines[6].discount = "38";

        bizzySo.orderLines[7] = new BizzySalesOrderLine();
        bizzySo.orderLines[7].productId = "GA0100000";
        bizzySo.orderLines[7].quantity = 20;
        // bizzySo.orderLines[7].principalId = "Supreme";
        // bizzySo.orderLines[7].discount = "42.01";

        bizzySo.orderLines[8] = new BizzySalesOrderLine();
        bizzySo.orderLines[8].productId = "GA0500010";
        bizzySo.orderLines[8].quantity = 20;
        // bizzySo.orderLines[8].principalId = "Supreme";
        // bizzySo.orderLines[8].discount = "31.14";
        
        return bizzySo;
    }
}
