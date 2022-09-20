package com.renderapi;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner; // Import the Scanner class to read text files
import java.net.*;


public class FileTransfer {
	
	// FileTransfe regelt de opslag van de socket
	// sock is de geopende socket van de TCP poort
	final Socket sock;
	
	// buffer grootte
	// DESIGNQUESTION 1KB of zelfs 4KB gaan gebruiken?
	// latency?
	final static int blockSize = 512;

	// File object en file metadata
	static File openFile;
	String fileName;
	long fileSize;
	
	public FileTransfer(Socket sock, int port, String fileName, long size ) 
	{
		// open TCP socket
		this.sock = sock;
		
		// file metadata
		this.fileName = fileName;
		this.fileSize = size;
	}
	
	
	public String recvFile(Socket sock, ServerSocket serverSock ) {

		String msg = "";
		// Maak het file object aan
		openFile = new File( this.fileName );

		this.fileSize = openFile.length();
		
		// Controleer of het bestand te schrijven is
		boolean write;
		write = openFile.canRead();
		
		// Stuur error als file niet te schrijven is 
		if( !write ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "Can not read from file: " +  this.fileName  );
			return msg;
		}
	
		// File input stream
		InputStream inStream;
		
		// Debug log dat er geprobeerd wordt om naar file te schrijven
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to read file: " + this.fileName );
		
		try {
			inStream = new FileInputStream( openFile );
		} catch(FileNotFoundException e) {
			// Handle de File Not Found exceptie
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "File not found: " +  this.fileName + " Exception: " + e.getMessage() );
			return msg;
		} 
		
		//hou de bestandsgrootte bij
		long totalBytes = 0;

		// Schrijf de file size naar de debug log
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "File size: " + this.fileSize );

		// Transfer buffer
		byte[] buffer = new byte[FileTransfer.blockSize];
		int bytesRead = 0;
		
		
		// Output stream wordt niet gebruikt
		DataInputStream dataInput;
		DataOutputStream outStream;

		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Transfer via port: " + serverSock.getLocalPort() );
			
		try {
			outStream = new DataOutputStream ( sock.getOutputStream() );	
			dataInput = new DataInputStream( inStream );
		
			do {
				// Lees FileTransfer.blockSize bytes van de stream en hou de totaal grootte bij
				bytesRead = dataInput.readNBytes(buffer, 0, FileTransfer.blockSize );
				totalBytes += bytesRead;
				
				// Debug hoeveel bytes er al gelezen zijn
				ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Read from socket bytes: " + totalBytes );

				// schrijf de buffer naar de file
				if( totalBytes <  this.fileSize  ) {
					outStream.write(buffer, 0,  bytesRead); 
				}
				// controleer de grootte
			} while( totalBytes < this.fileSize );

			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "TotalBytes: " + totalBytes + " FileSize: " + this.fileSize );
				 
			long left;
			
			left = this.fileSize - totalBytes + bytesRead;
			
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Bytes left: " + left );

			dataInput.readNBytes(buffer,0,(int)left);
			outStream.write(buffer,0,(int)left); 
			
			
			// Struit de outstream
			outStream.flush();
			outStream.close();

		} catch (IOException e ) {
			// Handle IO exception en schrijf JSON error als die optreed
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IOException: " + e.getMessage() );
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "Could not save file: " +  this.fileName  );
			return msg;
		}
		
		
		return msg;
	
	}
	
	public String saveFile() {

		// Maak het file object aan
		openFile = new File( this.fileName );

		try {
			// Probeer de file te openen
			openFile.createNewFile();
		} catch (IOException e) {
			// Log de error naar de server log
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IOException: " + e.getMessage() );
			
			// Stuur de file IO error naar de JSON output.
			String msg;
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.IOERR,  "Could not save file: " +  this.fileName + " " + e.getMessage() );
			return msg;
		}

		// Controleer of het bestand te schrijven is
		boolean write;
		write = openFile.canWrite();
		
		// Stuur error als file niet te schrijven is 
		if( !write ) {
			String msg;
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "Can not write to file: " +  this.fileName  );
			return msg;
		}
		
		// File output stream
		OutputStream outStream;
		
		// Debug log dat er geprobeerd wordt om naar file te schrijven
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to save file: " + this.fileName );
		
		try {
			// Open de file output stream
			outStream = new FileOutputStream( openFile, false );
		} catch(FileNotFoundException e) {
			// Handle de File Not Found exceptie
			String msg;
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "File not found: " +  this.fileName + " Exception: " + e.getMessage() );
			return msg;
		} 
		
		//hou de bestandsgrootte bij
		long totalBytes = 0;

		// Schrijf de file size naar de debug log
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "File size: " + this.fileSize );

		// Transfer buffer
		byte[] buffer = new byte[FileTransfer.blockSize];
		int bytesRead = 0;
		
		
		// Output stream wordt niet gebruikt
		DataInputStream dataInput;
		DataOutputStream dataOutput; 

		try {
			// Open het bestand naar een Stream
			dataInput = new DataInputStream(this.sock.getInputStream());
			dataOutput = new DataOutputStream(this.sock.getOutputStream());
		} catch (IOException e) {
			// Handle IO exceptie
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IOException: " + e.getMessage() );
			try {
				// Probeer bestand te sluiten
				outStream.close();
			} catch (IOException ex) {
				// Schrijf error naar logfile
				ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IOException: " + ex.getMessage() );
			}
			// Schrijf de file transfer error naar JSON output
			String msg;
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "Could not save file: " + this.fileName  + " " + e.getMessage() );
			return msg;			
		}
		
		try {

			do {
				// Lees FileTransfer.blockSize bytes van de stream en hou de totaal grootte bij
				bytesRead = dataInput.readNBytes(buffer, 0, FileTransfer.blockSize );
				totalBytes += bytesRead;
				
				// Debug hoeveel bytes er al gelezen zijn
				ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Read from socket bytes: " + totalBytes );

				// schrijf de buffer naar de file
				if( totalBytes <  this.fileSize  ) {
					outStream.write(buffer, 0,  bytesRead); 
				}
				// controleer de grootte
			} while( totalBytes < this.fileSize );
				 
			// verwijder het laatste blok
			totalBytes -= FileTransfer.blockSize;
			
			// bereken de overige bytes
			int left =(int)( this.fileSize - totalBytes );

			// schrijf deze naar de debug log
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Bytes left: " + left );
			
			//schrijf de laatste bytes
			// DESIGNQUESTION volgens mij werkt dit, maar is het handig een MD5 checksum controle te doen
			outStream.write(buffer, 0,  left); 
			
			// Debug.
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "TotalBytes: " + totalBytes + " FileSize: " + this.fileSize );
			
			// Struit de outstream
			outStream.flush();
			outStream.close();

		} catch (IOException e ) {
			// Handle IO exception en schrijf JSON error als die optreed
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "IOException: " + e.getMessage() );
			String msg;
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILETRANSFERERR,  "Could not save file: " +  this.fileName  );
			return msg;
		}
		
		// Stuur de save message naar de JSON output
		String msg;
		msg = MessageHandler.prepareMessage( RenderAPI.NetworkMessageType.FILESAVED, "" + this.fileSize  );
		return msg;
	}
	
}
