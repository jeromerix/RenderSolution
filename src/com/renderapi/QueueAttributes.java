package com.renderapi;
import java.util.ArrayList;

import java.text.SimpleDateFormat;  
import java.util.Date;  

public class QueueAttributes {
	int id;
	Date start;
	RenderAttributes attributes;
	
	public QueueAttributes() {
		attributes = new RenderAttributes();
	}
}
