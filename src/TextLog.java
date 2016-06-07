import java.io.*;
import java.util.*;

public class TextLog {

	static final boolean DEBUG = true;
	
	// Build list of all records for this file and type
	public static ArrayList<TextMessageRecord> buildRecordList(File aggregatedFile, String recordType) throws IOException{
		
		// Open reader for file
		BufferedReader reader = new BufferedReader(new FileReader(aggregatedFile.toString()));
		
		// Iterate over file and build array
		String line = reader.readLine();
		ArrayList<TextMessageRecord> textRecs = new ArrayList<TextMessageRecord>();
		while (line != null){
			
			TextMessageRecord textRec = getTextRecordByType(recordType, line);
			
			// Get next line
			line = reader.readLine();
			
			if (!textRec.successfulParse) 
				continue;
			
			// If it doesn't need to be split, add it as is. Otherwise, split
			if (textRec.getNumTextsContainted() == 1)
				textRecs.add(textRec);
			else {
				ArrayList<TextMessageRecord> split = splitMessage(textRec);
				textRecs.addAll(split);
			}
							
		}
		
		System.out.println("Parsed " + textRecs.size() + " records for " + recordType);
		
		reader.close();
		return textRecs;
	}
	
	// Given a string describing the type of the message record, generate that type
	public static TextMessageRecord getTextRecordByType(String recordType, String info) throws IOException{
		TextMessageRecord textRec;
		if (recordType.equals("carrier"))
			textRec = new CarrierRecord(info);
		else if (recordType.equals("phone"))
			textRec = new PhoneRecord(info);			
		else {
			throw new IOException("Did not recognize type " + recordType);
		}
		return textRec;
	}
	
	// Compare two ArrayLists (assume both in chronological order) and return ArrayList of orphans
	public static ArrayList<TextMessageRecord> compareTextLogs(ArrayList<TextMessageRecord> log1, ArrayList<TextMessageRecord> log2) throws IOException{
		
		FileWriter writer;
		String empty = " , , , , , ";
		if (DEBUG) {
			try {
				writer = new FileWriter("bin/dynamicDiff.csv");
			}
			catch (IOException e){
				e.printStackTrace();
				return null;
			}
		}
		
		ArrayList<TextMessageRecord> orphans = new ArrayList<TextMessageRecord>();
		int orphanCount1 = 0;
		int orphanCount2 = 0;
				
		ListIterator<TextMessageRecord> it1 = log1.listIterator();
		ListIterator<TextMessageRecord> it2 = log2.listIterator();
		while (it1.hasNext() || it2.hasNext()){
			
			// If one has reached end, add other to orphan list
			if (!it1.hasNext()) {
				orphans.add(it2.next());
				orphanCount2++;
				continue;
			}
			else if (!it2.hasNext()) {
				orphans.add(it1.next());
				orphanCount1++;
				continue;
			}
			
			// Check if the messages are the same
			TextMessageRecord tm1 = it1.next();
			TextMessageRecord tm2 = it2.next(); 
			if (tm1.equals(tm2)) {
				if (DEBUG) writer.write(tm1.toString().replace('\n', ' ') + tm2.toString());
				continue;
			}
			// If not, add whoever is earlier to orphan list and back up other
			if (tm1.getTime().before(tm2.getTime())) {
				orphans.add(tm1);
				if (DEBUG) writer.write(tm1.toString());
				orphanCount1++;
				it2.previous();
			}
			else {
				orphans.add(tm2);
				if (DEBUG) writer.write(empty + tm2.toString());
				orphanCount2++;
				it1.previous();
			}
			
			
		}
		
		if (DEBUG) writer.close();
		
		System.out.println("Found " + orphans.size() + " orphans.");
		
		// Determine how many from each source
		if (log1.size() > 0){
			String source1 = log1.get(0).getSource();
			System.out.println(source1 + ": " + orphanCount1 + " orphans");
		}
		if (log2.size() > 0){
			String source2 = log2.get(0).getSource();	
			System.out.println(source2 + ": " + orphanCount2 + " orphans");
		}
		
		return orphans;		
	}
	
	public static ArrayList<TextMessageRecord> splitMessage(TextMessageRecord tm) throws IOException{
		ArrayList<TextMessageRecord> splitMessages = new ArrayList<TextMessageRecord>();
		int nMsgs = tm.getNumTextsContainted();
		int approxLen = (int)Math.floor(tm.getLen() / nMsgs);
		TextMessageRecord newTm ;
		for (int i = 0; i < nMsgs; i++){
			newTm = getTextRecordByType(tm.getSource(), tm.getRawInfo());
			// Override length
			newTm.setLen(approxLen);
			splitMessages.add(newTm);
		}
		return splitMessages;
			
	}
}







