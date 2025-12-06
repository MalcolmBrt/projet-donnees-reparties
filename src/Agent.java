import java.util.Hashtable;
import java.io.Serializable;


public interface Agent extends Serializable {
	public void setNameServer(Hashtable<String,Object> ns);
	public Hashtable<String,Object> getNameServer();
	public void move(Node target) throws MoveException;
	public void main() throws MoveException;
}