package com.renderapi;

import java.io.*;
import java.net.*;

import java.util.ArrayList;

public class FileRecvHandler extends Thread {

	String project;
	String file;
	
	// Constructor
	public FileRecvHandler( String projNumStr, String fileName ) 
	{
		this.project = projNumStr;
		this.file = fileName;			
	}

	
}
