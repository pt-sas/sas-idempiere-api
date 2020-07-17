import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class RemoteApi implements IRemoteApi {
    public RemoteApi() {}

    public boolean injectSo(BizzySalesOrder bizzySo) {
        return SOInjector.apiName(bizzySo);
    }

    public static void main(String args[]) {
        try {
            RemoteApi server = new RemoteApi();
            Remote stub = (Remote) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(1579);
            registry.rebind("SASiDempiereRemoteApi", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}