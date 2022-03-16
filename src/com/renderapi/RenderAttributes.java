package com.renderapi;

import java.util.ArrayList;

public class RenderAttributes {
	int projectNum;
	String projectName;
		
	// DESIGNQUESTION aerenderexe final maken, hard-coded?
	String aerenderExe = "undefined";
	String projectPath = "undefined";
	String compositionName = "undefined";
	String renderSettings = "undefined";
	String outputSettings = "undefined";
	String outputFile = "undefined";

	String currentFile;
	long currentFileSize;
	
	// projectpath zou gestript moeten zijn, 
	// in vergelijking met de gegevens in deze lijst
	ArrayList<String> projectFiles;

	public RenderAttributes() {
		
		// maak lijst voor project files
		projectFiles = new ArrayList<String>();
		
	}
}


