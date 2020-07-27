import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

public class Client {
    private Client() {}

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0]; 
        int port = (args.length < 1) ? null : Integer.parseInt(args[1]); 

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            IRemoteApi stub = (IRemoteApi) registry.lookup("SASiDempiereRemoteApi");
            boolean response = stub.injectSo(null); // TODO
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static BizzySalesOrder createTestBizzySo() {
        BizzySalesOrder bizzySo = new BizzySalesOrder();

        bizzySo.soff_code = 'A';
        bizzySo.description = "RMI Testing v1";
        bizzySo.dateOrdered = new Date(System.currentTimeMillis());
        bizzySo.bpHoldingNo = 3806;
        bizzySo.bpLocationName = "PIONEER ELECTRIC- Kenari Mas [Kenari Mas Jl. Kramat Raya Lt. Dasar Blok C No. 3-5]";

        bizzySo.orderLines = new BizzySalesOrderLine[5];
        bizzySo.orderLines[0] = new BizzySalesOrderLine();
        bizzySo.orderLines[0].productId = "AB0301485";
        bizzySo.orderLines[0].quantity = 20;
        bizzySo.orderLines[1] = new BizzySalesOrderLine();
        bizzySo.orderLines[1].productId = "AB0301440";
        bizzySo.orderLines[1].quantity = 30;
        bizzySo.orderLines[2] = new BizzySalesOrderLine();
        bizzySo.orderLines[2].productId = "AB0301430";
        bizzySo.orderLines[2].quantity = 40;
        bizzySo.orderLines[3] = new BizzySalesOrderLine();
        bizzySo.orderLines[3].productId = "AB0301420";
        bizzySo.orderLines[3].quantity = 50;
        bizzySo.orderLines[4] = new BizzySalesOrderLine();
        bizzySo.orderLines[4].productId = "AB0301635";
        bizzySo.orderLines[4].quantity = 60;

        return bizzySo;
    }
}
