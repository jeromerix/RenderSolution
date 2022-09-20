package com.renderapi;

import java.util.ArrayList;

public class RenderAttributes {
	int projectNum;
	String projectName;
		
	// DESIGNQUESTION aerenderexe final maken, hard-coded?
	String aerenderExe = "c:\\Program Files\\Adobe\\Adobe After Effects 2020\\Support Files\\aerender.exe";
	String projectPath = "";
	String compositionName = "Comp 1";
	String renderSettings = "";
	String outputSettings = "";
	String outputFile = "";

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


