# Balance Texts

**Author:** Joe Polin 

**Date:** May 2016

**Description:** Much like balancing one's checkbook, it can be useful to cross-reference my records of text messages with my carrier's records. In this case, I want to compare a log of text messages exported from my phone (using [SMS to Text](https://play.google.com/store/apps/details?id=com.smeiti.smstotext&hl=en)) with the logs downloaded from my carrier's portal (in this case, T-Mobile). The goal is to detect dropped text messages (those recorded by T-Mobile but that never reached my phone).

**To run**:

Before running, make sure that your folder structure looks like

balanceTexts
	\ bin
	\ data
		\ phone
			\ 2016-05-29.csv
		\ carrier
			\ 2016-03-12.csv
			\ 2016-04-14.csv
			.
			.
			.
			
It is important that all csv files be sorted by date (should be automatically), and that the files be named so that an alphanumeric sort will read them chronologically. Note that this code DOES NOT sort the dates (todo: throw error if things are found to be out of order).

**Notes:**

- Matching: When comparing texts from the two different logs, I make sure that the logs match on
  - Outgoing/incoming
  - Phone number
  - Message type (picture, text)
  - Date/time (within 1 minute, subject to change)
 	- Matching the date time can be unreliable when multiple messages to/from the same person are sent in rapid succession. To address this, I make sure that, when searching for a match, I find the earliest corresponding entry that matches. Let's say someone sent me 3 messages within 1 minute, and the first one was dropped (it only shows up on my carrier's log, but not my phone's). Then, when it's possible that I match the 2 texts on my phone to the *first* two texts on my carrier for the time interval. Even though this is technically wrong, we'll still identify an orphan from the same number and at nearly exactly the same time. There could be other edge cases like this that will need to be monitored manually for now.