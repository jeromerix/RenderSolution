package com.renderapi;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files
import java.net.*;

public class RenderConfig {
		
	public RenderConfig() {
		 		
	}

	public String addProjectFile( int num, String name ) {
		
		String retval = "";
		int index = -1;
		
		// Vertaal num (project_num uit de JSON) naar index.
		for( RenderAttributes attr: RenderAPI.projects ) {
			if( attr.projectNum == num ) {
				index = RenderAPI.projects.indexOf(attr);
				break;
			}
		}
		
		// Voeg file toe waar mogelijk
		if( index < 0) {
			retval = MessageHandler.prepareError( RenderAPI.NetworkErrorType.PROJECTGETERR, "Project " + num + " does not exists"); 
		} else {
			// Wijzig de RenderAttributes
			RenderAttributes attributes;
			
			// DESIGNQUESTION Schets maken van het geheugenmodel?
			attributes = RenderAPI.projects.get(index);
		
			File file = new File( attributes.projectPath + "/" + name );
			if( !file.exists() ) {
				retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILENOTFOUNDERR, attributes.projectPath + "/" + name  );
				return retval;
			}
			
			
			// Controleer of de file nog niet bestaat
			
			int exists;
			exists = attributes.projectFiles.indexOf(name);
			
			if( exists != -1) {
				retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTFILEADDERR, "Filename: " + name + " is already in list.");				
				return retval;
			}
			
			
			attributes.projectFiles.add(name);
			
