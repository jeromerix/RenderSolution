package com.renderapi;

import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class RenderAPI {
	
	// Voor de server log
	public static enum MessageType { NOTICE, WARNING, ERROR, FATAL, DEBUG };

	// Of nog beter: via de commandline arguments?
	static final String iniFile = "/RenderSolutions/renderapi.ini";
	
	// Deze twee variabelen ophalen via INI file
	static int port = 4242; 
	static String serverLogFilename = "/RenderSolutions/logfile.txt"; 
	
	//regelt add project, en het wijzigen van render attributes
	static RenderConfig settings;
	
	// lijst met projecten
	static ArrayList<RenderAttributes> projects;
	
	// globale server log file
	// DESIGNQUESTION deze via netwerk en JSON toegankelijk maken?
	static ServerLog log;
	
	// enum voor netwerk messages
	enum NetworkMessageType {
		PROJECTADD(0),
		PROJECTDELETE(1),
		GETATTR(2),
		SETATTR(3),
		UPLOADFILE(4),
		DOWNLOADFILE(5),
		FILESAVED(6),
		PROJECTADDFILE(7),
		PROJECTDELFILE(8),
		PROJECTSYNC(9);

		int msg;
		
		private NetworkMessageType(int msgNum)
	    {
	        this.msg = msgNum;
	    }
		public int get()
	    {
	        return this.msg;
	    }
	};
	
	// enum voor netwerk error messages
	enum NetworkErrorType {
		NODATA(0),
		JSONINVALID(1),
		NOCMDARG(2),
		SOCKETERR(3),
		FILETRANSFERERR(4),
		IOERR(5),
		UNKNOWNFILESIZE(6),
		PROJECTADDERR(7),
		PROJECTGETERR(8),
		PROJECTDELERR(9),
		GETATTRERR(10),
		SETATTRERR(11),
		PROJECTGETFILESERR(12),
		FILENOTFOUNDERR(13),
		PROJECTFILEADDERR(14),
		PROJECTFILEDELERR(15),
		PROJECTSYNCERR(16);
		
		int err;
		
		private NetworkErrorType(int errNum)
	    {
	        this.err = errNum;
	    }
		public int get()
	    {
	        return this.err;
	    }
	}

	public static void main( String[] args ) {
				
		// Maak de server log
		// eerste flag is voor verbose (Schrijf de server log naar System.out)
		// tweede flag is voor debug (Schrijf de debug messages naar System.out en logfile)
		log = new ServerLog( true, true );
		
		// Globale server socket
		ServerSocket serverSock = null;
		
		// settings en projects
		settings = new RenderConfig();
		projects = new ArrayList<RenderAttributes>();
		
		try {
			// probeer de server socket te openen
			serverSock = new ServerSocket( port );
		} catch ( IOException ex ) {
			ServerLog.attachMessage( RenderAPI.MessageType.FATAL, "Could not open server port: " + port );
					ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "IO Error: " + ex.getMessage()  );
			return;
		}
				
		// Server now listens to port
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Server started at port: " + port );
			
		// Oneindige lus voor netwerk client requests
		while (true)
		{
			// de Socket
			Socket sock = null;
			
			try
			{
				// open de socket voor de client
				sock = serverSock.accept();
				
				// log
				ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "A new client is connected: " + sock);
				
				// maak de data streams
				DataInputStream dataInput = new DataInputStream(sock.getInputStream());
				DataOutputStream dataOutput = new DataOutputStream(sock.getOutputStream());
				
				// maak de client handler thread
				Thread t = new ClientHandler(sock, dataInput, dataOutput, settings);

				// Start de thread
				t.start();
				
			}
			catch (Exception e) {
				try {
					// Sluit de thread
					ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Closing connection: " + sock);
					sock.close();
					serverSock.close();
				} catch ( IOException ex ) {
					// geef de IO error, als er iets misgaat
					ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IO Error " + ex.getMessage()  );
				}
				e.printStackTrace();
			}
		}
	}
}


			

	



