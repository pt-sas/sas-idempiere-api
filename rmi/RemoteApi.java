import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteApi extends Remote{  
    public String injectSo(BizzySalesOrder bizzySo) throws RemoteException;  
    // public String testingRmi() throws RemoteException;
}  
