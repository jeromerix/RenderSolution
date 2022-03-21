package com.renderapi;

import java.io.*;

import java.net.*;
import java.util.ArrayList;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * De RenderAPI is een applicatie die het mogelijk maakt voor de gebruiker
 * om via de API, Adobe Aftere Effects commando's uit te voeren op de server.
 *
 * @author      Dagmar Hofman
*/
public class RenderAPI {
	
	// Voor de server log
	/**
	 * De ServerLog message types: NOTICE, WARNING, ERROR, FATAL en DEBUG
	*/
	public static enum MessageType { NOTICE, WARNING, ERROR, FATAL, DEBUG };

	// Of nog beter: via de commandline arguments?
	static final String iniFile = "/RenderSolutions/renderapi.ini";
	
	// Deze twee variabelen ophalen via INI file
	static int port = 4242; 
	static String serverLogFilename = "c:\\RenderSolutions\\logfile.txt"; 
	static String serverStatusJSON = "c:\\RenderSolutions\\status.json"; 
	
	//regelt add project, en het wijzigen van render attributes
	static RenderConfig settings;
	
	// lijst met projecten
	static ArrayList<RenderAttributes> projects;
	static ArrayList<QueueAttributes> queue;
	
	// globale server log file
	// DESIGNQUESTION deze via netwerk en JSON toegankelijk maken?
	static ServerLog log;
	
	/**
	 * De MessageHandler class genereert aan de hand van een 
	 * NetworkMessageType de boodschap die naar de socket gestuurt moet worden.
	*/
	public static enum NetworkMessageType {
		PROJECTADD(0),
		PROJECTDELETE(1),
		GETATTR(2),
		SETATTR(3),
		UPLOADFILE(4),
		DOWNLOADFILE(5),
		FILESAVED(6),
		PROJECTADDFILE(7),
		PROJECTDELFILE(8),
		PROJECTSYNC(9),
		RENDERCMDOK(10);

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
	
	/**
	 * De MessageHandler class genereert aan de hand van een 
	 * NetworkErrorType de foutmelding die naar de socket gestuurt moet worden.
	*/
	public static enum NetworkErrorType {
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
		PROJECTSYNCERR(16),
		RENDERCMDFAIL(17);
		
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
	
	/**
	* Deze functie leest alle bytes uit een bestand,  
	* naar een string
	* 
	* @param filePath De bestandsnaam
	* @return De inhoud van het bestand 
	*/
	public static String readAllBytes(String filePath) 
    {
		
		File file;
		file = new File(filePath);
		
		if( file.exists() == false ) {
			ServerLog.attachMessage( RenderAPI.MessageType.WARNING, "Status file did not exist. Created.");
			systemOutJson();
			return "";
		}
        StringBuilder contentBuilder = new StringBuilder();
        try  
        {
        	BufferedReader br = new BufferedReader(new FileReader(filePath));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) 
            {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
	
	/**
	* Deze functie leest de <b>system.json</b> file.  
	* En zet de variabelen.
	*/
	public static void systemParseJSON() {

		String fileData;
		
		fileData = readAllBytes( RenderAPI.serverStatusJSON );
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "status JSON read: " + fileData );
		
		// probeer het JSON request te parsen
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(fileData);
			
		} catch (Exception e ) {
			ServerLog.attachMessage( RenderAPI.MessageType.WARNING, "Could not read status data: " + e.getMessage() );
			return;
		}


		JSONObject systemData;
		
		systemData = (JSONObject)json.get("system_data");
		
		JSONArray projects;
		
		projects = (JSONArray)systemData.get("project_data");
		
		ArrayList<JSONObject> projectData = new ArrayList<JSONObject>(projects);
		
		for( JSONObject tmp: projectData ) {
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "tmp: " + tmp.toJSONString() );
			RenderAttributes tmpAttr = new RenderAttributes();
			String projNumStr;
			
			projNumStr = tmp.get( "project_num" ).toString();
			tmpAttr.projectNum = Integer.parseInt(projNumStr);
			
			tmpAttr.aerenderExe = tmp.get("aerender_exe").toString();
			
			tmpAttr.projectName = tmp.get("project_name").toString();
			tmpAttr.projectPath = tmp.get("project_path").toString();
			tmpAttr.compositionName = tmp.get("composition_name").toString();
			tmpAttr.renderSettings= tmp.get("render_settings").toString();
			tmpAttr.outputSettings = tmp.get("output_settings").toString();
			tmpAttr.outputFile = tmp.get("output_file").toString();

			tmpAttr.projectFiles = new ArrayList<String>();
			
			JSONArray projectFiles;
			projectFiles = (JSONArray)tmp.get("files");

			int i;
			
			for (i = 0; i < projectFiles.size(); i++  ) {
				String fileName;
				fileName = (String)projectFiles.get(i);
				tmpAttr.projectFiles.add(fileName);
			}
				
			RenderAPI.projects.add(tmpAttr);
		}
		
		JSONArray renderQueue;
		
		renderQueue = (JSONArray)systemData.get("queue");
		
		ArrayList<JSONObject> queueData = new ArrayList<JSONObject>(renderQueue);
		String msg = "";
		
