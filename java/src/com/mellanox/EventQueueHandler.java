package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import com.mellanox.JXBridge;

public class EventQueueHandler {
	
	protected static int sizeEvent = 64; //64 bytes for each event
	protected static int eventQueueSize = 1; //how many events will the queue hold
	
	private long id = 0;
	private long evLoopID = 0;
	// Direct buffer to the registered memory
	ByteBuffer eventQueue = null;
	boolean stopEventLoop = false;
	private SessionClient session = null;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
//	private static JOMXLog logger = JOMXLog.getLog(JOMXMsgPool.class.getCanonicalName());
	
	//c-tor
	public EventQueueHandler(){
		
		long [] ar;
		
		ar = JXBridge.createEQH();
		evLoopID = ar[0];
		id = ar[1];
		logger.log(Level.INFO, "createEQH returned with " + evLoopID + " and " + id);
		eventQueue = JXBridge.allocateEventQ(id, sizeEvent, eventQueueSize);
	}
	
	public void addSession(SessionClient ses){
		//in the future will be add_to_map
		session = ses;
	}
	
	public void removeSesssion(SessionClient ses){
		//in the future will remove from map
		session = null;
	}
	
	public int runEventLoop (int maxEvents, int timeOut){
		//TODO: check if there are still events in the event queue
		
		JXBridge.runEventLoop(evLoopID);
		//start reading from ByteBuffer
		int counter = 0;
		//TODO: when time out will be supported, the loop should count to actual_events and not max_events
		while (counter < maxEvents && !stopEventLoop){
			//TODO: convertToEnum
			int eventType = eventQueue.getInt();
			switch (eventType){
			case 0: //session established
				logger.log(Level.INFO, "received a session established event");
				session.onSessionEstablished();
				break;
			case 2: //session error
				
				break;
				
			default:
				logger.log(Level.SEVERE, "received a session established event");
				System.out.println("ERRROR: received a session established event");
			}
			counter++;
			
		}
		return 0;

	}
	
	public int breakEventLoop(){
		stopEventLoop = true;
		return 0;
	}

	public void close (){
		JXBridge.closeEQH(id, evLoopID);

	}
}
