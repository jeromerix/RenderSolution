package com.renderapi;

public class MessageHandler {
	
	// uitgebreide class voor netwerk messages en warnings
	
	// message nummers: 
	// PROJECTADD PROJECTDELETE GETATTR SETATTR UPLOADFILE DOWNLOADFILE FILESAVED
	static int msgNum;
	
	// error nummers:
	// NODATA JSONINVALID NOCMDARG SOCKETERR FILETRANSFERERR IOERR 
	// UNKNOWNFILESIZE PROJECTADDERR PROJECTGETERR PROJECTDELERR 
	// GETATTRERR SETATTRERR
	static int errNum;
	
	// message of error argument
	static String arg;
	
	// DESIGNQUESTION in eerdere versie nodig, in deze niet?
	static String lastMessage = "undefined";
	
	public static String getLastMessage( ) {
		// return last message
		return lastMessage;
	}
	
	public static String stripNewlines( String str ) {
		str = str.replace("\n", "").replace("\r", "");
		return str;
	}
	
	public static String prepareMessage( RenderAPI.NetworkMessageType msg, String arg) {
		// uitgebreide structuur voor network messages 
		String retval = "undefined";
		
		if( msg == RenderAPI.NetworkMessageType.PROJECTADD  ) {
			retval = "{"
				 + " \"message\": " + msg.get() + ","
				 + " \"project_num\": " + arg +","
				 + " \"description\": \"Project added.\""
				 + "}";
		} else if (msg == RenderAPI.NetworkMessageType.GETATTR ) {
			retval = "{"
					+ "  \"message\": " + msg.get() + ","
					+ "  \"project_num\": " + arg + ""
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.PROJECTDELETE  ) {
			retval = "{"
					+ "\"message\": " + msg.get() + ", "
					+ "\"project_num\": " + arg + ", "
					+ "\"description\": \"Project Deleted\" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.SETATTR) {
			retval = "{"
					+ "\"message\": " + msg.get() + ", "
					+ "\"project_num\": " + arg +", "
					+ "\"description\": \"Render Attributes changed for project " + arg + ".\" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.FILESAVED ) {
			retval = "{"
					+ " \"message\": " + msg.get() + ", "
					+ "\"description\": \"File saved. (" + arg + ") KBytes written. \" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.PROJECTADDFILE ) {
			retval = "{"
					+ " \"message\": " + msg.get() + ", "
					+ "\"description\": \"File added to project. (" + arg + ") \" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.PROJECTSYNC ) {
			retval = "{"
					+ " \"message\": " + msg.get() + ", "
					+ "\"description\": \"Project files synchronized.\" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.PROJECTDELFILE ) {
			retval = "{"
					+ " \"message\": " + msg.get() + ", "
					+ "\"description\": \"File removed from project. (" + arg + ") \" "
					+ "}";
		} else if (msg == RenderAPI.NetworkMessageType.RENDERCMDOK ) {
			retval = "{"
					+ " \"message\": " + msg.get() + ", "
					+ "\"description\": \"Render Queue OK. (" + arg + ") \" "
					+ "}";
		}
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "prepared message: " + stripNewlines(retval) );
		lastMessage = retval;
		return retval;
	}

	
	public static String prepareMessage( RenderAPI.NetworkMessageType msg, String arg, long size, String fileName, int port) {
		// uitgebreide structuur voor network messages
		// overloaded functie voor upload en download
		String retval = "undefined";
		
		if (msg == RenderAPI.NetworkMessageType.UPLOADFILE) {
			retval = "{ "
					+ "  \"message\": " + msg.get() + ","
					+ "  \"project_num\": " + arg +","
					+ "  \"file_size\": " + size +","
					+ "  \"file_name\": \"" + fileName + "\","
					+ "  \"port\": \"" + port + "\","
					+ "  \"description\": \"Waiting for file upload...\""
					+ "}";
		}  else if (msg == RenderAPI.NetworkMessageType.DOWNLOADFILE) {
			retval = "{"
					+ "  \"message\": " + msg.get() + ","
					+ "  \"project_num\": " + arg +","
					+ "  \"file_name\": \"" + fileName + "\","
					+ "  \"port\": \"" + port + "\","
					+ "  \"description\": \"Waiting for file download...\""
					+ "}";
		} 
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "prepared message: " + stripNewlines(retval) );
		lastMessage = retval;
		return retval;
	}

	
	public static String prepareError(  RenderAPI.NetworkErrorType err, String arg) {
		// uitgebreide structuur voor network errors 
		String retval = "undefined";
		
		if( err == RenderAPI.NetworkErrorType.NODATA  ) {
			retval = "{ "
					+ " \"error\": " + err.get() + ", "
					+ " \"description\": \"No data received\" "
					+ "}";			
		} else if (err == RenderAPI.NetworkErrorType.JSONINVALID ) {
			retval = "{"
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"JSON data invalid\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.NOCMDARG ) {
			retval = "{"
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"No JSON command or argument\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		}else if (err == RenderAPI.NetworkErrorType.SOCKETERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not bind socket.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		/* Error messages 5-9 reserved ! */
		} else if (err == RenderAPI.NetworkErrorType.FILETRANSFERERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"File Transfer Error.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";					
		} else if (err == RenderAPI.NetworkErrorType.IOERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"IO Error.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";					
		} else if (err == RenderAPI.NetworkErrorType.UNKNOWNFILESIZE ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"File size unknown.\" "
					+ "}";					
		}  else if (err == RenderAPI.NetworkErrorType.PROJECTADDERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not add project.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";					
		} else if (err == RenderAPI.NetworkErrorType.PROJECTFILEADDERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not add project file.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";					
		}else if (err == RenderAPI.NetworkErrorType.PROJECTFILEDELERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not delete project file.\" "
					+ "  \"file_name\": \"" + arg + "\" "
					+ "}";					
		} else if (err == RenderAPI.NetworkErrorType.PROJECTGETERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not get projects.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.PROJECTDELERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not delete project.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.GETATTRERR) {
			retval =
					"{"
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not get render attributes.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.SETATTRERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not set render attributes.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.PROJECTSYNCERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Could not sync project files.\", "
					+ "  \"reason\": \"" + arg + "\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.FILENOTFOUNDERR ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"File not found. (" + arg + ") .\" "
					+ "}";
		} else if (err == RenderAPI.NetworkErrorType.RENDERCMDFAIL ) {
			retval = 
					"{ "
					+ "  \"error\": " + err.get() + ", "
					+ "  \"description\": \"Render Queue Fail. (" + arg + ") .\" "
					+ "}";
		}
		lastMessage = retval;
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "prepared error: " + stripNewlines(retval) );
		return retval;
	}

}
