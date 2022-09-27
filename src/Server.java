
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	private static final int DATAGRAMPORT = 4445;
	static DatagramSocket datagramSocket = null;
	static ServerGUI gui = null;
	static HashMap<String, User> clients = new HashMap<String, User>();
	static ArrayList<ServerSearchThread> ongoingSearches = new ArrayList<ServerSearchThread>();

	public static void main(String[] args) {

		createDatagramSocket();
		printActivity("Server", " listening .....");
		gui = new ServerGUI();
		gui.setVisible(true);
		while (true) {
			receiveDatagrams();
		}
	}

	/**
	 * Creates a datagram socket at port number DATAGRAMPORT.
	 */
	private static void createDatagramSocket() {
		try {
			datagramSocket = new DatagramSocket(DATAGRAMPORT);
		} catch (SocketException e) {
			Util.printErrorAndExit("Server could not create or access socket.");
		}

	}

	/**
	 * Prints client/server activity to terminal.
	 * 
	 * @param clientName
	 * @param clientActivity
	 */
	private static void printActivity(String clientName, String clientActivity) {
		System.out.println(">>>>> " + clientName + clientActivity + "\n");
	}

	/**
	 * Prints name of clients to terminal. Useful for testing
	 */
	public void printClientList() {
		for (Map.Entry client : clients.entrySet()) {
			System.out.println(client.getKey());
		}

	}

	/**
	 * Receives packets from clients and decides what to based on the first byte of
	 * the packet.
	 */
	public static void receiveDatagrams() {
		byte[] buf = new byte[Util.TRANS_BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		try {
			datagramSocket.receive(packet);
		} catch (IOException e) {
			Util.printErrorAndExit("Could not receive datagramPacket");
		}

		// Process packets
		byte firstbyte = buf[0];
		if (firstbyte == Util.CONNECT) {
			getClientInfo(packet, buf);
		} else if (firstbyte == Util.SEARCH) {
			createSearchThread(packet, buf);
		} else if (firstbyte == Util.EXIT) {
			removeClient(packet, buf);
		} else if (firstbyte == Util.DOWNLOAD_REQUEST) {
			forwardDownloadRequest(packet, buf);
		} else if (firstbyte == Util.SEND_MSG) {
			forwardMsg(packet, buf);
		}

	}

	/**
	 * Method used to pass a download request. The method prepends requestor's
	 * details to the original download request message before sending it.
	 * 
	 * @param packet
	 * @param buf
	 */
	private static void forwardDownloadRequest(DatagramPacket packet, byte[] buf) {
		String msg = "";
		String rcvr = null;
		String downloadRequestor = getUsername(packet); //person requesting download
		InetAddress requestorAddress = getAddress(downloadRequestor);
		int requestorPort = getPort(downloadRequestor);

		try {
			msg = new String(buf, 1, packet.getLength(), "UTF-8");
			rcvr = msg.split(",")[0];

			String[] res = msg.split(",");
			String update = downloadRequestor + " requested download \"" + res[1] + "\"";
			gui.updateActivity(update);

			msg = downloadRequestor + "," + requestorAddress + "," + requestorPort + "," + msg;
		} catch (UnsupportedEncodingException e) {
			Util.printErrorAndExit(e.toString());
		}


		byte[] msgBuf = msg.getBytes();
		byte[] buffer = new byte[1 + msgBuf.length];

		buffer[0] = Util.DOWNLOAD_REQUEST;
		for (int i = 0; i < msgBuf.length; i++) {
			buffer[i + 1] = msgBuf[i];
		}

		DatagramPacket packie = new DatagramPacket(buffer, buffer.length, getAddress(rcvr), getPort(rcvr));
		sendDatagram(packie);

	}


	/**
	 * Forwards a message to the recipient. Received message contains the
	 * recipient's user name and message. So the server uses the user name to look
	 * up the client's address. It prefixes the src's user name to the message and
	 * then forwards the message.
	 * 
	 * @param packet Received message packet
	 * @param buf    The packet contents
	 */
	private static void forwardMsg(DatagramPacket packet, byte[] buf) {

		String msgContents = "";
		for (int i = 1; i < buf.length; i++) {
			int charCode = buf[i];
			if (charCode == 0) {
				break;
			}
			char[] ch = Character.toChars(charCode);
			String s = new String(ch);
			msgContents = msgContents + s;
		}

		String[] msgContentsArray = msgContents.split(",");
		String senderUsername = msgContentsArray[0].trim();
		String recipientUsername = msgContentsArray[1].trim();
		String msg = msgContentsArray[2].trim();

		InetAddress recipientAddress = getAddress(recipientUsername);
		int recipientPort = getPort(recipientUsername);

		if (recipientAddress != null && recipientPort != 0) {
			String messageToForward = senderUsername + "," + msg;
			byte[] msgBuf = new byte[Util.TRANS_BUFFER_SIZE];
			msgBuf[0] = (byte) Util.RCV_MSG;

			byte[] messageToForwardBuf = messageToForward.getBytes();

			for (int i = 0; i < messageToForwardBuf.length; i++) {
				msgBuf[i + 1] = messageToForwardBuf[i];
			}

			DatagramPacket msgPacket = new DatagramPacket(msgBuf, msgBuf.length, recipientAddress, recipientPort);
			sendDatagram(msgPacket);
		} else {
			Util.printErrorAndExit("Message could not be send");
		}
	}


	/**
	 * Uses the InetAddress from the packet to get the user name of the sender of
	 * the packet.
	 * 
	 * @param packet Received packet.
	 * @return Returns the name of the packet's sender.
	 */
	public static String getUsername(DatagramPacket packet) {
		InetAddress address = packet.getAddress();
		int port = packet.getPort();

		for (Map.Entry client : clients.entrySet()) {
			String name = (String) client.getKey();
			User user = (User) client.getValue();
			if ((user.port == port) && (user.address.equals(address))) {
				return name;
			}
		}
		return null;
	}

	/**
	 * Creates a thread to carry out a search.
	 * 
	 * @param packet Received packet with search request.
	 * @param buf    A byte array with the packet contents.
	 */
	private static void createSearchThread(DatagramPacket packet, byte[] buf) {

		String msg = Client.getStringMessage(packet, buf).trim();
		String[] results = msg.split(";");
		String update = results[0] + " made a search request for \"" + results[1] + "\"";
		gui.updateActivity(update);

		ServerSearchThread search = new ServerSearchThread(packet, buf);
		search.start();

	}

	
	/**
	 * Given a client's user name, the method looks up the client's inet address.
	 * 
	 * @param username The client's user name.
	 * @return Returns the client's address
	 */
	public static InetAddress getAddress(String username) {
		for (Map.Entry client : clients.entrySet()) {
			String clientName = (String) client.getKey();
			clientName = clientName.trim();
			User newUser = (User) client.getValue();
			InetAddress address = (InetAddress) newUser.getIP();
			if (username.equals(clientName)) {
				return address;
			}
		}
		return null;

	}

	/**
	 * Given a client's address, the method looks up the port number at which the
	 * client is receiving messages.
	 * 
	 * @param The client's address.
	 * @return Returns the client's port number.
	 */
	public static int getPort(String name) {

		for (Map.Entry client : clients.entrySet()) {
			String n = (String) client.getKey();
			User newUser = (User) client.getValue();
			if (name.equals(n)) {
				return newUser.getPort();
			}
		}
		return 0;
	}

	/**
	 * Sends an ACK to the client notifying them of successful initial connection.
	 * 
	 * @param rcvdPacket Packet received from client.
	 */
	private static void sendConnectionAck(DatagramPacket rcvdPacket) {
		byte[] buffer = new byte[1];
		buffer[0] = Util.CONNECTION_ACK;
		InetAddress address = rcvdPacket.getAddress();
		int port = rcvdPacket.getPort();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
		sendDatagram(packet);
	}

	/**
	 * Sends datagram packet.
	 * 
	 * @param packet This is the datagram packet to send.
	 */
	private static void sendDatagram(DatagramPacket packet) {
		try {
			datagramSocket.send(packet);
		} catch (IOException e) {
			Util.printErrorAndExit("Could not send datagramPacket");
		}

	}

	/**
	 * Removes client from list and also removes the address of the client from the
	 * address list.
	 * 
	 * @param packet Packet sent from client indicating that they have left the
	 *               chat.
	 * @param buf    This is the buffer containing packet contents.
	 */
	private static void removeClient(DatagramPacket packet, byte[] buf) {

		String msgContents = "";
		for (int i = 1; i < buf.length; i++) {
			int charCode = buf[i];
			if (charCode == 0) {
				break;
			}
			char[] ch = Character.toChars(charCode);
			String s = new String(ch);
			msgContents = msgContents + s;
		}

		String[] msgContentsArray = msgContents.split(",");
		String sender = msgContentsArray[0];
		String messageText = "\n" + sender + " has left.";
		clients.remove(sender);
		String clientName = sender;
		broadcastUpdatedList();
		String update = clientName + " has left the chat.";
		gui.updateActivity(update);
	}

	/**
	 * Gets the client's user name, address and port number.
	 * 
	 * @param packet The datagram packet received from the client
	 * @param buf    A byte array with the packet's contents.
	 */
	private static void getClientInfo(DatagramPacket packet, byte[] buf) {
		String username = "";
		for (int i = 1; i < buf.length; i++) {
			int charCode = buf[i];
			if (charCode == 0) {
				break;
			}
			char[] ch = Character.toChars(charCode);
			String s = new String(ch);
			username = username + s;
		}

		if (clients.containsKey(username)) {
			sendConnectionFailedMsg(packet);
			return;
		}

		InetAddress clientInetAddress = packet.getAddress();
		int clientPort = packet.getPort();
		sendConnectionAck(packet);
		addClientToUserList(username, clientInetAddress, clientPort);

	}

	private static void sendConnectionFailedMsg(DatagramPacket rcvdPacket) {
		byte[] buffer = new byte[1];
		buffer[0] = Util.CONNECTION_FAILED;
		InetAddress address = rcvdPacket.getAddress();
		int port = rcvdPacket.getPort();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
		sendDatagram(packet);

	}

	/**
	 * Adds the client to the clients list (HashMap). Adds the clients address
	 * details to the address list.
	 * 
	 * @param clientName       The client's user name.
	 * @param clientInetAddres The client's IP address
	 * @param clientPort       The port being used by the client.
	 */
	static void addClientToUserList(String clientName, InetAddress clientInetAddres, int clientPort) {

		User newUser = new User(clientName, clientInetAddres, clientPort);
		clients.put(clientName, newUser);
		broadcastUpdatedList();
		String update = clientName + " has joined!";
		gui.updateActivity(update);
	}

	/**
	 * Makes a string with all connected users separated by comma and sends to
	 * connected clients. Client can recognize user list by checking if (first byte
	 * of packet == CLIENT_LIST constant). List is broadcasted whenever the user
	 * list is updated, i.e, when client joins or exits.
	 */
	public static void broadcastUpdatedList() {

		String allClients = "";
		ArrayList<String> clientsArray = new ArrayList<String>();
		int count = 0;
		for (Map.Entry client : clients.entrySet()) {
			clientsArray.add(client.getKey() + "");
			count++;
		}

		Collections.sort(clientsArray);
		for (String client : clientsArray) {
			allClients = allClients + client + ",";
		}

		if (count != 0) {
			allClients = allClients.substring(0, allClients.length() - 1);
		}

		gui.updateActiveList(allClients);

		byte[] buffer = new byte[Util.TRANS_BUFFER_SIZE];
		buffer[0] = (byte) Util.CLIENT_LIST;

		byte[] allClientsBuff = allClients.getBytes();

		for (int i = 0; i < allClientsBuff.length; i++) {
			buffer[i + 1] = allClientsBuff[i];
		}

		for (Map.Entry client : clients.entrySet()) {
			User newUser = (User) client.getValue();
			InetAddress address = (InetAddress) newUser.getIP();
			int port = (int) newUser.getPort();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
			sendDatagram(packet);
		}
	}
}
