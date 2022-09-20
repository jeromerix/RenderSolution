package com.renderapi;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;  
import java.util.Date; 
import java.util.UUID;    

public class ExecHandler  extends Thread  {
	
	String execCmd;
	static UUID uuid; 
	static String uuidStr;
	ExecAttributes tmpAttr;
	int queueId;
	
	public ExecHandler( String cmd, int id ) {
		this.execCmd = cmd;
		
		this.queueId = id;
		
		uuid = UUID.randomUUID();
		uuidStr = uuid.toString();  
	}
	
	public ArrayList<String> getTokens(String str) {
	    ArrayList<String> tokens = new ArrayList<String>();
	    StringTokenizer tokenizer = new StringTokenizer(str, " ");
	    while (tokenizer.hasMoreElements()) {
	        tokens.add(tokenizer.nextToken());
	    }
	    return tokens;
	}
	

	public void processLine(String line) {

		String startTime = "";
		String endTime = "";
		
		String compareLabel = "";
		String startLabel = "PROGRESS:  Start:";
		String endLabel = "PROGRESS:  End:";

		String progressLabel = "PROGRESS: ";
		
		if(line.length() > startLabel.length() ) {
			compareLabel = line.substring(0, startLabel.length());
			if( compareLabel.compareTo(startLabel) == 0) {
				startTime = line.substring(startLabel.length() + 1);
				tmpAttr.startTime = startTime;
				tmpAttr.active = true;
			};
		}
		if( line.length() > endLabel.length() ) {
			compareLabel = line.substring(0, endLabel.length());
			if( compareLabel.compareTo(endLabel) == 0) {
				endTime = line.substring(endLabel.length() + 1);
				tmpAttr.endTime = endTime;				
				tmpAttr.active = true;
			};
		}
		
		if( startTime != "" ) {
			tmpAttr.startTime = startTime;
		}
		if( endTime != "" ) {
			tmpAttr.endTime = endTime;
		}
	}
	public void processProgressLine(String line) {

		String progressTime = "";
		
		String compareLabel = "";
		String progressLabel = "PROGRESS: ";

		
		if(line.length() > progressLabel.length() + 11) {
			compareLabel = line.substring(0, progressLabel.length());
			if( compareLabel.compareTo(progressLabel) == 0) {
				progressTime = line.substring(progressLabel.length() + 1,progressLabel.length() + 11 );
				if( progressTime.charAt(1) == ':' ) { 
					tmpAttr.progressTime = progressTime;
				} else {
					progressTime = "";
				}
			};
		}
		
	}
	
	@Override
	public void run() 
	{
		
		var processBuilder = new ProcessBuilder();
		
		processBuilder.command(execCmd);
		
		RenderAPI.execCount++;
		
		tmpAttr = new ExecAttributes( uuidStr, "none", "none", "none", false );
		RenderAPI.execProgress.add(tmpAttr);
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Render Job Started: " + uuidStr );

		
		String recv = "";
		String str ="";
		int lineCount = 0;
		try {
			
			int i;
			
			var process = processBuilder.start();
			var reader = new BufferedReader( new InputStreamReader(process.getInputStream()));

		
			while( recv == "" ) {
				
				// Lees de JSON data van de socket.
				try {
			        while ( reader.ready() || process.isAlive() ) {
			        	str = reader.readLine();
			        	if( str == null ) continue;
			    		lineCount++;
			    		if( lineCount < 89 ) {
			    			processLine(str);
			    		} else {
			    			processProgressLine(str);
			    		}
			    		

			    		for( i=0;i<RenderAPI.execProgress.size(); i++ ) {
			    			if( RenderAPI.execProgress.get(i).uuid.compareTo(this.uuidStr) == 0 ) {
			    				RenderAPI.execProgress.set(i, tmpAttr);
			    			}
			    		}
			    		
						recv += str;
			        }
				} catch (IOException e) {
					// DESIGNQUESTION misschien hier een foutmelding naar de socket sturen?
					ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IO Error " + e.getMessage()  );
				}
			}

			process.waitFor();
			tmpAttr.active = false;
			ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Render Job Finished: " + this.uuidStr );
			tmpAttr.finished = true;
			
    		for( i=0;i<RenderAPI.execProgress.size(); i++ ) {
    			
    			if( RenderAPI.execProgress.get(i).uuid.compareTo(this.uuidStr) == 0 ) {
    				RenderAPI.execProgress.set(i, tmpAttr);
    			}
    		}
		
    		RenderAPI.systemOutJson();
    		
    		RenderCommands.doCmdDelProjectFromQueueId( this.queueId );

		} catch( Exception e ) {

    		ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Exception: " + e.getMessage() );
        }
		
	}
}