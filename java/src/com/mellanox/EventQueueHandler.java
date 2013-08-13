package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import com.mellanox.JXBridge;

public class EventQueueHandler {//implements Runnable{
	
	private int eventQueueSize; //how many events will the queue hold
	
	private long id = 0;
	private long evLoopID = 0;
	// Direct buffer to the registered memory
	ByteBuffer eventQueue = null;
	//this will be map in the future
	private Eventable eventable = null;
	int offset = 0;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
//	private static JOMXLog logger = JOMXLog.getLog(JOMXMsgPool.class.getCanonicalName());
	
	public long getID(){return id;}
	
	//c-tor
	public EventQueueHandler(int size){
		
		long [] ar;
		
		ar = JXBridge.createEQH();
		evLoopID = ar[0];
		id = ar[1];
		this.eventQueueSize = size;
		eventQueue = JXBridge.allocateEventQ(id, evLoopID, eventQueueSize);
	}
	
	public void addEventHandler(Eventable ev){
		//in the future will be add_to_map
		eventable = ev;
	}
	
	public void removeSesssion(Eventable ev){
		//in the future will remove from map
		eventable = null;
	}
	
	public int runEventLoop (int maxEvents, int timeOut){

//		while (true){
		int numEvents = JXBridge.getNumEventsQ(id);
		logger.log(Level.INFO, "there are "+numEvents+" events");
		for(int i=1; i<numEvents; i++){
			int eventType = eventQueue.getInt();
			eventable.onEvent(eventType, eventQueue);
		}
		eventQueue.rewind();
		
		JXBridge.runEventLoop(id);
		
		//start reading from ByteBuffer
		//TODO: convertToEnum
		int eventType = eventQueue.getInt();
		eventable.onEvent(eventType, eventQueue);

		
		//all events in queue are read in java side, so we must reset the offset of EventQueue buffer
		return 0;

	}
	

	public void close (){
		JXBridge.closeEQH(id, evLoopID);

	}


	public void run() {
		runEventLoop(1, 0);
		runEventLoop(1, 0);

	}
}
