/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and  limitations under the License.
 **
 */
package com.mellanox.jxio;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.ElapsedTimeMeasurement;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventMsgError;
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventNewSession;
import com.mellanox.jxio.impl.EventSession;
import com.mellanox.jxio.impl.EventSessionEstablished;

/**
 *
 */
public class EventQueueHandler implements Runnable {

	private final long refToCObject;
	private final int eventQueueSize = 5000; //size of byteBuffer
	private int eventsWaitingInQ = 0;
	private ByteBuffer eventQueue = null;
	private ElapsedTimeMeasurement elapsedTime = null; 
	private Map<Long,Eventable> eventables = new HashMap<Long,Eventable>();
	private Map<Long,Msg> msgsPendingReply = new HashMap<Long,Msg>();
	private Map<Long, Msg> msgsPendingNewRequest = new HashMap<Long, Msg>();
	private volatile boolean stopLoop = false;
	private static final Log logger = Log.getLog(EventQueueHandler.class.getCanonicalName());

	// ctor
	public EventQueueHandler() {
		DataFromC dataFromC = new DataFromC();
		boolean statusError = Bridge.createCtx(eventQueueSize, dataFromC);
		if (statusError){
			logger.log(Level.INFO, "there was an error creating ctx on c side!");
		}
		this.eventQueue = dataFromC.eventQueue;
		this.refToCObject = dataFromC.ptrCtx;
		this.elapsedTime = new ElapsedTimeMeasurement(); 
	}

	/**
	 * Entry point for Thread.start() implementation from Runnable interfaces
	 */
	public void run() {
		while (!this.stopLoop) {
			runEventLoop(1000, -1 /* Infinite */);
		}    
	}

	/**
	 * Stops the running thread which is blocked on the run() interface
	 */
	public void stop() {
		this.stopLoop = true;
		breakEventLoop();
	}

	/**
	 * Main progress engine thread entry point. 
	 * This function will cause all depending objects callbacks to be activated respectfully on new event occur.
	 * the calling thread will block for 'maxEvents' or a total duration of 'timeOutMicroSec.
	 * @param maxEvents: function will block until processing max events (callbacks) before returning (or the timeout reached) 
	 * @param timeOutMicroSec: function will block until max duration of timeOut measured in micro-sec (or maxEvents reached)
	 * @return number of events processes or zero if timeout
	 */
	public int runEventLoop(int maxEvents, long timeOutMicroSec) {

		boolean is_forever = (timeOutMicroSec == -1) ? true : false;
		if (is_forever)
			logger.log(Level.INFO, "[" + getId() + "] there are " + eventsWaitingInQ + " events in Q. requested to handle " + maxEvents + " max events, for infinite duration");
		else 
			logger.log(Level.INFO, "[" + getId() + "] there are " + eventsWaitingInQ + " events in Q. requested to handle " + maxEvents + " max events, for a max duration of " + timeOutMicroSec/1000 + " msec.");

		elapsedTime.resetStartTime();
		int eventsHandled = 0;

		while ((maxEvents > eventsHandled) && ((is_forever) || (!elapsedTime.isTimeOutMicro(timeOutMicroSec)))) {

			if (eventsWaitingInQ <= 0) { // the event queue is empty now, get more events from libxio
				eventQueue.rewind();
				eventsWaitingInQ = Bridge.runEventLoop(getId(), timeOutMicroSec);
			}

			// process in eventQueue pending events

			if (eventsWaitingInQ > 0) { // there are still events to be read, but they exceed maxEvents
				handleEvent(eventQueue);
				eventsHandled++;
				eventsWaitingInQ--;

			}

			logger.log(Level.INFO, "[" + getId() + "] there are "+eventsWaitingInQ+" events in Q. handled " + eventsHandled+" events, elapsed time is "+ elapsedTime.getElapsedTimeMicro() + " usec.");
		}

		logger.log(Level.INFO, "[" + getId() + "] returning with "+eventsWaitingInQ+" events in Q. handled " + eventsHandled+" events, elapsed time is "+ elapsedTime.getElapsedTimeMicro() + " usec.");
		return eventsHandled;
	}

	/**
	 * Main progress engine thread break point.
	 * Calling this function will force the runEventLoop() function to return when possible, 
	 * no matter the number of events or duration it still should be blocking.
	 * 
	 * This function can be called from any thread context
	 */
	public void breakEventLoop() {
		Bridge.breakEventLoop(getId());		
	}

	/**
	 * Close (and stops) this EQH and release all corresponding Java and Native resources 
	 * (including closing the related SM, SS & CS)
	 * 
	 * This function Should be called only once no other thread is inside the runEventLoop()
	 */
	public void close() {
		while (!this.eventables.isEmpty()) {
			for (Map.Entry<Long,Eventable> entry : this.eventables.entrySet())
			{
				Eventable ev = entry.getValue();
				if (!ev.getIsClosing()){
					logger.log(Level.INFO, "closing eventable with refToCObject " + entry.getKey()); 
					ev.close();
				}
			}
			runEventLoop(1,-1);
			logger.log(Level.WARNING, "attempting to close EQH while objects " + this.eventables.keySet() + " are still listening. aborting");
			//			runEventLoop (1,0);
		}
		logger.log(Level.INFO, "no more objects listening");
		Bridge.closeCtx(getId());
		this.stopLoop = true;
	}

