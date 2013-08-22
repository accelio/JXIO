package com.mellanox;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import com.mellanox.Event.*;
import com.mellanox.JXBridge;

public class EventQueueHandler {
	
	private int eventQueueSize = 1000; //size of byteBuffer
	
	private long id = 0;
	private int eventsNotRead = 0;
	// Direct buffer to the registered memory
	ByteBuffer eventQueue = null;
	//this will be map in the future
//	private Eventable eventable = null;
	
//	Map eventables = new HashMap();
	
	
	Map <Long,Eventable> eventables = new HashMap<Long,Eventable>();
//	int offset = 0;
	protected boolean stopLoop = false;
	
	private static JXLog logger = JXLog.getLog(EventQueueHandler.class.getCanonicalName());
	
	
	
	//c-tor
	public EventQueueHandler(int size){
		
		DataFromC dataFromC = new DataFromC();
		boolean statusError = JXBridge.createCtx(eventQueueSize, dataFromC);
		if (statusError){
			logger.log(Level.INFO, "there was an error creating ctx on c side!");
		}
		this.eventQueue = dataFromC.eventQueue;
		this.id = dataFromC.ptrCtx;
		

	}
	
	public void addEventable(Eventable eventable){
		logger.log(Level.INFO, "** adding "+eventable.getId()+" to map ");
		eventables.put(eventable.getId(), eventable);
	}
	
	public void removeEventable(Eventable eventable){
		logger.log(Level.INFO, "** removing "+eventable.getId()+" from map ");
		eventables.remove(eventable.getId());
	}
	
	/*
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
	}*/
	
	
	
	public int runEventLoop (int maxEvents, int timeOut){
		logger.log(Level.INFO, "there are "+eventsNotRead+" events");
        for(int i=0; i<eventsNotRead; i++){
                 int eventType = eventQueue.getInt();
                 long id = eventQueue.getLong();
                 System.out.println("***** event is "+eventType);
         		Event event = parseEvent(eventType, eventQueue);
         		Eventable eventable = eventables.get(id);
         		logger.log(Level.INFO, "** eventable "+eventable+" id "+ id);
                eventable.onEvent(eventType, event);
		}
		eventQueue.rewind();
		eventsNotRead = 0;
		
		eventsNotRead = JXBridge.runEventLoop(id);
         
		for(int i=0; i<eventsNotRead; i++){
            int eventType = eventQueue.getInt();
            long id = eventQueue.getLong();
            System.out.println("***** event is "+eventType);
            Event event = parseEvent(eventType, eventQueue);
     		Eventable eventable =  eventables.get(id);
     		logger.log(Level.INFO, "** eventable "+eventable+" id "+ id);
            eventable.onEvent(eventType, event);
		}
		eventsNotRead = 0;
         return 0;

	}
	

	public void close (){
		if (!this.eventables.isEmpty()){
			logger.log(Level.WARNING, "attempting to close EQH while objects are still listening. aborting");
			return;
		}
		logger.log(Level.INFO, "no more objects listening");
		JXBridge.closeCtx(id);
		this.stopLoop = true;
	}
	
	
	public long getID(){return id;}

	
	//this function closes all sessions on this eqh
	public void stopAndClose(){
		
		JXBridge.stopEventLoop(id);
		//process all events in buffer
		 int numEvents = JXBridge.getNumEventsQ(id);
//		logger.log(Level.INFO, "there are "+numEvents+" events");
	     System.out.println("there are "+numEvents+" events");
	     for(int i=0; i<numEvents; i++){
	    	int eventType = eventQueue.getInt();
	    	long id = eventQueue.getLong();
	    	Event event = parseEvent(eventType, eventQueue);
      		Eventable eventable = eventables.get(id);
            eventable.onEvent(eventType, event);
		}
	     
	     for (Map.Entry<Long,Eventable> entry : eventables.entrySet())
	     {
	        entry.getValue().close();
	     }
 
	    
	     close();
		
	}
	
	public Event parseEvent(int eventType, ByteBuffer eventQueue){
		
		switch (eventType){

		case 0: //session error event
		{
			int errorType = eventQueue.getInt();
			int reason = eventQueue.getInt();
			String s = JXBridge.getError(reason);
			EventSession evSes = new EventSession(errorType, s);
			return evSes;
		}	
		case 1: //msg error
			EventMsgError evMsgErr = new EventMsgError();
			return evMsgErr;

		case 2: //session established
			EventSessionEstablished evSesEstab = new EventSessionEstablished();
			return evSesEstab;
			
		case 3: //on reply
			EventNewMsg evMsg = new EventNewMsg();
			return evMsg;
			
		case 4: //on new session
			long ptrSes = eventQueue.getLong();
			String uri = readString(eventQueue);		
			String srcIP = readString(eventQueue);			
			
			EventNewSession evNewSes = new EventNewSession(ptrSes, uri, srcIP);
			return evNewSes;
			
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
			return null;
		}
		
		
		
	}
	
	
	private String readString (ByteBuffer buf){
		int len = buf.getInt();
		byte b[] = new byte[len+1];
		
		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));

		return s1;
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
