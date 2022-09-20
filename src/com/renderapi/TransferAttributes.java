package com.renderapi;

import java.net.*;

public class TransferAttributes {
	ServerSocket sock;
	String fileName;
	long fileSize;
	int port;
	public TransferAttributes( ServerSocket s, int port, String fileName, long fileSize ) {
		this.sock = s;
		this.port = port;
		this.fileName = fileName;
		this.fileSize = fileSize;
	}
	int getPort() {
		return port;
	}
	long getSize() {
		return fileSize;
	}
	String getName() {
		return fileName;
	}
}