	static abstract class Eventable {

		private long id = 0;
		private boolean isClosing = false; //indicates that this class is in the process of releasing it's resources

		/*
		enum eventType {
			sessionError, msgError, sessionEstablished, msgRecieved,
		    newSession 
		} */

		final long getId() { 
			return id; 
		} 

		void setId(final long id) {
			if (this.id == 0)
				this.id = id;
			// TODO: 'else throw' exception instead of final member 'refToCObject'
		} 

		public abstract boolean close();

		boolean getIsClosing() { 
			return isClosing; 
		}

		void setIsClosing(boolean isClosing) {
			this.isClosing = isClosing; 
		}

		abstract void onEvent(Event ev);
	}

	long getId() { return refToCObject; }

	void addEventable(Eventable eventable) {
		logger.log(Level.INFO, "** adding "+eventable.getId()+" to map ");
		if (eventable.getId() != 0){
			eventables.put(eventable.getId(), eventable);
		}
	}

	void removeEventable(Eventable eventable) {
		logger.log(Level.INFO, "** removing "+eventable.getId()+" from map ");
		eventables.remove(eventable.getId());
	}

	void addMsgInUse(Msg msg) {
		if (msg.getId() != 0){
			msgsPendingReply.put(msg.getId(), msg);
		}
	}

	Msg getAndremoveMsgInUse(long id) {
		Msg msg = msgsPendingReply.get(id);
		msgsPendingReply.remove(id);
		return msg;
	}

	private void handleEvent(ByteBuffer eventQueue) {

		Eventable eventable;
		int eventType = eventQueue.getInt();
		long id = eventQueue.getLong();
		switch (eventType) {

		case 0: //session error event
		{
			int errorType = eventQueue.getInt();
			int reason = eventQueue.getInt();
			String s = Bridge.getError(reason);
			EventSession evSes = new EventSession(eventType, id, errorType, s);
			eventable = eventables.get(id);
			eventable.onEvent(evSes);
		}	
		break;
		
		case 1: //msg error
		{
			EventMsgError evMsgErr = new EventMsgError(eventType, id);
			eventable = eventables.get(id);
			eventable.onEvent(evMsgErr);
		}
		break;
		
		case 2: //session established
		{
			EventSessionEstablished evSesEstab = new EventSessionEstablished(eventType, id);
			eventable = eventables.get(id);
			eventable.onEvent(evSesEstab);
		}
		break;
		
		case 3: //on request
		{
			Msg msg = this.msgsPendingNewRequest.get(id);
			long session_id = eventQueue.getLong();
			logger.log(Level.INFO, "session refToCObject" +  session_id);
			eventable = eventables.get(session_id);
			EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);
			eventable.onEvent(evMsg);
		}
		break;
		
		case 4: //on reply
		{
			Msg msg = msgsPendingReply.remove(id);
			logger.log(Level.INFO, "msg is "+ msg);
			EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);		
			eventable = msg.getClientSession();
			logger.log(Level.INFO, "eventable is "+ eventable);
			eventable.onEvent(evMsg);
		}
		break;

		case 5: //on new session
		{
			long ptrSes = eventQueue.getLong();
			String uri = readString(eventQueue);		
			String srcIP = readString(eventQueue);			

			eventable = eventables.get(id);
			EventNewSession evNewSes = new EventNewSession(eventType, id, ptrSes, uri, srcIP);
			eventable.onEvent(evNewSes);
		}
		break;

		case 7: //on fd ready
		{
			int fd = eventQueue.getInt();		
			int events = eventQueue.getInt();			
			logger.log(Level.SEVERE, "received FD Ready event - not handled");
		}
		break;

		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
			//TODO: throw exception
		}

	}

	private String readString(ByteBuffer buf) {
		int len = buf.getInt();
		byte b[] = new byte[len+1];

		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));

		return s1;
	}

	private class DataFromC {
		long ptrCtx;
		ByteBuffer eventQueue;

		DataFromC() {
			ptrCtx = 0;
			eventQueue = null;
		}
	}

	public boolean bindMsgPool(MsgPool msgPool) {
		//the messages inside the pool must be added to hashmap, so that the appropraite msg can be tracked 
		//once a request arrives
		List<Msg> msgArray = msgPool.getAllMsg();
		for (Msg msg : msgArray) {
			msgsPendingNewRequest.put(msg.getId(), msg);
		}
		return Bridge.bindMsgPool(msgPool.getId(), this.getId());

	}

	void releaseMsgBackToPool(Msg msg) {
		this.msgsPendingNewRequest.put(msg.getId(), msg);

	}

	public void releaseMsgPool(MsgPool msgPool) {
		//TODO implement!
	}
}
