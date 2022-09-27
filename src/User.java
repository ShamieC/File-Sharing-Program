
import java.net.*;

/**
 * 
 * A data type that holds a client's information.
 *
 */
public  class User {
	String username;
	InetAddress address;
	int port;

	/**
	 * The class's constructor
	 * @param username The client's user name
	 * @param address The client's IP address
	 * @param port The client's UDP port 
	 */
	public User (String username,InetAddress address, int port) {
		this.username = username;
		this.address = address;
		this.port = port;
	}

	public String getName() {
		return username;
	}

	public InetAddress getIP() {
		return address;
	}

	public int getPort() {
		return port;
	}

}
