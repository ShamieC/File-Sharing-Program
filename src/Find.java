
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.ArrayList;

import static java.nio.file.FileVisitResult.*;

/**
 * Code that looks for searched files in the shared folder and its
 * sub-directories. It returns both exact and close matches. Close matches are
 * determined by a fuzzy ratio which is calculated using the Levenshtein
 * distance between the search term and the files in the shared folder.
 *
 */
public class Find {

	static ArrayList<String> searchResults = new ArrayList<String>();
	static ArrayList<Path> filePaths = new ArrayList<Path>();
	static String searchTerm;
	static String dir;

	/**
	 * Constructor
	 * 
	 * @param dir        The directory in which to search files.
	 * @param searchTerm The name of file to be searched for.
	 */
	Find(String dir, String searchTerm) {
		Find.searchTerm = searchTerm;
		Find.dir = dir;
	}

	public void find() {
		Path startingDir = Paths.get(dir);
		Finder finder = new Finder();

		try {
			Files.walkFileTree(startingDir, finder);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(-1);
		}
	}

	/**
	 * Returns a list of the files matching the requested file.
	 * 
	 * @return
	 */
	public String getSearchResults() {
		String results = "";

		for (String file : searchResults) {
			results = results + file + ",";
		}

		return results;
	}

	/**
	 * Clears the list of results
	 */
	public void clearSearchResults() {
		searchResults.clear();
	}

	/**
	 * Returns the absolute paths of files matching the requested file.
	 * 
	 * @return Returns an array list with absolute file paths.
	 */
	public ArrayList<Path> getfilePaths() {
		return filePaths;
	}

	/**
	 * Deletes all stored the file paths.
	 */
	public void clearfilePaths() {
		filePaths.clear();
	}

	/**
	 * Inner class used to walk down the system's file system starting from a
	 * specified directory.
	 *
	 */
	public static class Finder extends SimpleFileVisitor<Path> {

		Finder() {
		}

		void find(Path file) {
			Path name = file.getFileName();
			filePaths.add(file.toAbsolutePath());
			String filename = removePath(name);

			if (isCloseMatch(filename)) {
				searchResults.add(filename);
			}
		}

		/**
		 * Uses the fuzzy ratio to determine whether a search result is similar to the
		 * searched file.
		 * 
		 * @param fname The result to be compared to the search term.
		 * @return Returns true if the search result is a close match.
		 */
		private boolean isCloseMatch(String fname) {
			int extensionPos = 0;
			if (fname.contains(".")) {
				extensionPos = fname.indexOf(".");
				fname = fname.substring(0, extensionPos);
			}
			int levDest = LevenshteinDistance(fname);
			double wordSum = fname.length() + searchTerm.length();
			double fuzzyRatio = ((wordSum - levDest) / wordSum) * 100;

			if (fuzzyRatio >= 75 || wordsHaveCommonSequnce(fname)) {
				return true;
			}
			return false;
		}

		private boolean wordsHaveCommonSequnce(String fname) {
			if (fname.toLowerCase().contains(searchTerm.toLowerCase())
					|| searchTerm.toLowerCase().contains(fname.toLowerCase())) {
				return true;
			}

			return false;
		}

		/**
		 * Removes the part containing the absolute path from the search result.
		 * 
		 * @param filepath The path.
		 * @return The file name with extension but with no path.
		 */
		private String removePath(Path filepath) {
			String filename = "";

			// To cater for both Windows and Unix systems.
			if (filepath.toString().contains("\\")) {
				int endOfPath = filepath.toString().lastIndexOf("\\");
				filename = filepath.toString().substring(endOfPath + 1);
			} else {
				int endOfPath = filepath.toString().lastIndexOf("/");
				filename = filepath.toString().substring(endOfPath + 1);
			}
			return filename;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			find(file);
			return CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			// find(dir);
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			System.err.println(exc);
			return CONTINUE;
		}
	}

	/**
	 * Computes the Levenshtein Distance between a search results and the search
	 * term. The Levenshtein distance between two words is the minimum number of
	 * single-character edits (insertions, deletions or substitutions) required to
	 * change one word into the other - Wikipedia.
	 * 
	 * @param filename The filename.
	 * @return The Levenshtein Distance between the search result and the search
	 *         term.
	 */
	static int LevenshteinDistance(String filename) {
		String source;
		String target;

		if (searchTerm.length() > filename.length()) {
			target = searchTerm.toLowerCase();
			source = filename.toLowerCase();
		} else {
			target = filename.toLowerCase();
			source = searchTerm.toLowerCase();
		}

		int[][] dist = new int[source.length() + 1][target.length() + 1];

		for (int i = 0; i < dist.length; i++) {
			for (int j = 0; j < dist[i].length; j++) {
				if (i == 0) {
					dist[i][j] = j;
				} else if (j == 0) {
					dist[i][j] = i;
				} else {
					int cost;
					if (source.charAt(i - 1) == target.charAt(j - 1)) {
						cost = 0;
					} else {
						cost = 1;
					}
					int deletion = dist[i - 1][j] + 1;
					int insertion = dist[i][j - 1] + 1;
					int substitution = dist[i - 1][j - 1] + cost;
					dist[i][j] = Math.min(deletion, Math.min(insertion, substitution));
				}
			}
		}
		return dist[dist.length - 1][dist[0].length - 1];
	}
}
