import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class ConnectionListener implements Remote {
    public ConnectionListener() {}

    public String testingRmi() {
        System.out.println("Someone's connecting to this");
        return "This is just a placeholder because iDempiere-related dependencies didn't compile";
    }

    public static void main(String args[]) {

        try {
            ConnectionListener obj = new ConnectionListener();
            Remote stub = UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("RemoteApi3", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}