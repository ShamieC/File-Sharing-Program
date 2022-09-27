import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * To allow concurrent multiple searches, the server dispatches a thread for
 * every search. This class contains code executed by a thread during the
 * search. The thread broadcasts a search query to all clients and sends back
 * search results to the searcher only after all the connected clients have
 * replied.
 *
 */
public class ServerSearchThread extends Thread {
	HashMap<String, String> results = new HashMap<String, String>();
	DatagramSocket serverSearchSocket;
	InetAddress searcherAddress;
	DatagramPacket searchPacket;
	String searchTerm;
	int searcherPort;
	String searcher;
	long startTime;
	byte[] buf;
	int timeout = 0;

	/**
	 * Constructor
	 * 
	 * @param packet The packet received from the searcher.
	 * @param buf    A buffer with the packet contents.
	 */
	ServerSearchThread(DatagramPacket packet, byte[] buf) {
		this.searcher = Server.getUsername(packet);
		this.searcherAddress = Server.getAddress(searcher);
		this.searcherPort = Server.getPort(searcher);
		this.searchPacket = packet;
		this.buf = buf;

		try {
			String searchMsg = new String(buf, 1, packet.getLength(), "UTF-8");
			searchTerm = searchMsg.split(";")[1];
		} catch (UnsupportedEncodingException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	public void run() {

		createSocket();

		broadcastSearchQuery();

		while (true) {
			receiveResults();

			if (searchIsComplete()) {
				sendSearchResults(combineList());
				break;
			}

			if (overtime() && !results.isEmpty()) {
				sendSearchResults(combineList());
				break;
			}

			if (overtime()) {
				broadcastSearchQuery();
			}

			if (overtime() || timeout == 30) {
				break;
			}

		}
	}

	/**
	 * Sends the search results to the searcher and waits to receive an ACK.
	 * 
	 * @param list The list with search results.
	 */
	private void sendSearchResults(String list) {

		while (true) {
			byte[] listBuf = list.getBytes();

			byte[] resultsBuf = new byte[1 + listBuf.length];
			resultsBuf[0] = Util.SEARCH_RESULT;
			for (int i = 0; i < listBuf.length; i++) {
				resultsBuf[i + 1] = listBuf[i];
			}

			DatagramPacket packet = new DatagramPacket(resultsBuf, resultsBuf.length, searcherAddress, searcherPort);
			sendDatagram(packet);

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				Util.printErrorAndExit(e.toString());
			}

			byte[] buffer = new byte[Util.RCV_BUFFER_SIZE];
			DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
			try {
				serverSearchSocket.receive(ackPacket);
				if (buffer[0] == Util.ACK) {
					break;
				}
			} catch (IOException e) {
				Util.printErrorAndExit("Could not receive searchResult");
			}
		}
	}

	/**
	 * This method is used to check if the server has waited for too long to receive
	 * results from all clients.
	 * 
	 * @return true to indicate timeout else returns false.
	 */
	private boolean overtime() {
		long currentTime = System.currentTimeMillis();

		if (currentTime - startTime > 2000) {
			timeout++;
			return true;
		}
		return false;
	}

	/**
	 * Method used to receive search results packets from clients.
	 */
	private void receiveResults() {
		byte[] rcvBuf = new byte[Util.RCV_BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length);
		try {
			serverSearchSocket.setSoTimeout(20);
			serverSearchSocket.receive(packet);
		} catch (SocketException se) {

		} catch (IOException e) {
		}

		byte firstbyte = rcvBuf[0];
		if (firstbyte == Util.SEARCH_RESULT) {
			getSearchResult(packet, rcvBuf);
		}

	}

	/**
	 * Used to extract the result list from the packet received from clients.
	 * 
	 * @param packet Packet received from a client.
	 * @param buf2   A byte array with the packet's contents
	 */
	private void getSearchResult(DatagramPacket packet, byte[] buf2) {
		String searchedTerm = null;
		String searcher = null;
		String fileOwner = null;
		String fileList = null;

		try {
			String searchResultMsg = new String(buf2, 1, packet.getLength(), "UTF-8");
			String[] resultInfo = searchResultMsg.split(";");
			searcher = resultInfo[0];
			searchedTerm = resultInfo[1];
			fileOwner = resultInfo[2];
			fileList = resultInfo[3];
		} catch (UnsupportedEncodingException e) {
			Util.printErrorAndExit(e.toString());
		}

		if (this.searchTerm.equals(searchedTerm) && this.searcher.equals(searcher)) {
			results.putIfAbsent(fileOwner, fileList);
		}

	}

	/**
	 * Creates a UDP socket to receive and send packets relating to the search.
	 */
	private void createSocket() {
		try {
			serverSearchSocket = new DatagramSocket();
		} catch (SocketException e1) {
			Util.printErrorAndExit("Could not create socket server search socket.");
		}
	}

	@SuppressWarnings("rawtypes")
	/**
	 * Sends a search query to all clients other than the searcher.
	 */
	private void broadcastSearchQuery() {

		for (Map.Entry client : Server.clients.entrySet()) {
			String name = (String) client.getKey();

			if (!name.equals(searcher) && !results.containsKey(name)) {
				User newUser = (User) client.getValue();
				InetAddress address = (InetAddress) newUser.getIP();
				int port = (int) newUser.getPort();
				sendDatagram(new DatagramPacket(buf, buf.length, address, port));
			}
		}

		startTime = System.currentTimeMillis();

	}

	/**
	 * Combines a list of search results to be sent to the searcher.
	 * 
	 * @return a list of search results.
	 */
	private String combineList() {
		String list = "";
		for (Map.Entry results : results.entrySet()) {
			String owner = (String) results.getKey();
			owner = owner.trim();
			String file = (String) results.getValue();
			file = file.trim();
			String item = file + owner + ";";
			list = list + item;
		}
		return list;
	}

	/**
	 * Returns true if search is complete. A search is complete when all the
	 * connected clients have sent back their replies to the search query.
	 * 
	 * @return Returns true if search is complete.
	 */
	private boolean searchIsComplete() {
		if (results.size() == (Server.clients.size() - 1)) {
			return true;
		}
		return false;
	}

	/**
	 * Method used to send UDP packets.
	 * 
	 * @param packet the packet to be sent.
	 */
	private void sendDatagram(DatagramPacket packet) {
		try {
			serverSearchSocket.send(packet);
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

}
