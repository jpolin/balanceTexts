import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Code needed to convert records in csv files to objects that we can work with
public abstract class TextMessageRecord {
	
	// Definition of message
	protected boolean successfulParse;
	protected Date timestamp;
	protected String contactNumber; // 9 numerals only
	protected boolean outgoing;
	protected String rawInfo;
	protected String source; // eg phone, carrier, etc.
	protected int len;
	final protected int maxMessageLength = 160; //chars
	
	final int timeJitterSeconds = 90; // Might need to try adjusting this as needed
	
	public TextMessageRecord(String commaDelimitedInfo){
		rawInfo = commaDelimitedInfo;
		successfulParse = parseInfo(rawInfo);		
	}
	
	// Copy constructor
	public TextMessageRecord(TextMessageRecord tm){
		successfulParse = tm.successfulParse;
		timestamp = tm.timestamp;
		contactNumber = tm.contactNumber;
		outgoing = tm.outgoing;
		rawInfo = tm.rawInfo;
		source = tm.source;
		len = tm.len;
	}
	
	// Specific to data source, returns true if record extracted
	public abstract boolean parseInfo(String commaDelimitedInfo);
	
	// Check if is same
	public boolean equals(TextMessageRecord text){
		// Go from least to most costly checks + most to least likely to trigger
		if (text.outgoing != outgoing)
			return false;
		else if (!text.contactNumber.equals(contactNumber))
			return false;
		else if (Math.abs(text.timestamp.getTime() - timestamp.getTime()) > 1000 * timeJitterSeconds)
			return false;
		// All checks passed
		return true;
	}
	
	// Print for csv file
	 public String toString() {
		 return timestamp.toString() + ", "
				 + contactNumber + ", "
				 + (outgoing ? "Outgoing" : "Incoming") + ", "
				 + source + ", \n";
	 }
	 
	 // Figure out how many files to split into
	 public int getNumTextsContainted(){
		 return (int) Math.ceil((double)len / maxMessageLength);
	 }
	 
	 // Some set/get methods
	 public String getSource(){ return source; }
	 public Date getTime(){ return timestamp; }
	 public int getLen(){ return len; }
	 public String getRawInfo(){ return rawInfo; }
	 public void setLen(int l){len = l; } 
}

// For carrier (customize for files in carrier directory)
class CarrierRecord extends TextMessageRecord {
	
	public CarrierRecord(String commaDelimitedInfo){
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
		DateFormat format = new SimpleDateFormat("MM/dd/yy KK:mm a");
		try {
			timestamp = format.parse(timeStr);
			// They have this on pacific time for some reason (add 3 hrs of milliseconds)
			timestamp = new Date(timestamp.getTime() + 3 * 60 * 60 * 1000);
		} catch (ParseException e) {
			System.out.println("Could not extract date from " + timeStr);
			return false;
		}
		
		// Get phone number as numeral only string
		contactNumber = fields[2].replaceAll("[()\\s-]*","");
		
		// Direction
		outgoing = fields[3].equals("Outgoing") ? true : false;
		
		source = "carrier";
		len = 1 ; // Don't know don't care
		return true;
	}
	
}

// For phone (customize for files in phone output)
class PhoneRecord extends TextMessageRecord {
	
	public PhoneRecord(String commaDelimitedInfo){
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
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
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
		
		source = "phone";
		len = rawInfo.length();
		
		return true;
	}	

}