			retval = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.PROJECTADDFILE, name );
		}		
		
		return retval;
	}
	
	public String delProjectFile( int num, String name ) {
		String retval = "";
		
		int indexProj = -1;
		
		// Vertaal num (project_num uit de JSON) naar index.
		for( RenderAttributes attr: RenderAPI.projects ) {
			if( attr.projectNum == num ) {
				indexProj = RenderAPI.projects.indexOf(attr);
				break;
			}
		}

		RenderAttributes attributes;
		int indexAttr = -1;
		
		// DESIGNQUESTION Schets maken van het geheugenmodel?
		attributes = RenderAPI.projects.get(indexProj);

		for( String projName: attributes.projectFiles ) {
			if( projName.compareTo(name) == 0 ) {
				indexAttr = attributes.projectFiles.indexOf(projName);
				break;
			}
		}

		File file = new File( attributes.projectPath + "/" + name );
		
		if( indexAttr == -1 ) {
			
			if( !file.exists() ) {
				retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.FILENOTFOUNDERR, attributes.projectPath + "/" + name  );
				return retval;
			}
		}
		
		boolean success;
		ArrayList<String> fileList;
		fileList = attributes.projectFiles;
		String fileToRemove;
		fileToRemove = attributes.projectFiles.get(indexAttr);
		
		success = fileList.remove( fileToRemove );
		
		if( !success ) {
			retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTDELERR, attributes.projectPath + "/" + name  );
			return retval;
		}
		
		// TODO delete file van server
		if ( file.delete() ) { 
			retval = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.PROJECTDELFILE, attributes.projectPath + "/" + name  );
		} else {
			retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTFILEDELERR, attributes.projectPath + "/" + name  );
		} 
				
		return retval;
	}

	public void listf(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    File[] fList = directory.listFiles();
	    if(fList != null) {
	        for (File file : fList) {      
	            if (file.isFile()) {
	                files.add(file);
	            } else if (file.isDirectory()) {
	                listf(file.getAbsolutePath(), files);
	            }
	        }
	    }
	}
	
	public String syncProjectFiles( int num ) {
		String retval = "";
				
		int index = -1;
		
		// Vertaal num (project_num uit de JSON) naar index.
		for( RenderAttributes attr: RenderAPI.projects ) {
			if( attr.projectNum == num ) {
				index = RenderAPI.projects.indexOf(attr);
				break;
			}
		}

		if( index == -1 ) {
			retval = MessageHandler.prepareError( 
					RenderAPI.NetworkErrorType.PROJECTSYNCERR, 
					"Project number: " 
					+ num 
					+ " is not in the project list"
				);
			
			return retval;
		} 

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to get index: " + index );

		RenderAttributes attributes = RenderAPI.projects.get(index);

		ArrayList<File> fileList = new ArrayList<File>();
		
		listf( attributes.projectPath, fileList );
		
		attributes.projectFiles.clear();

		for( File file: fileList ) {
			int len = attributes.projectPath.length();
			String foundFileName;
			foundFileName = file.getAbsolutePath().substring(len);
			
			attributes.projectFiles.add(foundFileName);
			
			ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "(sync) File: " + foundFileName );
			
		}
	
		
		retval = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.PROJECTSYNC, "" );
		
		
		return retval;
	}
	public String getProjectFiles( int num ) {
		String retval = "";
		int index = -1;
		
		// Vertaal num (project_num uit de JSON) naar index.
		for( RenderAttributes attr: RenderAPI.projects ) {
			if( attr.projectNum == num ) {
				index = RenderAPI.projects.indexOf(attr);
				break;
			}
		}

		if( index == -1 ) {
			retval = MessageHandler.prepareError( 
					RenderAPI.NetworkErrorType.PROJECTGETFILESERR, 
					"Project number: " 
					+ num 
					+ " is not in the project list"
				);
			
			return retval;
		} 

		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Trying to get index: " + index );

		RenderAttributes attributes = RenderAPI.projects.get(index);

		String msg = "";		
		msg = "{ ";
		msg += "\"project_files\": [ ";
		
		for( String projectFile: attributes.projectFiles ) {
			msg += "{\"file_name\": \"" + projectFile + "\"},";
		}
		
		msg = msg.substring(0, msg.length() - 1);  
		msg += " ] } \n";
		
		return msg;
	}
	
	public String addProject( int num, String name ) {
		// add projects
		// DESIGNQUESTION add project in deze class en get, delete in een andere class?
		// misschien bijelkaar zetten?
		int i;
		
		String retval;
	
		// nieuwe temp render attributes
		RenderAttributes tmp;
		tmp = new RenderAttributes();
		
		// controleer of project al bestaat
		for( i = 0; i < RenderAPI.projects.size(); i++  ) {
			tmp = RenderAPI.projects.get(i);
			if( tmp.projectNum == num  ) {
				retval = MessageHandler.prepareError(RenderAPI.NetworkErrorType.PROJECTADDERR, "Project " + num + " already exists." );
				return retval;
			}
		}
		
		// Voeg de renderattributes (lege class) toe
		// wijzig die later met seT_render_attributes etc.
		RenderAttributes attr;
		attr = new RenderAttributes();		
		attr.projectNum = num;
		attr.projectName = name;
		
		RenderAPI.projects.add(attr);
		
		retval = MessageHandler.prepareMessage(RenderAPI.NetworkMessageType.PROJECTADD, "" + num);
		
		return retval;
	}
	
	
	public void setRenderAttribute(int index, String key, String val ) {

		// zet een render attribute
		RenderAttributes data;
		data = RenderAPI.projects.get(index);
		
		if( key == "aerender_exe" ) {
			data.aerenderExe = val;
		}
		if( key == "project_path" ) {
			data.projectPath = val;
		}
		if( key == "composition_name" ) {
			data.compositionName = val;
		}
		if( key == "render_settings" ) {
			data.renderSettings= val;
		}
		if( key == "output_settings" ) {
			data.outputSettings = val;
		}
		if( key == "output_file" ) {
			data.outputFile = val;
		}
		RenderAPI.projects.set(index, data);
	}
	
	public String getRenderAttr( int index )
	{
		// Haal de JSON op met render attributes
		String msg;
		msg = "{ \"render_attributes\": ";
		msg += " { \"project_num\": " + RenderAPI.projects.get(index).projectNum + ", ";
		msg += "  \"aerender_exe\": \"" + RenderAPI.projects.get(index).aerenderExe + "\",";
		msg += "  \"project_path\": \"" + RenderAPI.projects.get(index).projectPath + "\",";
		msg += "  \"composition_name\": \"" + RenderAPI.projects.get(index).compositionName + "\",";
		msg += "  \"render_settings\": \"" + RenderAPI.projects.get(index).renderSettings + "\",";
		msg += "  \"output_settings\": \"" + RenderAPI.projects.get(index).outputSettings + "\",";
		msg += "  \"output_file\": \"" + RenderAPI.projects.get(index).outputFile + "\"} ";
		msg += " }";

		return msg;
		
	}
}
