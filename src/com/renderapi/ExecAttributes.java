package com.renderapi;

public class ExecAttributes {
	String uuid;
	String startTime;
	String endTime;
	String progressTime;
	int queueIndex;
	boolean active;
	boolean finished;
	
	public ExecAttributes( String uuid, String startTime, String endTime, String progressTime, boolean active) {
		this.uuid = uuid;
		this.startTime = startTime;
		this.endTime = endTime;
		this.progressTime = progressTime;
		this.active = active;
		this.finished = false;
	}
}
