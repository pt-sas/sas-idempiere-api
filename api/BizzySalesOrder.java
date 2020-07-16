import java.io.Serializable;
import java.util.Date;

public class BizzySalesOrder implements Serializable {
    private static final long serialVersionUID = 20200709L;

    public char soff_code; // soff_code Kenari mau dipisahkan dengan Sunter
    public String description;
    public Date dateOrdered;
    public int bpHoldingNo;
    public String bpLocationName; // TODO kode internal Bizzy?

    public BizzySalesOrderLine[] orderLines;
}