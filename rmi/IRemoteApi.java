import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemoteApi extends Remote{  
    public boolean injectSo(BizzySalesOrder bizzySo) throws RemoteException;  
}  
