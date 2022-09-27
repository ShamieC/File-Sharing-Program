
/**
 * Contains constants and methods common to all classes.
 */
public class Util {
	public static final int SEND_BUFFER_SIZE = 40000;
	public static final int TRANS_BUFFER_SIZE = 50000;
	public static final int RCV_BUFFER_SIZE = 60000;
	public static final byte CONNECT = 1;
	public static final byte CONNECTION_ACK = 2;
	public static final byte CONNECTION_FAILED = 3;
	public static final byte CLIENT_LIST = 4;
	public static final byte SEND_MSG = 5;
	public static final byte RCV_MSG = 6;
	public static final byte EXIT = 7;
	public static final byte SEARCH = 8;
	public static final byte SEARCH_RESULT = 9;
	public static final byte ACK = 10;
	public static final byte DOWNLOAD_REQUEST = 11;
	public static final byte APPROVE_DOWNLOAD = 12;
	public static final byte START_DOWNLOAD = 13;

	/**
	 * Prints error messages and terminates program. Usually invoked in catch
	 * statements.
	 * 
	 * @param errorMsg The error message.
	 */
	public static void printErrorAndExit(String errorMsg) {
		System.err.println(errorMsg);
		System.exit(-1);
	}
}
