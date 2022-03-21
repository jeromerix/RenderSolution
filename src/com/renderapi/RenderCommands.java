package com.renderapi;


import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.util.ArrayList;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RenderCommands {
	// Hanteerd de Render Commands
	// Zie ook de protocol.txt
	RenderConfig cfg;
	
	static int renderCommandCount = 0;
	
	MessageHandler msgs;
	String jsonCmdOutput;

	RenderCommands() {
		cfg = new RenderConfig();
	}

	public String doCmdSendFile(String arg) {
		// send file metadata
		int index = -1;
		String msg = "";
		int projNum = -1;
		
		// probeer het JSON request te parsen
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			// handle errors
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , "" );
			return msg;
		}
		
		if ( json.containsKey( "project_num" ) == false ) {
			// contorleer project_num
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "Missing project_num argument." );
			return msg;
		}
	
		// DESIGNQUESTION error als de project_num null is?
		String projNumStr = json.get("project_num").toString() ;
		
		projNum = Integer.parseInt( projNumStr );
	
		// haal de index op van het project
		for( RenderAttributes attr : RenderAPI.projects ) {
			if(attr.projectNum == projNum  ) {
				index = RenderAPI.projects.indexOf(attr);
			}
		}
		
		// geef fout als project niet in de lijst staat
		if( index == -1 ) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Project not found. Project Num: " + projNum );
			return msg;
		} 

		// Haal de file metadata op uit het save request commando
		String fileName;
		String fileSize;
		
		long size = -1;
		
		fileName = json.get("file_name").toString();
		
		try {
			fileSize = json.get("file_size").toString();
		} catch ( NullPointerException e) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.UNKNOWNFILESIZE, projNumStr  );
			return msg;
		}
		
		// haal de size op
		// DESIGNQUESTION controleren op te grote files?
		size = Integer.parseInt(fileSize);
				
		// HAal de project metadata uit de lijst
		RenderAttributes data = RenderAPI.projects.get(index);
			
		// zet de data in de render lijst
		// DESIGNQUESTION is deze metadata eigelijk wel nodig?
		data.currentFile = fileName;
		data.currentFileSize = size;
		
		RenderAPI.projects.set(index, data);

		// start de thread voor het ophalen van de file
		FileSaveHandler fileThread;
		fileThread = new FileSaveHandler( size, projNumStr, fileName );

		fileThread.start();
		
		// wacht op de wait message
		while( msg == "" ) {
			msg = FileSaveHandler.getClientText();
		}
		FileSaveHandler.clearClientText();

		// Log de open port
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "FileHandler created. filesize: " + size + " port: " + fileThread.getActivePort() );
		
		return msg;
		
	}
	
	public String doCmdRecvFile(String arg) {
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "doCmdRecvFile" );		
		// Receive file metadata
		int index = -1;
		String msg = "";
		int projNum = -1;
		
		// probeer het JSON request te parsen
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			// handle errors
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , "" );
			return msg;
		}
		
		if ( json.containsKey( "project_num" ) == false ) {
			// contorleer project_num
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "Missing project_num argument." );
			return msg;
		}
	
		// DESIGNQUESTION error als de project_num null is?
		String projNumStr = json.get("project_num").toString() ;
		
		projNum = Integer.parseInt( projNumStr );
	
		// haal de index op van het project
		for( RenderAttributes attr : RenderAPI.projects ) {
			if(attr.projectNum == projNum  ) {
				index = RenderAPI.projects.indexOf(attr);
			}
		}
		
		// geef fout als project niet in de lijst staat
		if( index == -1 ) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Project not found. Project Num: " + projNum );
			return msg;
		} 

		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Haal file data.." );		
		
		// Haal de file metadata op uit het save request commando
		String fileName;
		String fileSize;
		
		long size = -1;
		
		fileName = json.get("file_name").toString();
		
		try {
			fileSize = json.get("file_size").toString();
		} catch ( NullPointerException e) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.UNKNOWNFILESIZE, projNumStr  );
			return msg;
		}

		// start de thread voor het ophalen van de file
		FileRecvHandler fileThread;
		fileThread = new FileRecvHandler( size, projNumStr, fileName );

		fileThread.start();
		
		// wacht op de wait message
		while( msg == "" ) {
			msg = FileRecvHandler.getClientText();
		}
		FileRecvHandler.clearClientText();

		// Log de open port
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "FileHandler created. filesize: " + size + " port: " + fileThread.getActivePort() );
		
		return msg;
		
	}


	public String doCmdAddProject(String arg) {
		// Add project
		String retval;
		
		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// Controleer op juiste JSON data.
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , "" );
			return retval;
		}
		
		// Controleer op project_name argument
		if ( json.containsKey( "project_name" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_name argument." );
			return retval;
		}

		// Controleer op project_num argument
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
		
		// Zet de projectnaam metadata
		String projName = "undefined";
		int projNum = -1;
		
		// converteer. 
		// DESIGNQUESTION json.get() kan ook een integer ophalen? 
		String projNumStr = json.get("project_num").toString() ;
		projNum = Integer.parseInt( projNumStr );		
		projName = json.get("project_name").toString();
		
		// Dan is deze misschien niet nodig
		if( projNum == -1 ) {
			retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTGETERR, "Could not convert Project Num." );
			return retval;
		}
		
		ServerLog.attachMessage( RenderAPI.MessageType.NOTICE, "Adding project \"" + projName + "\" number: " + projNum  );

		retval = cfg.addProject(projNum, projName);
				
		return retval;
	
	}

	public String doCmdGetProjects() {
		// genereer de projectenlijst en stuur JSON
		String msg = "";
		
		msg = "{";
		msg += "\"projects\": [ ";
		
		for( RenderAttributes attr: RenderAPI.projects ) {
			msg += " {\"project_num\": \"" + attr.projectNum + "\",";
			msg += "\"project_name\": \"" + attr.projectName + "\"},";
		}
		msg = msg.substring(0, msg.length() - 1);  
		msg += " ] } ";
		
		return msg;
	}
	
	public String doCmdSyncProjectFiles(String arg ) {
		String retval = "";

		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// Controleer op juiste JSON data.
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , arg );
			return retval;
		}
				
		// Controleer op project_num argument
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
				
		// Controleer of project_num beschikbaar is
		String projNumStr = json.get("project_num").toString() ;
		int projNum;
		projNum = Integer.parseInt( projNumStr );
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to sync project files: " + projNum );

		retval = cfg.syncProjectFiles( projNum );		
						
		return retval;
	}
	
	public String doCmdAddProjectFile(String arg ) {
		String retval = "";
		
		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// Controleer op juiste JSON data.
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , "" );
			return retval;
		}
		
		// Controleer op project_num argument
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
		
		// Controleer op file_name argument
		if ( json.containsKey( "file_name" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing file_name argument." );
			return retval;
		}
		
		// Controleer of project_num beschikbaar is
		String projNumStr = json.get("project_num").toString() ;
		int projNum;
		projNum = Integer.parseInt( projNumStr );
		
		// Haal file name op uit JSON
		String fileName = json.get("file_name").toString() ;
		
		retval = cfg.addProjectFile( projNum, fileName );
		return retval;
	}
	
	public String doCmdDelProjectFile(String arg ) {
		String retval = "";
		
		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// Controleer op juiste JSON data.
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , "" );
			return retval;
		}
		
		// Controleer op project_num argument
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
		
		// Controleer op file_name argument
		if ( json.containsKey( "file_name" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing file_name argument." );
			return retval;
		}
		
		// Controleer of project_num beschikbaar is
		String projNumStr = json.get("project_num").toString() ;
		int projNum;
		projNum = Integer.parseInt( projNumStr );
		
		// Haal file name op uit JSON
		String fileName = json.get("file_name").toString() ;
		
		retval = cfg.delProjectFile( projNum, fileName );
		return retval;
	}
	
	
	public String doCmdGetProjectFiles(String arg ) {
		String retval = "";

		JSONParser parser = new JSONParser();
		JSONObject json;
		
		// Controleer op juiste JSON data.
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.JSONINVALID , arg );
			return retval;
		}
		
		// Controleer op project_num argument
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
				
		// Controleer of project_num beschikbaar is
		String projNumStr = json.get("project_num").toString() ;
		int projNum;
		projNum = Integer.parseInt( projNumStr );
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to get project files: " + projNum );
	
		retval = cfg.getProjectFiles( projNum );
		
		return retval;
	}
	
	public String doCmdDeleteProject(String arg ) {
		// metadata
		String retval;
		int projNum = -1;
		int removeIndex = -1;
		
		// controleer of de JSON valide is
		JSONParser parser = new JSONParser();
		JSONObject json;	
		
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
			return retval;
		}
		
		// controleer of project_num gezet is
		if ( json.containsKey( "project_num" ) == false ) {
			retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return retval;
		}
		
		// haal de index uit de lijst
		String projNumStr = json.get("project_num").toString() ;		
		projNum = Integer.parseInt( projNumStr );
		
		for( RenderAttributes attr: RenderAPI.projects ) {
			if( attr.projectNum == projNum ) {
				removeIndex = RenderAPI.projects.indexOf(attr);
			}
		}
		
		// Remove index waar mogelijk
		if( removeIndex == -1) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Project " + projNumStr + " does not exists"); 
		} else {
			RenderAPI.projects.remove(removeIndex);		
			retval = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.PROJECTDELETE , projNumStr ); 
		}
		
		return retval;
	}
		
	public String doCmdGetRenderAttributes(String arg) {
		
		int index = -1;
		String msg = "undefined";
		int projNum = -1;
		
		// controleer of de JSON klopt
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
			return msg;
		}
		
		if ( json.containsKey( "project_num" ) == false ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return msg;
		}
	
		// Controleer of project_num beschikbaar is
		String projNumStr = json.get("project_num").toString() ;
		
		projNum = Integer.parseInt( projNumStr );
	
		// haal het project op uit de lijst
		for( RenderAttributes attr : RenderAPI.projects ) {
			if(attr.projectNum == projNum  ) {
				index = RenderAPI.projects.indexOf(attr);
				break;
			}
		}
		
		if( index == -1 ) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.GETATTRERR, "Project not found. Project Num: " + projNum );
		} else {
			// haal de render attributes uit RenderConfig
			msg = cfg.getRenderAttr(index);
		}
		
		return msg;	
	}
	 
	public String doCmdGetSystemStatus( String arg ) {
		return RenderAPI.systemOutJson();
	}

	public String doCmdDelProjectFromQueue(String arg ) {
		int index = -1;
		String msg = "";
		
		msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "Failing Command: " + arg );
		
		// Controleer of JSON klopt
		JSONParser parser = new JSONParser();
		JSONObject json;	
		
		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
			return msg;
		}
		
		// controleer project_num
		if ( json.containsKey( "id" ) == false ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.RENDERCMDFAIL, "Could not delete project. No id." );
			return msg;
		}
		
		int id;
		String idStr;
		
		idStr = json.get( "id" ).toString();
		
		id = Integer.parseInt(idStr);
		
		for( QueueAttributes attr: RenderAPI.queue ) {
			if( attr.id == id ) {
				index = RenderAPI.queue.indexOf(attr);
				break;
			}
		}
		
		if( index == -1 ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.RENDERCMDFAIL, "Could not delete project. Invalid Id: " + id );
			return msg;
		}
		
		QueueAttributes ret;
		
		ret = RenderAPI.queue.remove(index);
		
		msg = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.RENDERCMDOK, "Deleted project. Id: " + ret.id );
		return msg;

	}

	public static String escape( String input ) {
		String retval;
		retval = input.replaceAll("\"", "\\\"");
		return retval;
	}
	
	public static String doCmdGetProjectQueue() {

		String jsonMsg = "{ \"queue\" : [ ";
		boolean empty = true;
		for( QueueAttributes attr: RenderAPI.queue ) {
			empty = false;
			
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			
			jsonMsg += " { \"record\": { \"id\": " + attr.id + ", ";
			jsonMsg += " \"start\": \"" + formatter.format(attr.start)  + "\"  , ";
			jsonMsg += " \"aerender_exe\": \"" + escape(attr.attributes.aerenderExe) + "\"  , ";
			jsonMsg += " \"project_path\": \"" + escape(attr.attributes.projectPath) + "\"  , ";
			jsonMsg += " \"output_file\": \"" + escape(attr.attributes.outputFile) + "\"  , ";
			jsonMsg += " \"comp_name\": \"" + escape( attr.attributes.compositionName ) + "\"  , ";
			jsonMsg += " \"output_settings\": \"" + escape(attr.attributes.outputSettings) + "\"  , ";
			jsonMsg += " \"render_settings\": \"" + escape(attr.attributes.renderSettings) + "\" } },";

		}
			
		if( !empty ) {
			jsonMsg = jsonMsg.substring(0, jsonMsg.length() - 2);
			jsonMsg += " } ] }";
		} else {
			jsonMsg += "  ] }";
		}
		
		return jsonMsg;
	}

	public String doCmdAddProjectQueue(String arg ) {

		int index = -1;
		String msg = "";
		int projNum = -1;

		msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "Failing Command: " + arg );

		QueueAttributes attr = new QueueAttributes();
		
		// Controleer of JSON klopt
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
			return msg;
		}
		
		// controleer project_num
		if ( json.containsKey( "project_num" ) == false ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return msg;
		}
			
		String projNumStr = json.get("project_num").toString();
		
		projNum = Integer.parseInt( projNumStr );

				
		// haal het project op uit de lijst
		for( RenderAttributes attributes : RenderAPI.projects ) {
			if(attributes.projectNum == projNum  ) {
				index = RenderAPI.projects.indexOf(attributes);
				break;
			}
		}
		
		if( index == -1 ) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.GETATTRERR, "Project not found. Project Num: " + projNum );
			return msg;
		} 		

		
		attr.attributes.projectNum = projNum;
		
		attr.attributes.aerenderExe = RenderAPI.projects.get(index).aerenderExe;
		attr.attributes.projectPath = RenderAPI.projects.get(index).projectPath ;
		attr.attributes.compositionName = RenderAPI.projects.get(index).compositionName;
		attr.attributes.outputFile = RenderAPI.projects.get(index).outputFile;
		
		if(  RenderAPI.projects.get(index).renderSettings != ""  ) {
			attr.attributes.renderSettings = RenderAPI.projects.get(index).renderSettings;
		}
		if(  RenderAPI.projects.get(index).outputSettings != ""  ) {
			attr.attributes.outputSettings = RenderAPI.projects.get(index).outputSettings;
		}

		
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
		Date date = new Date();		
		String dateStr = formatter.format(date);
		
		// controleer project_num
		if ( json.containsKey( "start" ) == false ) {
			attr.start = date;
			ServerLog.attachMessage( RenderAPI.MessageType.WARNING, "No start time for render command: " + attr.id + " Defaulting to current time." );
		} else {
			dateStr = json.get("start").toString();
		}
		
		try {
			date = formatter.parse(dateStr);
			attr.start = date;
		} catch ( Exception e ) {
			ServerLog.attachMessage( RenderAPI.MessageType.ERROR, "Could not parse: " + dateStr + " " + e.getMessage() );
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.RENDERCMDFAIL , "Parse Error." );
			return msg;
		}
		
		RenderCommands.renderCommandCount++;
		
		attr.id = RenderCommands.renderCommandCount;
		
		RenderAPI.queue.add(attr);
					
		msg = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.RENDERCMDOK, "Render data added to queue.");
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Render Command (" + attr.id + "), Date (" + formatter.format(date) + ") Added."  );
			
		return msg;
	}
	
	public String doCmdSetRenderAttributes(String arg ) {
		int index = -1;
		String msg = "undefined";
		int projNum = -1;
		
		// Controleer of JSON klopt
		JSONParser parser = new JSONParser();
		JSONObject json;	

		try {
			json = (JSONObject) parser.parse(arg);
		} catch (Exception e ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.JSONINVALID, "" );
			return msg;
		}
		
		// controleer project_num
		if ( json.containsKey( "project_num" ) == false ) {
			msg = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTGETERR, "Missing project_num argument." );
			return msg;
		}
	
		
		String projNumStr = json.get("project_num").toString();
		
		projNum = Integer.parseInt( projNumStr );
		
		// haal de index op uit de lijst
		for( RenderAttributes attr : RenderAPI.projects ) {
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "output_settings: \"" + "attr: " + attr.projectNum + " projNum: " + projNum + "\"");
			if(attr.projectNum == projNum  ) {
				index = RenderAPI.projects.indexOf(attr);
				break;
			}
		}
		
		// als project niet gevonden
		if( index == -1 ) {
			msg = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Project not found. Project Num: " + projNum );
			return msg;
		} 
		
		// Render attributes
		String aerenderExe = RenderAPI.projects.get(index).aerenderExe;
		String projectPath = RenderAPI.projects.get(index).projectPath;
		String compositionName = RenderAPI.projects.get(index).compositionName;
		String renderSettings = RenderAPI.projects.get(index).renderSettings;
		String outputSettings = RenderAPI.projects.get(index).outputSettings;
		String outputFile = RenderAPI.projects.get(index).outputFile;
		
		String value;
		
		// probeer aerenderExe 
		try {
			value = json.get("aerender_exe").toString();

			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "aerender_exe: \"" + value + "\"" );
		
		} catch (NullPointerException e ) {
			value = aerenderExe;
		}
	
		cfg.setRenderAttribute(index, "aerender_exe", value);
		
		// probeer projectPath 
		try {
			value = json.get("project_path").toString();

			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "project_path: \"" + value + "\"");
			
		} catch (NullPointerException e ) {
			value = projectPath;
		}

		cfg.setRenderAttribute(index, "project_path", value);

		// probeer compositionName 
		try {
			value = json.get("composition_name").toString();
		
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "composition_name: \"" + value + "\"");

		} catch (NullPointerException e ) {
			value = compositionName;
		}

		cfg.setRenderAttribute(index, "composition_name", value);

		// probeer renderSettings 
		try {
			value = json.get("render_settings").toString();
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "render_settings: \"" + value + "\"");
		} catch (NullPointerException e ) {
			value = renderSettings;
		}

		cfg.setRenderAttribute(index, "render_settings", value);
		
		// probeer outputSettings 
		try {
			value = json.get("output_settings").toString();
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "output_settings: \"" + value + "\"");
		} catch (NullPointerException e ) {
			value = outputSettings;
		}

		cfg.setRenderAttribute(index, "output_settings", value);
				
		// probeer outputFile 
		try {
			value = json.get("output_file").toString();
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "output_file: \"" + value + "\"");
		} catch (NullPointerException e ) {
			value = outputFile;
		}
	
		cfg.setRenderAttribute(index, "output_file", value);

		// return message
		msg = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.GETATTR, projNumStr );
		
		return msg;
		
	}

	
	public String processCommand(String cmd, String arg  )	{
		String retval;
		
		retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.NOCMDARG, "No Such Command: " + cmd );

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "handling command: " + cmd);
		
		// Handle commando's 
		if (cmd.compareTo("add_project") == 0 ) {				
			retval = this.doCmdAddProject( arg ); 
		} else if (cmd.compareTo("get_projects") == 0 ) {
			retval = this.doCmdGetProjects();
		} else if (cmd.compareTo("del_project") == 0 ) {
			retval = this.doCmdDeleteProject( arg ); 
		} else if (cmd.compareTo("get_render_attributes") == 0 ) {
			retval = this.doCmdGetRenderAttributes( arg ); 
		} else if (cmd.compareTo("set_render_attributes") == 0 ) {
			retval = this.doCmdSetRenderAttributes( arg ); 
		} else if( cmd.compareTo( "send_file" ) == 0 ) {
			retval = this.doCmdSendFile(arg); 
		} else if( cmd.compareTo( "recv_file" ) == 0 ) {
			retval = this.doCmdRecvFile(arg); 
		} else if( cmd.compareTo( "add_project_file" ) == 0 ) {
			retval = this.doCmdAddProjectFile(arg); 
		} else if( cmd.compareTo( "del_project_file" ) == 0 ) {
			retval = this.doCmdDelProjectFile(arg); 
		} else if( cmd.compareTo( "get_project_files" ) == 0 ) {
			retval = this.doCmdGetProjectFiles(arg); 
		} else if( cmd.compareTo( "sync_project_files" ) == 0 ) {
			retval = this.doCmdSyncProjectFiles(arg); 
		} else if( cmd.compareTo( "get_system_status" ) == 0 ) {
			retval = this.doCmdGetSystemStatus(arg); 
		} else if( cmd.compareTo( "queue_add" ) == 0 ) {
			retval = this.doCmdAddProjectQueue(arg); 
		} else if( cmd.compareTo( "queue_get" ) == 0 ) {
			retval = this.doCmdGetProjectQueue(); 
		} else if( cmd.compareTo( "queue_del" ) == 0 ) {
			retval = this.doCmdDelProjectFromQueue(arg); 
		}

		RenderAPI.systemOutJson();
		
		return retval;
	}
	
}
