import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

public class Client {
    private Client() {}

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0]; // agrs[0] == host

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteApi stub = (RemoteApi) registry.lookup("RemoteApi3");
            String response = stub.testingRmi();
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
