package com.renderapi;

import java.util.ArrayList;
import java.text.SimpleDateFormat;  
import java.util.Date;  
import java.io.*;


public class ServerLog {
	
	//de server logfile
	static ArrayList<String> logfile = new ArrayList<String>();
	static ArrayList<String> logfileJSON = new ArrayList<String>();
	
	// Verbosity
	static boolean verbose;
	static boolean debug;
	
	public ServerLog( boolean verbose, boolean debug ) {
		// construct
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Server log started");
		ServerLog.verbose = verbose;
		ServerLog.debug = debug;
	}
	
	public static String getServerLogJSON() {
		String json = "";
		
		json = "{";
		json += "\"server_log\": [ ";
		
		for( String record: ServerLog.logfileJSON ) {
			json += record + ",";
		}
		if( ServerLog.logfileJSON.size() > 0 ) {
			json = json.substring(0, json.length() - 1);
		}
		json += " ] } ";

		
		return json;
	}
	public static void attachMessage ( RenderAPI.MessageType type, String message) {
		// een server log message bestaat uit:
		// Datum - Type - Message
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
		Date date = new Date();
		String logMessage;
		String logMessageJSON;
		String typeStr = "";
		switch( type ) {
			case NOTICE:
				typeStr = "NOTICE";
				break;
			case WARNING:
				typeStr = "WARNING";
				break;
			case ERROR:
				typeStr = "ERROR";
				break;
			case FATAL:
				typeStr = "FATAL";
				break;
			case DEBUG:
				typeStr = "DEBUG";
				break;
		}
		
		logMessage = formatter.format(date) + " " + typeStr  + " " + message + "\n";
		logfile.add(logMessage);
		
		if( type != RenderAPI.MessageType.DEBUG ) {
			logMessageJSON = " { \"date\" : \"" + 
				formatter.format(date) +
				"\", \"type\": \"" + 
				typeStr  + 
				"\", \"message\" : \"" + 
				message + 
				"\" } ";
			
			logfileJSON.add(logMessageJSON);
		}
		
		
		// Maak de logfile aan
		File file = new File(RenderAPI.serverLogFilename);
		FileOutputStream outStream;
		PrintStream printStream = null;
		try {
			// open de LogFile
			outStream = new FileOutputStream( file, true );
			printStream = new PrintStream( outStream );
		} catch (FileNotFoundException e) {
			// DESIGNQUESTION Als er op de back-end de logfile niet toegankelijk wordt gemaakt
			// b.v. via FTP, dan exit de server? Testen?
			System.out.println( "FATAL Could not open server log file: " + RenderAPI.serverLogFilename );
			System.exit(0);
		}
		if( !file.canRead() || !file.canWrite() ) {
			System.out.println( "FATAL Could not open server log file: " + RenderAPI.serverLogFilename );
			System.out.println();
			System.exit(0);
		}
		try {
			
			// schrijf naar logFile en omit debug messaages als nodig
			if( !ServerLog.debug ) {
				if( type != RenderAPI.MessageType.DEBUG ) {
					printStream.print(logMessage);
				}
			} else {
				printStream.print(logMessage);				
			}

			// schrijf naar System.out en omit debug messaages als nodig
			if( ( ServerLog.verbose && !ServerLog.debug ) ) {
				if( type != RenderAPI.MessageType.DEBUG ) {
					System.out.print(logMessage);
				}
			} else if ( ServerLog.verbose  ) {
				System.out.print(logMessage);
			}
			
			printStream.close();
		} catch ( NullPointerException e) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Could not log message: " + logMessage  );
		}
	}

	public static String getServerLog() {
		// maak een String object van alle messages.
		String retval = "";
		for(String msg: logfile) {
			retval += msg;
			retval += "\n";
		}
		return retval;
	}
	
}
