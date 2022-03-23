package com.renderapi;
import java.util.ArrayList;

import java.text.SimpleDateFormat;  
import java.util.Date;  

public class QueueAttributes {
	int id;
	Date start;
	RenderAttributes attributes;
	
	boolean renderActive;
	boolean renderFinished;
	
	
	public QueueAttributes() {
		attributes = new RenderAttributes();
		renderActive = false;
		renderFinished = false;
	}

	
	String getCommandStr() {
		String retval;
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "attributes.projectName: " + attributes.projectName );
		retval =  "\"" + attributes.aerenderExe  + "\""; 
		retval += " -project \"" + attributes.projectPath + "/" + attributes.projectName + "\"";
		retval += " -comp \"" + attributes.compositionName + "\"";
		if( attributes.renderSettings != "" ) {
			retval += " -renderSettings \"" + attributes.renderSettings + "\" ";
		}
		if( attributes.outputSettings != "" ) {
			retval += " -outputSettings \"" + attributes.outputSettings + "\" ";			
		}
		retval += " -output \"" + attributes.projectPath  + "/" + attributes.outputFile + "\"";
		
		ServerLog.attachMessage( RenderAPI.MessageType.DEBUG, "Created Exec Command: " + retval );

		
		return retval;
	}
}
