import java.io.*;
import java.text.*;
import java.util.*;


public class TextLog {
	
	// Build list of all records for this file and type
	public static ArrayList<textMessageRecord> buildRecordList(File aggregatedFile, String recordType) throws IOException{
		
		// Open reader for file
		BufferedReader reader = new BufferedReader(new FileReader(aggregatedFile.toString()));
		
		// Iterate over file and build array
		String line = reader.readLine();
		ArrayList<textMessageRecord> textRecs = new ArrayList<textMessageRecord>();
		while (line != null){
			textMessageRecord textRec;
			
			// Map to custom message formats/parsers here
			if (recordType.equals("carrierRecord"))
				textRec = new carrierRecord(line);
			else if (recordType.equals("phoneRecord"))
				textRec = new phoneRecord(line);
			else {
				reader.close();
				throw new IOException("Did not recognize type " + recordType);
			}
			
			// Add to list
			if (textRec.successfulParse)
				textRecs.add(textRec);
			
			// Get next line
			line = reader.readLine();
				
		}
		
		System.out.println("Parsed " + textRecs.size() + " records for " + recordType);
		
		reader.close();
		return textRecs;
	}
}

// Code needed to convert records in csv files to objects that we can work with
abstract class textMessageRecord {
	
	// Definition of message
	public boolean successfulParse;
	public Date timestamp;
	public String contactNumber; // 9 numerals only
	public boolean outgoing;
	public String rawInfo;
	
	final int timeJitterSeconds = 90; // Might need to try adjusting this as needed
	
	public textMessageRecord(String commaDelimitedInfo){
		rawInfo = commaDelimitedInfo;
		successfulParse = parseInfo(rawInfo);		
	}
	
	// Specific to data source, returns true if record extracted
	public abstract boolean parseInfo(String commaDelimitedInfo);
	
	// Check if is same
	public boolean isEqual(textMessageRecord text){
		// Go from least to most costly checks + most to least likely to trigger
		if (text.outgoing != outgoing)
			return false;
		else if (!text.contactNumber.equals(contactNumber))
			return false;
		else if (Math.abs(text.timestamp.getTime() - timestamp.getTime()) > timeJitterSeconds)
			return false;
		// All checks passed
		return true;
	}
}

// For carrier (customize for files in carrier directory)
class carrierRecord extends textMessageRecord {
	
	public carrierRecord(String commaDelimitedInfo){
		super(commaDelimitedInfo);
	}
	
	public boolean parseInfo(String commaDelimitedInfo){
		// Location is useless, and confuses the commas. Remove any field that has a comma in quotes:
		// Original: 11/26/15,11:29 AM,"Painesvl, OH",(440) 796-6227,Outgoing,Text,$0.00,
		// After:    11/26/15,11:29 AM,               (440) 796-6227,Outgoing,Text,$0.00,
		commaDelimitedInfo = commaDelimitedInfo.replaceAll("\"([^\"]*),([^\"]*)\",", "");
		
		String fields[] = commaDelimitedInfo.split(",");
		
		// Check if we have a full record
		if (fields.length != 6)
			return false;
		
		// Get date/time
		String timeStr = fields[0] + " " + fields[1];
		DateFormat format = new SimpleDateFormat("MM/DD/YY HH:MM a");
		try {
			timestamp = format.parse(timeStr);
		} catch (ParseException e) {
			System.out.println("Could not extract date from " + timeStr);
			return false;
		}
		
		// Get phone number as numeral only string
		contactNumber = fields[2].replaceAll("[()\\s-]*","");
		
		// Direction
		outgoing = fields[3].equals("Outgoing") ? true : false;
		
		return true;
	}
}

// For phone (customize for files in phone output)
class phoneRecord extends textMessageRecord {
	
	public phoneRecord(String commaDelimitedInfo){
		super(commaDelimitedInfo);
	}
	// 2015-09-26,01:29:05,out,+14404886932,Eric Schupp,"No problem, he asked :) haha you have many aspirations mr. Schupp"
	public boolean parseInfo(String commaDelimitedInfo){
		
		String fields[] = commaDelimitedInfo.split(",");
		
		// Check if we have a full record (not bothering to catch commas in message payload)
		if (fields.length < 6)
			return false;
		
		// Get date/time
		String timeStr = fields[0] + " " + fields[1];
		DateFormat format = new SimpleDateFormat("MM-DD-YY HH:MM:SS");
		try {
			timestamp = format.parse(timeStr);
		} catch (ParseException e) {
			// May be because a newline was used in a text
			// System.out.println("Could not extract date from " + timeStr);
			return false;
		}
		
		// Direction
		outgoing = fields[2].equals("out") ? true : false;
		
		// Get phone number as numeral only string (drop +1)
		contactNumber = fields[3].replaceAll("^(\\+1)","");
		
		return true;
	}
}






