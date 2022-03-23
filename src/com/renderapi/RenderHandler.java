package com.renderapi;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RenderHandler  extends Thread  {
	
	QueueAttributes attributes;
	
	public RenderHandler() {
	}
	
	@Override
	public void run() 
	{
		
		int i;
		QueueAttributes tmpAttr;
		
		for( i = 0; i < RenderAPI.queue.size(); i++ ) {
			tmpAttr = RenderAPI.queue.get(i);
			String execCmd;

			Date attrDate;
			Date now;
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			
			now = new Date();
			attrDate = tmpAttr.start;
			if (now.compareTo(attrDate) > 0 && tmpAttr.renderActive == false) {
			    tmpAttr.renderActive = true;
				execCmd = tmpAttr.getCommandStr();
				Thread t = new ExecHandler(execCmd, tmpAttr.id);
				t.start();
			} 			
		}
					
				
	}
}
