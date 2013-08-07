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
	private SessionBase session = null;
	int offset = 0;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
//	private static JOMXLog logger = JOMXLog.getLog(JOMXMsgPool.class.getCanonicalName());
	
	public long getID(){return id;}
	
	//c-tor
	public EventQueueHandler(){
		
		long [] ar;
		
		ar = JXBridge.createEQH();
		evLoopID = ar[0];
		id = ar[1];
//		logger.log(Level.INFO, "createEQH returned with " + evLoopID + " and " + id);
		eventQueue = JXBridge.allocateEventQ(id, evLoopID, sizeEvent, eventQueueSize);
	}
	
	public void addSession(SessionBase ses){
		//in the future will be add_to_map
		session = ses;
	}
	
	public void removeSesssion(SessionBase ses){
		//in the future will remove from map
		session = null;
	}
	
	public int runEventLoop (int maxEvents, int timeOut){
		//check if there are still events in the event queue - for should be here??? and then rewind

		int numEvents = JXBridge.getNumEventsQ(id);
		logger.log(Level.INFO, "there are "+numEvents+" events");
		for(int i=1; i<numEvents; i++){
//			logger.log(Level.INFO, "for before: position is "+eventQueue.position());
			int eventType = eventQueue.getInt();
			session.onEvent(eventType, eventQueue);
			}
		eventQueue.rewind();
		
		
		JXBridge.runEventLoop(id);
		
		//start reading from ByteBuffer
//		int counter = 0;
		//TODO: when time out will be supported, the loop should count to actual_events and not max_events
//		while (counter < maxEvents && !stopEventLoop){
			//TODO: convertToEnum
			int eventType = eventQueue.getInt();
			session.onEvent(eventType, eventQueue);
//			counter++;
			
//		}
		
		
		//all events in queue are read in java side, so we must reset the offset of EventQueue buffer
//		logger.log(Level.INFO, "rewind before: position is "+eventQueue.position());
		
		
		return 0;

	}
	
	public int breakEventLoop(){
		stopEventLoop = true;
		return 0;
	}
	
	/*
	private void processEvent (int eventType){

		
		switch (eventType){
		case 0: //session established
			logger.log(Level.INFO, "received a session established event");
			session.onSessionEstablished();
			break;
		case 2: //session error
			int errorType = eventQueue.getInt();
			int reason = eventQueue.getInt();
			String s = JXBridge.getError(reason);
			session.onSessionErrorCallback(errorType, s);
			break;
		case 3: //on msg
			logger.log(Level.INFO, "received a new msg event");
			session.onReplyCallback();
			break;
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
			int i1 = eventQueue.getInt(0);
			int i2 = eventQueue.getInt(4);
			int i3 = eventQueue.getInt(8);
			logger.log(Level.SEVERE, "i1="+i1+" i2="+i2+" i3="+i3);
		}
	
	}
*/
	public void close (){
		JXBridge.closeEQH(id, evLoopID);

	}
}
