package common;

import java.util.Hashtable;
import java.io.Serializable;


public interface Agent extends Serializable {
	public void setServices(Hashtable<String,Object> services);
	public Hashtable<String,Object> getServices();
	public void move(Node target) throws MoveException;
	public void main() throws MoveException;
}