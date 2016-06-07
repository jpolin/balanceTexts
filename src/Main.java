import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * @author Joe Polin
 *
 */
public class Main {
	
	static final boolean DEBUG = true;
	
	/**
	 * @param args:
	 * 		phone_exportdirectory (default: ../data/phone_files) // relative to bin dir
	 * 		carrier_export_directory (default: ../data/carrier_files) // relative to bin dir
	 * 		output_file_name (default: orphanEntries.csv) 
	 */
	public static void main(String[] args) {
		
		// Manual flags (mainly used for development)
		boolean useCachedOutputFiles = false;
		
		// Defaults (all paths relative to bin directory)
//		String phoneDirName = "../data/phone_test";
//		String carrierDirName = "../data/carrier_test";
		String phoneDirName = "../data/phone";
		String carrierDirName = "../data/carrier";
		String outputFileName = "orphanEntries.csv";
		String phoneAggregateName = "phone.csv";
		String carrierAggregateName = "carrier.csv";
		
		// Read args/enforce defaults
		if (args.length > 0)
			phoneDirName = args[0];
		if (args.length > 1)
			carrierDirName = args[1];
		if (args.length > 2)
			outputFileName = args[2];
		
		// Find and combine input files 
		File phoneFile, carrierFile;
		URI rootDir;
		try {
			// Generate paths
			rootDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			Path phoneDirPath = Paths.get(rootDir.resolve(phoneDirName));
			Path carrierDirPath = Paths.get(rootDir.resolve(carrierDirName));
			// Combine files
			phoneFile = Paths.get(rootDir.resolve(phoneAggregateName)).toFile();
			carrierFile = Paths.get(rootDir.resolve(carrierAggregateName)).toFile();
			// If we don't want to regenerate the output files
			if (!useCachedOutputFiles) {
				aggregateSimFiles(phoneDirPath, phoneFile);
				aggregateSimFiles(carrierDirPath, carrierFile);
			}
			
		} catch (URISyntaxException e) {
			System.out.println("Could not find specified folders: " + phoneDirName + " " + carrierDirName);
			return;
		} catch (IOException e){
			System.out.println("Could not combine files and generate aggregate files: " + 
					phoneAggregateName + " " + carrierAggregateName);
			return;
		}
		
		// Get lists of text records for all files we're interested in (2nd arg is name of class with custom parser)
		ArrayList<TextMessageRecord> phoneRecords = null;
		ArrayList<TextMessageRecord> carrierRecords = null;
		try {
			phoneRecords = TextLog.buildRecordList(phoneFile, "phone");
			carrierRecords = TextLog.buildRecordList(carrierFile, "carrier");
		} catch (IOException e) {
			System.out.println("Failed to parse files");
			e.printStackTrace();
			return;
		}
		
		// Reconcile the records
		ArrayList<TextMessageRecord> orphans;
		try {
			orphans = TextLog.compareTextLogs(phoneRecords, carrierRecords);
		}
		catch (IOException e){
			e.printStackTrace();
			return;
		}
		
		if (DEBUG){
			try {
				printTestLogs(phoneRecords, "bin/phoneRecordsDebug.csv");
				printTestLogs(carrierRecords, "bin/carrierRecordsDebug.csv");
			}
			catch (IOException e){
				System.out.println("Failed to write debug files");
				e.printStackTrace();
				// Continue anyways
			}
		}
		
		// Put orphans in file
		FileWriter writer;
		try {
			URI filePath = rootDir.resolve(outputFileName);
			writer = new FileWriter(filePath.getPath());
			for (TextMessageRecord rec : orphans)
				writer.write(rec.toString());
			writer.close();
			System.out.println("Wrote orphans to " + filePath.toString());
		}
		catch (IOException e){
			System.out.println("Could not write orphans to file " + outputFileName);
			e.printStackTrace();
			return;
		}
		
	}
	

	
	// Return the extension of a File. If the file has no extension, return an empty string
	public static String getFileExtension(File f){
		// Get file extension of output file
		String ext = "";
		String fname = f.getName();
		int extIndex = fname.toString().lastIndexOf(".");
		// Make sure we found an extension
		if (extIndex > 0)
			ext = fname.toString().substring(extIndex+1);
		return ext;
	}
	
	public static void printTestLogs(ArrayList<TextMessageRecord> log, String filename) throws IOException{
		FileWriter writer = new FileWriter(filename);
		for (TextMessageRecord tm : log)
			writer.write(tm.toString());
		writer.close();
	}
	
	// Combine all files in inputDir into outputFile (extension of outputFile must match all inputFiles
	// or they will be ignored)
	public static void aggregateSimFiles(Path inputDir, File outputFile) throws IOException{
		
		DirectoryStream<Path> dir = Files.newDirectoryStream(inputDir);
		FileWriter aggregate = new FileWriter(outputFile, false);		
		String desiredExtension = getFileExtension(outputFile);
		
		// Iterate over inputDir
		for (Path filePath : dir){
			// Make sure it has the right extension
			if (!desiredExtension.equals(getFileExtension(filePath.toFile())))
				continue;
			// Open file
			System.out.println("Collecting data from " + filePath.toString());
			FileReader inputFile = new FileReader(filePath.toFile());
			char buf[] = new char[1024];
			int charCount = inputFile.read(buf);
			// Write blocks to aggregate
			while (charCount != -1){
				aggregate.write(buf, 0, charCount);
				charCount = inputFile.read(buf);			}
			// Close input file and move onto next one
			inputFile.close();
		}
		aggregate.close();
		
	}

}


