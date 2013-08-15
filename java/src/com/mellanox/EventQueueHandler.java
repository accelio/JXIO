package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import com.mellanox.JXBridge;

public class EventQueueHandler {
	
	private int eventQueueSize; //how many events will the queue hold
	
	private long id = 0;
	private long evLoopID = 0;
	private int eventsRead = 0;
	// Direct buffer to the registered memory
	ByteBuffer eventQueue = null;
	//this will be map in the future
	private Eventable eventable = null;
	int offset = 0;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
	

	
	//c-tor
	public EventQueueHandler(int size){
		
		long [] ar = JXBridge.createEQH();
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
	
	public int runEventLoop2 (int maxEvents, int timeOut){

		int counter = 0;
		while (counter < maxEvents){
			int numEvents = JXBridge.getNumEventsQ(id);
			logger.log(Level.INFO, "there are "+numEvents+" events in queue. Max events is "+maxEvents);
			int eventsLeftInQ = numEvents - eventsRead;
			if (eventsLeftInQ <= maxEvents){ //all events from buffer must be read
				for(int i=0; i<eventsLeftInQ; i++){
					int eventType = eventQueue.getInt();
					eventable.onEvent(eventType, eventQueue);
				}
				counter += eventsLeftInQ;
				eventQueue.rewind();
				eventsRead = 0;
				JXBridge.runEventLoop(id);
			}else { //there are more events in buffer than need to be read
				for(int i=0; i<maxEvents; i++){
					int eventType = eventQueue.getInt();
					eventable.onEvent(eventType, eventQueue);
					counter++;
				}
				eventsRead += maxEvents;
				
			}
		}
		return maxEvents;
	}
	
	
	
	public int runEventLoop (int maxEvents, int timeOut){
        int numEvents = JXBridge.getNumEventsQ(id);
//		logger.log(Level.INFO, "there are "+numEvents+" events");
        System.out.println("there are "+numEvents+" events");
        for(int i=1; i<numEvents; i++){
                 int eventType = eventQueue.getInt();
                 eventable.onEvent(eventType, eventQueue);
		}
		eventQueue.rewind();

         JXBridge.runEventLoop(id);
         
		int eventType = eventQueue.getInt();
         eventable.onEvent(eventType, eventQueue);
         
         return 0;

	}
	

	public void close (){
		JXBridge.closeEQH(id, evLoopID);
	}
	
	
	public long getID(){return id;}
	
}
