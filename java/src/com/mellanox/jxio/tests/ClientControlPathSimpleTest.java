package com.mellanox.jxio.tests;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

import com.mellanox.jxio.*;


public class ClientControlPathSimpleTest {
	
	private static Log logger = Log.getLog(ClientControlPathSimpleTest.class.getCanonicalName());

	
	public static void main(String[] args) {
		
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl;
		
		int num_times = 1;
		
		eventQHndl = new EventQueueHandler();
		
		for (int i=0; i<num_times; i++) {
			
			ses = new MySesClient(eventQHndl, url);
//			eventQHndl.addEventable (ses);
			eventQHndl.runEventLoop(1000, -1 /* Infinite */);
//			eventQHndl.runEventLoop(1, 0);
			//for checking if server sends hello
			//for checking for event session tear down
			eventQHndl.runEventLoop(1, 0);
			
		}
		eventQHndl.close();
	}
}
