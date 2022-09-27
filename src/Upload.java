import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class contains the code executed by a thread to upload a file. Because
 * every upload is done by a thread, it is possible to do more than one upload
 * at time.
 */
public class Upload extends Thread {
	DataInputStream in = null;
	DataOutputStream out = null;
	ServerSocket serverSocket;
	InetAddress toAddress;
	Socket uploadSocket;
	String filename;
	String encrpytedKey;
	int uploadPort;
	int toPort;

	/**
	 * The class's constructor
	 * 
	 * @param requestorAddress The name of the person who requested the file.
	 * @param requestorPort    The IP address of the person who requested the file.
	 * @param requestedFile    The requested file.
	 */
	public Upload(InetAddress requestorAddress, int requestorPort, String requestedFile, String encrypted) {
		toAddress = requestorAddress;
		toPort = requestorPort;
		filename = requestedFile;
		encrpytedKey = encrypted;
	}

	public void run() {
		createServerSocket();

		sendTcpPortNumber();

		waitForConnection();

		setupIOStreams();

		Path path = getFilePath();

		sendFile(path);

		closeSockets();
	}

	/**
	 * The method looks for an available TCP port on the system and creates a
	 * socket.
	 */
	private void createServerSocket() {
		int port = 49152;
		int lastport = 65535;

		while (port < lastport) {
			try {
				serverSocket = new ServerSocket(port);
				uploadPort = port;
				break;
			} catch (IOException e) {
			}
			port++;
		}
	}

	/**
	 * Sends the port number of the established TCP socket to the user who requested
	 * a file download.
	 */
	private void sendTcpPortNumber() {
		String msg = uploadPort + "," + filename + "," + encrpytedKey;
		byte[] msgBuf = msg.getBytes();
		byte[] buf = new byte[1 + msgBuf.length];

		buf[0] = Util.START_DOWNLOAD;
		for (int i = 0; i < msgBuf.length; i++) {
			buf[1 + i] = msgBuf[i];
		}
		Client.sendPacket(buf, toAddress, toPort);
	}

	/**
	 * Waits for the user who requested the file to connect to the TCP stream.
	 */
	private void waitForConnection() {
		try {
			uploadSocket = serverSocket.accept();
			// System.out.println("Connected and ready to upload");
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Sets up input and output streams to be able to send and receive data on the
	 * TCP socket.
	 */
	private void setupIOStreams() {
		try {
			in = new DataInputStream(uploadSocket.getInputStream());
			out = new DataOutputStream(uploadSocket.getOutputStream());
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Method handles the sending of the file to user who requested the file. This
	 * method also creates a GUI which shows the progress of the file upload.
	 * 
	 * @param path The absolute path to the requested file.
	 */
	private void sendFile(Path path) {

		File file = new File(path.toString());
		long fileSize = file.length();
		sendFileSize(fileSize);

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			BufferedInputStream bufInputStream = new BufferedInputStream(fileInputStream);

			UploadGUI gui = new UploadGUI();
			gui.setVisible(true);

			long sentBytes = 0;
			long segmentSize = 20000;
			while (sentBytes != fileSize) {

				if (fileSize - sentBytes <= segmentSize) {
					segmentSize = fileSize - sentBytes;
				}

				byte[] buf = new byte[(int) segmentSize];
				bufInputStream.read(buf, 0, buf.length);

				out.write(buf);

				sentBytes = buf.length + sentBytes;
				int progress = (int) ((sentBytes / Double.valueOf(fileSize)) * 100);

				gui.updateProgress(progress, filename);
			}
			bufInputStream.close();
		} catch (IOException e) {}

	}



	/**
	 * Before the file is sent. This method is used to send the size of the the file
	 * to be uploaded to the user who requested the file.
	 * 
	 * @param fileSize The length of the file to uploaded.
	 */
	private void sendFileSize(long fileSize) {
		try {
			out.writeLong(fileSize);
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Searches for the requested file on computer and gets its path
	 * 
	 * @return Returns absolute path of the requested file if found.
	 */
	private Path getFilePath() {
		Find find = new Find(Client.sharedFolder, filename);
		find.find();

		ArrayList<Path> filePaths = find.getfilePaths();

		for (Path path : filePaths) {
			if (path.toString().contains(filename)) {
				return path;
			}
		}
		return null;
	}

	private void closeSockets() {
		try {
			out.close();
			in.close();
			uploadSocket.close();
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

}