		for( JSONObject tmp: queueData ) {
			JSONObject data = (JSONObject) tmp.get("record");
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "queue tmp: " + data.toJSONString() );
			// Controleer of JSON klopt
			JSONParser queueParser = new JSONParser();
			JSONObject queueJson;	

			try {
				queueJson = (JSONObject) queueParser.parse(data.toJSONString());
			} catch (Exception e ) {
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Parse Error. " + e.getMessage() );
				return;
			}
			
			QueueAttributes attr = new QueueAttributes();
			String attrId = queueJson.get("id").toString();
			
			Date startDate = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
			String dateStr;
			
			attr.id = Integer.parseInt(attrId);
			RenderCommands.renderCommandCount = attr.id + 1;
			
			dateStr = queueJson.get("start").toString(); 
			try {
				attr.start = formatter.parse(dateStr);
			} catch ( Exception e ) {
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Could not parse: " + dateStr );
			}
			
			attr.attributes = new RenderAttributes();
			
			attr.attributes.aerenderExe = queueJson.get("aerender_exe").toString();
			attr.attributes.projectPath = queueJson.get("project_path").toString();
			attr.attributes.compositionName = queueJson.get("comp_name").toString();
			attr.attributes.renderSettings = queueJson.get("render_settings").toString();
			attr.attributes.outputSettings = queueJson.get("output_settings").toString();
			attr.attributes.outputFile = queueJson.get("output_file").toString();

			RenderAPI.queue.add(attr);
		}
		
		
	}
		

	public static void networkLoop( ServerSocket serverSock) {
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
			Thread t = new ClientHandler(sock, dataInput, dataOutput, settings, queue);

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
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Calling sytemOutJson" );
	}

		
	/**
	* Deze functie scrhijft alle variabelen naar de <b>system.json</b> file.
	*/
	public static String systemOutJson() {
		String json  = "";
		
		ArrayList<RenderAttributes> attributes;
		attributes = RenderAPI.projects;
		
		json = "{ \"system_data\" : { \"project_data\" : [ \n";
		int i = 0;
		for( RenderAttributes attr: attributes ) {

			ArrayList<String> projectFiles;
			
			projectFiles = attr.projectFiles;
			json += " { ";						
			json += "\"project_num\" : " + attr.projectNum + ",";
			json += "\"project_name\" : \"" + attr.projectName  + "\",";
			json += "\"aerender_exe\" : \"" + attr.aerenderExe  + "\",";
			json += "\"project_path\" : \"" + attr.projectPath  + "\",";
			json += "\"composition_name\" : \"" +  attr.compositionName  +  "\",";
			json += "\"render_settings\" : \"" + attr.renderSettings  +  "\",";
			json += "\"output_settings\" : \"" + attr.outputSettings  + "\",";
			json += "\"output_file\" : \"" + attr.outputFile + "\",";
			
			json += "\"files\" : [ ";
			for( String fileName: projectFiles ) {
				fileName = fileName.replaceAll("\\\\+","\\\\\\\\");
				json += "\"" + fileName + "\",";
			}
			json = json.substring(0, json.length() - 1);  
			json += " ] ";
			json += "},";						
		}
		json = json.substring(0, json.length() - 1);  
		json += " ],";

		String temp;
		temp = RenderCommands.doCmdGetProjectQueue();
		json += temp.substring(1, temp.length());
		
		json += " }";
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "JSON generated: " + json );
		
		File outFile = new File( RenderAPI.serverStatusJSON );

		try {
			outFile.delete();
			outFile.createNewFile();
		} catch (IOException e) {
			// Log de error naar de server log
			ServerLog.attachMessage( RenderAPI.MessageType.FATAL, "IOException: " + e.getMessage() );
			System.exit(0);
		}
		
		// File output stream
		OutputStream outStream;
		
		// Debug log dat er geprobeerd wordt om naar file te schrijven
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to save file: " + outFile.getName() );
		
		try {
			// Open de file output stream
			outStream = new FileOutputStream( outFile, false );
			int len;
			len = (int) json.length();
			byte[] bytes = new byte[len];
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to save JSON: \n" + json );
			bytes = json.getBytes();
			try {
				outStream.write(bytes, 0, len);
				outStream.flush();
				outStream.close();
			} catch (IOException e) {
				// Log de error naar de server log
				ServerLog.attachMessage( RenderAPI.MessageType.FATAL, "IOException: " + e.getMessage() );
				System.exit(0);
			}	
		} catch(FileNotFoundException e) {
			// Handle de File Not Found exceptie
			ServerLog.attachMessage( RenderAPI.MessageType.FATAL, "File not found: " + e.getMessage() );
			System.exit(0);
		}
		return json;
	}
	
	
	/**
	* De <b>main</b> routine.
	*/
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
		RenderAPI.queue = new ArrayList<QueueAttributes>();
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

		// Lees de systeem status JSON
		systemParseJSON();
				
		// Oneindige lus voor netwerk client requests
		while (true)
		{
			networkLoop( serverSock );
			// renderLoop();
		}
	}
}


			

	



