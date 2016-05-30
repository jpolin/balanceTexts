import java.io.*;
import java.net.*;
import java.nio.file.*;


/**
 * @author Joe Polin
 *
 */
public class main {
	
	/**
	 * @param args:
	 * 		phone_exportdirectory (default: ../data/phone) // relative to bin dir
	 * 		carrier_export_directory (default: ../data/carrier) // relative to bin dir
	 */
	public static void main(String[] args) {
		
		// Defaults (all paths relative to bin directory)
		String phoneDirName = "../data/phone";
		String carrierDirName = "../data/carrier";
		String phoneAggregateName = "phone.csv";
		String carrierAggregateName = "carrier.csv";
		
		// Read args
		if (args.length > 0)
			phoneDirName = args[0];
		if (args.length > 1)
			carrierDirName = args[1];
		
		// Combine input files
		try {
			// Generate paths
			URI rootDir = main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			Path phoneDirPath = Paths.get(rootDir.resolve(phoneDirName));
			Path carrierDirPath = Paths.get(rootDir.resolve(carrierDirName));
			// Combine files
			aggregateSimFiles(phoneDirPath, Paths.get(rootDir.resolve(phoneAggregateName)).toFile());
			aggregateSimFiles(carrierDirPath, Paths.get(rootDir.resolve(carrierAggregateName)).toFile());
			
		} catch (URISyntaxException e) {
			System.out.println("Could not find specified folders: " + phoneDirName + " " + carrierDirName);
			return;
		} catch (IOException e){
			System.out.println("Could not combine files and generate aggregate files: " + 
					phoneAggregateName + " " + carrierAggregateName);
			return;
		}
		
		// Reconcile these two aggregate files
//		findDiscrepancies();
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
			FileReader inputFile = new FileReader(filePath.toFile());
			char buf[] = new char[1024];
			int charCount = inputFile.read(buf);
			// Write blocks to aggregate
			while (charCount != -1){
				aggregate.write(buf, 0, charCount);
				charCount = inputFile.read(buf)
;			}
			// Close input file and move onto next one
			inputFile.close();
		}
		aggregate.close();
		
	}

}
