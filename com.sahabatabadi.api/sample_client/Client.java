import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.sahabatabadi.api.rmi.IRemoteApi;
import com.sahabatabadi.api.rmi.MasterDataNotFoundException;
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
            System.out.println("Remote response: " + response);
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        } catch (MasterDataNotFoundException e) {
            System.err.println("Master data exception caught: " + e.getMessage());
        }
    }

    public static BizzySalesOrder createTestBizzySo() {
        BizzySalesOrder bizzySo = new BizzySalesOrder();

        bizzySo.soff_code = 'A';
        bizzySo.description = "Testing principal and discount retrieval from DB";
        bizzySo.dateOrdered = new Timestamp(System.currentTimeMillis());
        bizzySo.bpHoldingCode = "03806";
        // bizzySo.bpLocationName = "2201478"; // "PIONEER ELECTRIC- Kenari Mas [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]"; // non-tax
        bizzySo.bpLocationCode = "2205193"; // "PIONIR ELEKTRIK INDONESIA [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]"; // tax
        bizzySo.orderSource = 'S';

        ArrayList<BizzySalesOrderLine> orderlines = new ArrayList<>();

        BizzySalesOrderLine bizzySo0 = new BizzySalesOrderLine();
        orderlines.append(bizzySo0);
        bizzySo0.productCode = "AB0301485";
        bizzySo0.quantity = 100;
        // bizzySo0.principalId = "Philips";
        // bizzySo0.discount = "8";
        
        BizzySalesOrderLine bizzySo1 = new BizzySalesOrderLine();
        orderlines.append(bizzySo1);
        bizzySo1.productCode = "AB0301440";
        bizzySo1.quantity = 20;
        // bizzySo1.principalId = "Philips";
        // bizzySo1.discount = "8";
        
        // BizzySalesOrderLine bizzySo2 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo2.productCode = "AB0301430";
        // bizzySo2.quantity = 40;
        // // bizzySo2.principalId = "Philips";
        // // bizzySo2.discount = "8";
        
        // BizzySalesOrderLine bizzySo3 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo3.productCode = "BA0600010";
        // bizzySo3.quantity = 20;
        // // bizzySo3.principalId = "Panasonic";
        // // bizzySo3.discount = "61.0";

        // BizzySalesOrderLine bizzySo4 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo4.productCode = "BA0100116";
        // bizzySo4.quantity = 20;
        // // bizzySo4.principalId = "Panasonic";
        // // bizzySo4.discount = "51.0";

        // BizzySalesOrderLine bizzySo5 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo5.productCode = "FA0600085";
        // bizzySo5.quantity = 20;
        // // bizzySo5.principalId = "Schneider";
        // // bizzySo5.discount = "36.3";
    
        // BizzySalesOrderLine bizzySo6 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo6.productCode = "FA0100080";
        // bizzySo6.quantity = 20;
        // // bizzySo6.principalId = "Schneider";
        // // bizzySo6.discount = "38";

        // BizzySalesOrderLine bizzySo7 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo7.productCode = "GA0100000";
        // bizzySo7.quantity = 20;
        // // bizzySo7.principalId = "Supreme";
        // // bizzySo7.discount = "42.01";

        // BizzySalesOrderLine bizzySo8 = new BizzySalesOrderLine();
        // orderlines.append(ne bizzy);
        // bizzySo8.productCode = "GA0500010";
        // bizzySo8.quantity = 20;
        // // bizzySo8.principalId = "Supreme";
        // // bizzySo8.discount = "31.14";

        bizzySo.orderLines = orderlines.toArray(new BizzySalesOrderLine[orderlines.size()]);
        
        return bizzySo;
    }
}
