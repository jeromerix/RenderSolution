package com.renderapi;

import java.io.*;
import java.net.*;

import org.json.simple.*;
import org.json.simple.parser.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
	
class FileSaveHandler extends Thread 
{
	// Deze Streams en Socket zijn voor de file interactie op een bepaalde port.
	DataInputStream dataInput;
	DataOutputStream dataOutput;
	Socket sock;

	// de message RenderAPI.NetworkMessageType.UPLOADFILE sla ik op in clientText
	// deze is static en wordt door RenderCommands.doCmdSendFile() gecontroleerd.
	// de execution thread wacht dus netjes of deze thread haar taak gedaan heeft.
	static String clientText = "";

	// de Server Socket
	// kiest zelf een vrije poort
	ServerSocket serverSock;
	
	// hou de actieve (dus geopende) TCP poort bij. 
	int activePort = -1;
	
	// het JSON commando geeft filename en size.
	// RenderCommands.doCmdSendFile() (Die deze thread start) 
	// stuurt deze naar de constructor.
	long size = 0;
	String project;
	String file;
	
	// Volgens mij is dit niet echt hard nodig
	// En bevindt zich er vrijwel altijd een transfer in de queue
	// DESIGNQUESTION uitvoerig testen van deze functionaliteit?
	ArrayList<TransferAttributes> transferQueue;
	
	public FileSaveHandler( long size, String projNumStr, String fileName ) 
	{
		// De transfer queue
		this.transferQueue = new ArrayList<TransferAttributes>();
		
		// File metadata
		this.size = size;
		this.project = projNumStr;
		this.file = fileName;
				
	}

	public int getActivePort() {
		// Wordt niet gebruikt
		return this.activePort;
	}
	
	public static String getClientText() {
		// RenderCommands.doCmdSendFile() wacht netjes of de thread een wait message heeft gestuurd.
		// DESIGNQUESTION Als de wait message niet gestruurd wordt, gaat dat ten koste van de latency. Testen?
		return FileSaveHandler.clientText;
	}

	public static void clearClientText() {
		// Deze is nodig om ervoor te zorgen dat er bij het eerste upload commando, get poortnummer vernieuwt.
		FileSaveHandler.clientText = "";
	}
	
	public static String stripNewlines( String str ) {
		// uiteindelijk heb ik de JSON errors en messages op een regel gezet.
		str = str.replace("\n", "").replace("\r", "");
		return str;
	}
	
	public void closeAll(Socket sock, ServerSocket serverSock) {
		// Sluit Streams en Socket
		try {
			this.dataInput.close();
			this.dataOutput.close();
			sock.close();
			serverSock.close();
		} catch (IOException ex  ) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Could not close: " + ex.getMessage() );
		}
	}

	@Override
	public void run() 
	{		
		// Voor de file upload wait message
		String msg;

		try {
			// genereer een vrije TCP poort
			this.serverSock = new ServerSocket( 0 );
			this.activePort = serverSock.getLocalPort();
		} catch ( IOException ex ) {
			// TODO stuur de error naar de Client in plaats van de wait message?
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Could not bind socket. File upload canceled");
			
			return;
		}
		
		// Schrijf naar de server log dat de TCP connectie er is
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "FileHandler Thread created. filesize: " + this.size + " port: " + this.activePort );

		// Emit de file wait boodschap
		msg = MessageHandler.prepareMessage( RenderAPI.NetworkMessageType.UPLOADFILE, project, size, file, this.serverSock.getLocalPort() );
		
		// RenderCommands.doCmdSendFile() leest welke wait er is.
		FileSaveHandler.clientText = msg ;

		// Voeg de transfer toe aan de queue
		TransferAttributes attr;
		attr = new TransferAttributes(this.serverSock, this.activePort, this.file, this.size );
		transferQueue.add(attr);

		// Debug the transfer queue 
		for(TransferAttributes attributes: transferQueue ) {
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Transfer queue index: " + 
					transferQueue.indexOf(attributes) +
					" port: " + 
					attributes.port +
					" file name: \"" +
					attributes.fileName +
					"\" file size: " +
					attributes.fileSize
				);			
		}

		// Lus door de transferqueue
		// DESIGNQUESTION uitgebreid debuggen?
		for(TransferAttributes attributes: transferQueue ) {
			String message = "";		
		
			try {
				// De Thread wacht hier op een socket connectie, dit kan eventueel duren.
				// DESIGNQUESTION timeout installeren?
				this.sock = attributes.sock.accept();
				this.dataInput = new DataInputStream(this.sock.getInputStream());
				this.dataOutput = new DataOutputStream(this.sock.getOutputStream());
			} catch ( IOException ex ) {
				// TODO stuur de error naar de Client in plaats van de wait message?
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IO Error: " + ex.getMessage() );
				return;
			}
			
			// FileTransfer opent het bestand, en leest van de Socket sock.
			FileTransfer transferObj = new FileTransfer(this.sock, attributes.port, attributes.fileName, attributes.fileSize );
			
			// Probeer het bestand op te slaan
			message = transferObj.saveFile();
			
			// TODO send message to socket output! 
			try {
				dataOutput.writeBytes( message);
			} catch( IOException e ) {
				// Log de error message
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Cound not send text: " + stripNewlines(message) + " IO Error " + e.getMessage()  );
			}
			
			// Log dat de file gestuurd is.
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "File object transferred." );

		}
		
		// verwijder de file metadata van de queue
		// DESIGNQUESTION Hoop dat dit de juiste is, Uitvoerig testen?
		transferQueue.remove(attr);
		
		// Sluit alle sockets en streams.
		closeAll(sock, serverSock);
	}
}

