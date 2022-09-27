import java.io.*;
import java.net.*;

/**
 * This class contains the code executed by a thread to download a file. Because
 * every download is done by a thread, it is possible to do more than one
 * download at time.
 */
public class Download extends Thread {
	InetAddress srcAddress;
	int srcPort;
	Socket downloadSocket;
	DataInputStream in = null;
	DataOutputStream out = null;
	String filename;
	long fileSize;
	boolean pause = false;
	boolean cancel = false;

	/**
	 * Constructor
	 * @param srcAddress The address of the file owner.
	 * @param srcPort    The port
	 * @param filename   The name of the file about to be downloaded.
	 */
	public Download(InetAddress srcAddress, int srcPort, String filename) {
		this.srcAddress = srcAddress;
		this.srcPort = srcPort;
		this.filename = filename;
	}

	public void run() {
		createSocket();

		setupIOStreams();

		receieveFileSize();

		receiveFile();

		closeSockets();
	}

	/**
	 * Creates a stream socket and connects it to the file owner.
	 */
	private void createSocket() {
		try {
			downloadSocket = new Socket(srcAddress, srcPort);
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Initializes input and output streams for the TCP socket.
	 */
	private void setupIOStreams() {
		try {
			in = new DataInputStream(downloadSocket.getInputStream());
			out = new DataOutputStream(downloadSocket.getOutputStream());
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 * Used to receive the size of the file about to be downloaded.
	 */
	private void receieveFileSize() {
		try {
			fileSize = in.readLong();
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}

	}

	/**
	 * Used to create download a file.
	 */
	private void receiveFile() {
		File file = new File(filename);
		byte[] contentsBuf = new byte[20000];
		int totalBytesRead = 0;
		int bytesRead = 0;

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			BufferedOutputStream bufferedOutSream = new BufferedOutputStream(fileOutputStream);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			DownloadFileGUI gui = new DownloadFileGUI();
			gui.setVisible(true);

			long startTime = System.currentTimeMillis();
			
			while ((bytesRead = in.read(contentsBuf)) != -1) {
				if (cancel) {
					gui.setVisible(false);
					cancelDownload(fileOutputStream);
					break;
				}

				if (pause) {
					// if pause, buffer
					byteArrayOutputStream.write(contentsBuf);
				}

				if (!pause && byteArrayOutputStream.size() > 0) {
					bufferedOutSream.write(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
					totalBytesRead = totalBytesRead + byteArrayOutputStream.size();
					byteArrayOutputStream.reset();
					contentsBuf = new byte[1000];
					int progress = (int) ((totalBytesRead / Double.valueOf(fileSize)) * 100);
					gui.updateProgress(progress, filename);
				}

				if (!pause && byteArrayOutputStream.size() == 0) {
					bufferedOutSream.write(contentsBuf, 0, bytesRead);
					totalBytesRead = totalBytesRead + bytesRead;
					int progress = (int) ((totalBytesRead / Double.valueOf(fileSize)) * 100);
					gui.updateProgress(progress, filename);
				}
			}
			bufferedOutSream.flush();
			bufferedOutSream.close();
			long endTime = System.currentTimeMillis();
			long diffTime = endTime - startTime;
			System.out.println("File size (bytes): " + fileSize);
			System.out.println("Time (ms): " + diffTime );
		} catch (Exception e) {}
	}

	/**
	 * Used to pause or resume download
	 */
	public void setPause(boolean setPause) {
		this.pause = setPause;
	}

	/**
	 * Used to cancel download.
	 * 
	 * @param setCancel
	 */
	public void setCancel(boolean setCancel) {
		this.cancel = setCancel;
	}

	public void cancelDownload(FileOutputStream fileOutputStream) {
		try {
			fileOutputStream.close();
			while (in.read() != -1) {
				// Do nothing
			}
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	private void closeSockets() {
		try {
			out.close();
			in.close();
			downloadSocket.close();
		} catch (IOException e) {
			Util.printErrorAndExit(e.toString());
		}
	}

	/**
	 *
	 * An inner class used to created a GUI which shows the progress of the file
	 * download.
	 * 
	 */
	@SuppressWarnings({ "serial" })
	public class DownloadFileGUI extends javax.swing.JFrame {

		/**
		 * Creates new form ViewFiles
		 */
		public DownloadFileGUI() {
			initComponents();
		}

		private void initComponents() {

			DownloadProgressBar = new javax.swing.JProgressBar(0, 100);
			jLabel1 = new javax.swing.JLabel();
			ResumeButton = new javax.swing.JButton();
			PauseButton = new javax.swing.JButton();
			CancelButton = new javax.swing.JButton();

			setTitle(Client.name + ": Download");
			jLabel1.setText("File Downloading ...");

			ResumeButton.setText("Resume");
			ResumeButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					ResumeButtonActionPerformed(evt);
				}
			});

			PauseButton.setText("Pause");
			PauseButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					PauseButtonActionPerformed(evt);
				}
			});

			CancelButton.setText("Cancel Download");
			CancelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					CancelButtonActionPerformed(evt);
				}
			});

			javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
			getContentPane().setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addGap(41, 41, 41)
							.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
									.addComponent(DownloadProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE,
											javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(jLabel1)
									.addGroup(layout.createSequentialGroup().addComponent(PauseButton)
											.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(ResumeButton)
											.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(CancelButton)))
							.addContainerGap(41, Short.MAX_VALUE)));
			layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addGap(77, 77, 77).addComponent(jLabel1).addGap(30, 30, 30)
							.addComponent(DownloadProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
									.addComponent(ResumeButton).addComponent(PauseButton).addComponent(CancelButton))
							.addContainerGap(116, Short.MAX_VALUE)));

			pack();
		}

		private void PauseButtonActionPerformed(java.awt.event.ActionEvent evt) {
			setPause(true);
		}

		private void ResumeButtonActionPerformed(java.awt.event.ActionEvent evt) {
			setPause(false);
		}

		private void CancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
			setCancel(true);
		}

		public void updateProgress(int value, String filename) {
			if (value >= 100) {
				jLabel1.setText(filename + " Downloaded successfully!");
				DownloadProgressBar.setValue(value);
			} else {
				jLabel1.setText("Downloading " + filename + "...");
				DownloadProgressBar.setValue(value);
			}
		}

		private javax.swing.JButton CancelButton;
		private javax.swing.JProgressBar DownloadProgressBar;
		private javax.swing.JButton PauseButton;
		private javax.swing.JButton ResumeButton;
		private javax.swing.JLabel jLabel1;
	}
}
