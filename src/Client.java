
import java.io.*;
import java.net.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Base64;

public class Client {

	static String name;
	public static DatagramSocket udpSocket;
	static int serverUdpPort = 4445;
	static String serverIP = "127.0.0.1";
	static InetAddress serverAddress;
	public static int RCV_BUFFER_SIZE = 60000;
	public static String sharedFolder;
	static ClientGUI gui = null;
	static ChatRoomGUI chatgui = null;
	static String generatedKey = "";
	static String generatedEncryptedKey = "";
	static PublicKey publicKey;
	static PrivateKey privateKey;
	static String list = "";
	static String userlist = "";
	static String sendMessgeText = "";
	static String messageText = "";
	static String messages = "";

	public static void main(String[] args) {
		if (args.length < 1) {
			Util.printErrorAndExit("Usage: Client <username>");
		}

		name = args[0];

		ShutDownHook.serverIP = serverIP;
		ShutDownHook.serverPort = serverUdpPort;
		Runtime current = Runtime.getRuntime();
		current.addShutdownHook(new ShutDownHook());

		createSockets();

		connectToServer();

		gui = new ClientGUI(name);
		gui.setVisible(true);

		while (true) {
			listenForUdpPacket();
		}
	}

	/**
	 * Method runs infinitely waiting waiting to receive UDP packets.
	 */
	private static void listenForUdpPacket() {
		byte[] buf = new byte[Util.RCV_BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		try {
			udpSocket.receive(packet);
		} catch (IOException e) {
			Util.printErrorAndExit("Could not receive datagramPacket");
		}

		byte firstbyte = buf[0];
		if (firstbyte == Util.SEARCH) {
			searchForFileOnComputer(packet, buf);
		} else if (firstbyte == Util.SEARCH_RESULT) {
			displaySearchResults(packet, buf);
		} else if (firstbyte == Util.DOWNLOAD_REQUEST) {
			acceptDownloadRequest(packet, buf);
		} else if (firstbyte == Util.START_DOWNLOAD) {
			startDownload(packet, buf);
		} else if (firstbyte == Util.CLIENT_LIST) {
			String[] users = getClientList(buf);
			printUserList(users);
		} else if (firstbyte == Util.RCV_MSG) {
			receiveMsg(buf);
		}
	}

	/**
	 * Gets the INET address of the server and creates UDP sockets;
	 */
	private static void createSockets() {
		try {
			serverAddress = InetAddress.getByName(serverIP);
		} catch (UnknownHostException u) {
			Util.printErrorAndExit("IP address of host could not be determined.");
		}
		try {
			udpSocket = new DatagramSocket();
		} catch (SocketException e1) {
			Util.printErrorAndExit("Could not create or access socket.");
		}
	}

	/**
	 * Connects and sends the client's user name to the server.
	 */
	private static void connectToServer() {
		boolean registrationSuccessful = false;

		while (!registrationSuccessful) {
			byte[] nameBuf = name.getBytes();
			byte[] messageBuf = new byte[nameBuf.length + 1];

			messageBuf[0] = Util.CONNECT;
			for (int i = 0; i < nameBuf.length; i++) {
				messageBuf[i + 1] = nameBuf[i];
			}

			sendPacket(messageBuf, serverAddress, serverUdpPort);

			byte[] buf = new byte[RCV_BUFFER_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddress, serverUdpPort);
			receivePacket(packet);

			byte firstbyte = buf[0];

			if (firstbyte == Util.CONNECTION_ACK) {
				System.out.println("Connected to server!");
				registrationSuccessful = true;
			} else if (firstbyte == Util.CONNECTION_FAILED) {
				getNewUsername();
			}
		}
	}

	/**
	 * Used to request a new user name from client.
	 */
	private static void getNewUsername() {
		System.out.println("Username already taken. Please choose a different name:");
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(input);
		try {
			name = br.readLine().trim();
			if (name.isEmpty())
				getNewUsername();
		} catch (IOException e) {
			Util.printErrorAndExit("Could not get username.");
		}
	}

	/**
	 * Used to notify the server of the exit.
	 */
	public static void sendExit() {
		String msg = name + ",";
		byte[] buf = new byte[Util.SEND_BUFFER_SIZE];
		buf[0] = (byte) Util.EXIT;
		byte[] msgBuff = msg.getBytes();
		for (int i = 0; i < msgBuff.length; i++) {
			buf[i + 1] = msgBuff[i];
		}

		DatagramPacket msgPacket = new DatagramPacket(buf, buf.length, serverAddress, serverUdpPort);
		try {
			udpSocket.send(msgPacket);
		} catch (IOException e) {
		}
	}

	/**
	 * Invoked when a sender wants to send a message. The method concatenates the
	 * recipient's name and the message into one string and then sends to server.
	 * 
	 * @param recipient The recipient of the message.
	 * @param message   The message to be sent.
	 */
	public static void sendMessage(String recipient, String message) {
		if (message.length() == 0)
			return;
		String msg = name + "," + recipient + "," + message;
		messageText = "You: " + message;
		messages += messageText + "\n";

		byte[] buf = new byte[Util.SEND_BUFFER_SIZE];
		buf[0] = (byte) Util.SEND_MSG;
		byte[] msgBuff = msg.getBytes();
		for (int i = 0; i < msgBuff.length; i++) {
			buf[i + 1] = msgBuff[i];
		}

		DatagramPacket msgPacket = new DatagramPacket(buf, buf.length, serverAddress, serverUdpPort);
		sendTextDatagrams(msgPacket);
	}

	/**
	 * Extracts the sender's user name and the text message from the buffer.
	 * 
	 * @param buf The buffer containing the message packet contents.
	 */
	private static void receiveMsg(byte[] buf) {
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
		String msg = msgContentsArray[1];
		messageText = sender + ": " + msg;
		messages += messageText + "\n";

		if(chatgui != null) {
			chatgui.updateMessages(messages);
		}

	}

	/**
	 * Sends text datagram packets and updates GUI.
	 * 
	 * @param packet The packet to be sent.
	 */
	private static void sendTextDatagrams(DatagramPacket packet) {
		try {
			udpSocket.send(packet);
			if(chatgui != null) {
				chatgui.updateMessages(messages);
			}
		} catch (IOException e) {
			Util.printErrorAndExit("Could not send datagramPacket.");
		}
	}

	/**
	 * Sends UDP packets.
	 * 
	 * @param buf     The data to be sent.
	 * @param address Destination IP address.
	 * @param port    Destination port.
	 */
	public static void sendPacket(byte[] buf, InetAddress address, int port) {
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		try {
			udpSocket.send(packet);
		} catch (IOException e) {
			Util.printErrorAndExit("Could not send datagram packet.");
		}
	}

	/**
	 * Used to receive UDP Packets.
	 * 
	 * @param packet The packet to used to receive.
	 */
	private static void receivePacket(DatagramPacket packet) {
		try {
			udpSocket.receive(packet);
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Sends filename the user is searching for to the server.
	 * 
	 * @param searchTerm The file being searched for.
	 */
	public static void sendSearchTerm(String searchTerm) {
		if (searchTerm.isEmpty()) {
			return;
		}
		byte[] wordBuf = (name + ";" + searchTerm + ";").getBytes();
		byte[] messageBuf = new byte[wordBuf.length + 1];
		messageBuf[0] = Util.SEARCH;

		for (int i = 0; i < wordBuf.length; i++) {
			messageBuf[i + 1] = wordBuf[i];
		}

		sendPacket(messageBuf, serverAddress, serverUdpPort);
	}

	/**
	 * Searches for the specified file on the user's computer.
	 * 
	 * @param packet The packet containing the search request message.
	 */
	private static void searchForFileOnComputer(DatagramPacket packet, byte[] buf) {
		String searchMsg = getStringMessage(packet, buf).trim();
		String searchTerm = null;
		searchTerm = searchMsg.split(";")[1];

		Find find = new Find(sharedFolder, searchTerm);
		find.find();

		String resultsMsg = searchMsg + name + ";" + find.getSearchResults() + ";";
		find.clearSearchResults();

		byte[] resultsMsgBuf = resultsMsg.getBytes();
		byte[] messageBuf = new byte[resultsMsgBuf.length + 1];
		messageBuf[0] = Util.SEARCH_RESULT;

		for (int i = 0; i < resultsMsgBuf.length; i++) {
			messageBuf[i + 1] = resultsMsgBuf[i];
		}

		sendPacket(messageBuf, packet.getAddress(), packet.getPort());
	}

	/**
	 * Extracts the search result from the receive packet and sends to GUI.
	 * 
	 * @param packet
	 * @param buf
	 */
	private static void displaySearchResults(DatagramPacket packet, byte[] buf) {
		String msg = getStringMessage(packet, buf).trim();

		byte[] ack = new byte[1];
		ack[0] = Util.ACK;

		sendPacket(ack, packet.getAddress(), packet.getPort());

		String[] results = msg.split(";");

		list = "-- Select a file to download --,";
		for (int i = 0; i < results.length; i++) {
			String[] result = results[i].split(",");
			String sender = result[result.length - 1];
			if (result.length > 1) {
				for (int j = 0; j < result.length - 1; j++) {
					list = list + result[j] + "-" + sender + ",";
				}
			}
		}
		gui.updateFileList(list);
	}

	/**
	 * Sends a download request to the server with an randomly generated encrypted
	 * key.
	 * 
	 * @param filename The file being requested.
	 * @param owner    The name of the file owner
	 */
	public static void requestDownload(String filename, String owner) {
		String key = generateRandomKey();
		try {
			String encryptedKey = encryptKey(key);
			String msg = owner + "," + filename + "," + encryptedKey;
			byte[] buf = msg.getBytes();

			byte[] messageBuf = new byte[buf.length + 1];

			messageBuf[0] = Util.DOWNLOAD_REQUEST;
			for (int i = 0; i < buf.length; i++) {
				messageBuf[i + 1] = buf[i];
			}

			sendPacket(messageBuf, serverAddress, serverUdpPort);

		} catch (Exception e) {
			
		}
	}

	private static String encryptKey(String key) throws NoSuchAlgorithmException,
		NoSuchPaddingException, InvalidKeyException, 
		IllegalBlockSizeException, BadPaddingException {

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		SecureRandom secureRandom = new SecureRandom();
			 
		keyPairGenerator.initialize(2048,secureRandom);
		KeyPair pair = keyPairGenerator.generateKeyPair();
			 
		publicKey = pair.getPublic();

		privateKey = pair.getPrivate();
			 
		//Encrypt message
		Cipher encryptionCipher = Cipher.getInstance("RSA");
		encryptionCipher.init(Cipher.ENCRYPT_MODE,privateKey);
		String message = key;
		byte[] encryptedMessage = encryptionCipher.doFinal(message.getBytes());
		String encryption = Base64.getEncoder().encodeToString(encryptedMessage);
		generatedEncryptedKey = encryption;
		return encryption;
	}

	private static String generateRandomKey() {
		// chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                    + "0123456789"
                                    + "abcdefghijklmnopqrstuvxyz";
		int n = 6;
        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);
  
        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index = (int)(AlphaNumericString.length() * Math.random());
            // add Character one by one in end of sb
            sb.append(AlphaNumericString.charAt(index));
        }
  
        generatedKey = sb.toString();
		return generatedKey;
	}

	/**
	 * Accepts download request starts upload if keys match.
	 * 
	 * @param packet
	 * @param buf
	 */

	public static void acceptDownloadRequest(DatagramPacket packet, byte[] buf) {
		String msg = null;
		String[] info = null;
		InetAddress requestorAddress = null;

		try {
			msg = new String(buf, 1, packet.getLength(), "UTF-8");
			info = msg.split(",");
		} catch (UnsupportedEncodingException e) {
			Util.printErrorAndExit(e.toString());
		}

		try {
			requestorAddress = InetAddress.getByName(info[1].trim().substring(1));
		} catch (UnknownHostException e) {
			Util.printErrorAndExit(e.toString());
		}


		int requestorPort = Integer.parseInt(info[2]);

		String requestedFile = info[4];

		String encrpytedKey = info[5];

		try {
			// Contact originating client with tcp socket
			Upload flileUpload = new Upload(requestorAddress, requestorPort, requestedFile, encrpytedKey);
			flileUpload.start();

		} catch (Exception e) {}
	}

	/**
	 * Method used to start a download.
	 * 
	 * @param packet
	 * @param buf
	 */
	private static void startDownload(DatagramPacket packet, byte[] buf) {
		String msg = getStringMessage(packet, buf).trim();

		int port = Integer.parseInt(msg.split(",")[0].trim());
		String filename = msg.split(",")[1].trim();
		String key = msg.split(",")[2].trim();
		InetAddress srcAddress = packet.getAddress();

		if(key.equals(generatedEncryptedKey)) {
			Download download = new Download(srcAddress, port, filename);
			download.start();
		} else {
			System.out.println("key did not match");
		}
	}

	/**
	 * Prints user list to client on GUI.
	 * 
	 * @param users an array with the names of active users
	 */
	private static void printUserList(String[] users) {
		userlist = "";
		if(users.length != 0) {

			for (String user : users) {
				userlist += user + ",";
			}
			if (!userlist.equals("")) {
				userlist = userlist.substring(0, userlist.length() - 1);
			}
			gui.updateOnlineList(userlist);
			if(chatgui != null){
				chatgui.updateOnlineList(userlist);
			}
		}
	}

	/**
	 * Extracts client list from buffer contents.
	 * 
	 * @param buf Buffer containing user list.
	 * @return Returns an array of user names.
	 */
	private static String[] getClientList(byte[] buf) {
		byte[] buffs = new byte[buf.length - 1];
		for (int i = 0; i < buf.length - 1; i++) {
			buffs[i] = buf[i + 1];
		}
		String strList = new String(buf);
		String[] users = strList.split(",");
		return users;
	}

	public static void OpenChatRoom() {
		chatgui = new ChatRoomGUI(name);
		chatgui.setVisible(true);
		chatgui.updateOnlineList(userlist);
		chatgui.updateMessages(messages);
	}


	/**
	 * Used to extract a string message from a received UDP packet.
	 * 
	 * @param packet
	 * @param buf
	 * @return returns the received message.
	 */
	public static String getStringMessage(DatagramPacket packet, byte[] buf) {
		String msg = "";
		try {
			msg = new String(buf, 1, packet.getLength(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Util.printErrorAndExit(e.toString());
		}
		return msg;
	}

}
