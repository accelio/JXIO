package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import com.mellanox.JXBridge;

public class EventQueueHandler {
	
	private int eventQueueSize = 1000; //size of byteBuffer
	
	private long id = 0;
	private int eventsRead = 0;
	// Direct buffer to the registered memory
	ByteBuffer eventQueue = null;
	//this will be map in the future
	private Eventable eventable = null;
	int offset = 0;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
	

	
	//c-tor
	public EventQueueHandler(int size){
		
		DataFromC dataFromC = new DataFromC();
		boolean statusError = JXBridge.createCtx(eventQueueSize, dataFromC);
		if (statusError){
			logger.log(Level.INFO, "there was an error creating ctx on c side!");
		}
		this.eventQueue = dataFromC.eventQueue;
		System.out.println("***"+this.eventQueue.toString());
		this.id = dataFromC.ptrCtx;
		
//		long [] ar = JXBridge.createEQH();
//		evLoopID = ar[0];
//		id = ar[1];
//		this.eventQueueSize = size;
//		eventQueue = JXBridge.allocateEventQ(id, evLoopID, eventQueueSize);
	}
	
	public void addEventHandler(Eventable ev){
		//in the future will be add_to_map
		eventable = ev;
	}
	
	public void removeSesssion(Eventable ev){
		//in the future will remove from map
		eventable = null;
	}
	
	public int runEventLoop2 (int maxEvents, int timeOut){

		int numEvents = JXBridge.getNumEventsQ(id);
		logger.log(Level.INFO, "there are "+numEvents+" events in queue. Max events is "+maxEvents);
		int eventsLeftInQ = numEvents - eventsRead;
		if (eventsLeftInQ <= maxEvents){ //all events from buffer must be read
			for(int i=0; i<eventsLeftInQ; i++){
				int eventType = eventQueue.getInt();
				eventable.onEvent(eventType, eventQueue);
			}
			eventQueue.rewind();
			eventsRead = 0;
			JXBridge.runEventLoop(id);
			}else { //there are more events in buffer than need to be read
				for(int i=0; i<maxEvents; i++){
					int eventType = eventQueue.getInt();
					eventable.onEvent(eventType, eventQueue);
				}
				eventsRead += maxEvents;
				
			}
		return maxEvents;
	}
	
	
	
	public int runEventLoop (int maxEvents, int timeOut){
        int numEvents = JXBridge.getNumEventsQ(id);
//		logger.log(Level.INFO, "there are "+numEvents+" events");
        System.out.println("there are "+numEvents+" events");
        for(int i=1; i<numEvents; i++){
                 int eventType = eventQueue.getInt();
//                  eventable.onEvent(event);
                eventable.onEvent(eventType, eventQueue);
		}
		eventQueue.rewind();

         JXBridge.runEventLoop(id);
         
         System.out.println("**just before");
		int eventType = eventQueue.getInt();
		System.out.println("**just after");
         eventable.onEvent(eventType, eventQueue);
         
         return 0;

	}
	

	public void close (){
		JXBridge.closeCtx(id);
	}
	
	
	public long getID(){return id;}

	
	//this function closes all sessions on this eqh
	public void stopAndClose(){
		
		JXBridge.stopEventLoop(id);
		//process all events in buffer
		 int numEvents = JXBridge.getNumEventsQ(id);
//		logger.log(Level.INFO, "there are "+numEvents+" events");
	     System.out.println("there are "+numEvents+" events");
	     for(int i=1; i<numEvents; i++){
	    	 int eventType = eventQueue.getInt();
	         eventable.onEvent(eventType, eventQueue);
		}
	     eventable.close();
	     close();
		
	}
	
	public class DataFromC{
		long ptrCtx;
		ByteBuffer eventQueue;
		
		DataFromC(){
			ptrCtx = 0;
			eventQueue = null;
		}
	}
	
	
}
