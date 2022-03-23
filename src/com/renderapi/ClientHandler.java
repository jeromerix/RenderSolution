package com.renderapi;

import java.io.*;

import java.net.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * De ClientHandler Thread handelt alle netwerkverbingen af. 
 * En het voert de betreffende RenderCommand uit.
 *
 * @author      Dagmar Hofman
*/

public class ClientHandler extends Thread 
{
	// Data input, output en socket
	DataInputStream dataInput;
	DataOutputStream dataOutput;
	Socket sock;

	RenderCommands cmds;
	ArrayList<QueueAttributes> queue;
	
	/**
	* De constructor
	* 
	* @param s Een bestaande <b>socket</b> voor netwerkcommunicatie.
	* @param dis Een bestaande <b>data input stream</b> voor netwerkcommunicatie.
	* @param dos Een bestaande <b>data output stream</b> voor netwerkcommunicatie.
	* @param settings Een referentie naar de RenderConfig class.
	* @param queue Een referentie naar de globale <b>queue</b> met render opdrachten.
	*/
	public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, RenderConfig settings, ArrayList<QueueAttributes> queue ) 
	{
		// initialiseer socket en data streams
		this.sock = s;
		this.dataInput = dis;
		this.dataOutput = dos;

		// attach render commands
		cmds = new RenderCommands();
		this.queue = queue;
	}


	/**
	*
	* Deze functie contoleert of de opgegeven data <i>valid JSON</i> is.
	* 
	* @param test De te testen string
	* @return true als het JSON betreft, false als de data geen JSON is.
	*/
	public boolean testJsonValid(String test) {

		// simpele wrapper om te controleren of de input JSON data betreft
		JSONParser parser = new JSONParser();
		
		try {
			// controleer alleen op de exceptie, string data verder niet nodig
			JSONObject json = (JSONObject) parser.parse(test);
			String tmp = json.toJSONString();
		} catch (Exception e ) {
			// geef server warning
			ServerLog.attachMessage( RenderAPI.MessageType.WARNING, "Invalid JSON received: " + test);
			return false;
		}
	    return true;
	    
	}
	
	/**
	*
	* Deze functie handelt de JSON data verder af.
	* 
	* @param str De af te te handelen JSON string.
	* @see RenderCommands
	*/
	public void handleJson( String str ) {
		
		// handleJson wordt aangeroepen als de json data valid is
		// value retMsg wordt later naar de netwerk socket geschreven
		String retMsg;
		
		// probeer alsnog een parse te doen, voor alle zekerheid
		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// schrijf error en return bij een parse error
		try {
			json = (JSONObject) parser.parse(str);
		} catch (Exception e ) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Parse error: " + str);
			return;
		}
		
		// initialiseer met undefined
		String command = "undefined";
		String arguments = "undefined";
		
		// geen command? stuur dan error message en return
		if ( json.containsKey( "command" ) == false ) {
			retMsg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "Missing command." );
			sendText(retMsg);
			return;
		}

		// probeer of er de command te parsen is
		try {
			if ( json.containsKey( "command" ) ) {
				command = json.get("command").toString();		
			}
		} catch (NullPointerException e) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "NullPointerException: " + e.getMessage());
		}
	
		// Alleen het get_projects commando heeft geen arguments nodig.
		if ( json.containsKey( "arguments" ) == false && 
				command.compareTo("get_projects") != 0 && 
				command.compareTo("get_system_status") != 0 && 
				command.compareTo("get_render_status") != 0 && 
				command.compareTo("get_server_log") != 0 && 
				command.compareTo("queue_get") != 0  
				) {
			retMsg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.NOCMDARG, "Missing arguments." );
			sendText( retMsg );
			return;
		}

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Processing command: " + command + " arguments: " + arguments  );

		//probeer of arguments te parsen is, anders error
		try {
			if ( json.containsKey( "arguments" ) ) {
				arguments = json.get("arguments").toString();
			}
		} catch (NullPointerException e) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "NullPointerException: " + e.getMessage());
		}

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Processing command: " + command + " arguments: " + arguments  );

		// Merk op dat de command of de arguments variabele hier leeg zou kunnen zijn.
		// processCommand handelt dan de verder fouten af
		retMsg = cmds.processCommand(command, arguments);
		
		sendText( retMsg );
				
	}	
	
	/**
	*
	* Een eenvoudige wrapper om tekst naar de socket server output te sturen.
	* 
	* @param text De te sturen tekst
	*/
	public void sendText( String text ) 
	{
		// eenvoudige wrapper om text naar via socket te sturen
		try {
			dataOutput.writeBytes(text);
		} catch( IOException e ) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Cound not send text: " + text + " IO Error " + e.getMessage()  );
		}
	}
	
	/**
	*
	* De main thread run()
	* 
	*/
	@Override
	public void run() 
	{
		// dit is de main thread om netwerk connecties af te handelen
		String recv = "";
		String str = "";	
		
		// maak een InputStreamReader en een BufferedReader
		InputStreamReader inp = new InputStreamReader(dataInput,
				StandardCharsets.UTF_8);
	
		
		BufferedReader br = new BufferedReader(inp);		

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Thread started" );
		
		while( recv == "" ) {
		
			// Lees de JSON data van de socket.
			try {
				char c;
				int i = 0;
		        while ( br.ready() && i++ < 8196 ) {
		        	c = (char)br.read();
		        	str += "" + c;
		        }
				recv += str;
			} catch (IOException e) {
				// DESIGNQUESTION misschien hier een foutmelding naar de socket sturen?
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IO Error " + e.getMessage()  );
			}
		}
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Recieved message: " + recv  );
		
		// Handel valid JSON data of stuur foutmelding
		if( testJsonValid(recv) == true ) {
			handleJson(recv);
		} else {
			if( recv.length() < 3 ) {
				str = MessageHandler.prepareError(RenderAPI.NetworkErrorType.NODATA, "" );
				sendText(str);
			} else {
				str = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
				sendText(str);
			}
		}
		
		// Ruim de Streams en de Socket netjes op.
		try {
			this.dataInput.close();
			this.dataOutput.close();
			sock.close();
		} catch (IOException ex  ) {
			// Verlangt geen foutmelding
			// DESIGNQUESTION resource leak mogelijk?
		}
	}
}

