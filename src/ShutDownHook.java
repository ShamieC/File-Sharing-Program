/**
 * This class has code that is executed when the Client program is exited by
 * pressing Ctrl-C or by System.exit();
 * 
 * The purpose of this class is send a notification to the server
 * that the client is no longer online.
 */

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class ShutDownHook extends Thread {
	InetAddress serverAddress = null;
	static String serverIP = null;
	static int serverPort = 4445;
	DatagramSocket datagramSocket= null;

	public void run () {
		try {
			serverAddress = InetAddress.getByName(serverIP);
		} catch (UnknownHostException u) {}

		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {}

		Client.sendExit();

	}

}
