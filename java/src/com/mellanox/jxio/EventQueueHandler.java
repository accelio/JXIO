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
import com.mellanox.jxio.impl.Eventable;

public class EventQueueHandler implements Runnable {

	private final long id;
	private final Callbacks callbacks;
	private final int eventQueueSize = 5000; //size of byteBuffer
	private int eventsWaitingInQ = 0;
	private ElapsedTimeMeasurement elapsedTime = null; 
	private ByteBuffer eventQueue = null;
	private Map<Long,Eventable> eventables = new HashMap<Long,Eventable>();
	private Map<Long,Msg> msgsInUse = new HashMap<Long,Msg>();
	private boolean stopLoop = false;
	private static Log logger = Log.getLog(EventQueueHandler.class.getCanonicalName());

	public interface Callbacks {
		public abstract void onFdReady(long fd, int events, long priv_data);
	}

	//c-tor without callbacks
	public EventQueueHandler() {
		this(null); // no callbacks defined
	}

	//c-tor with callbacks
	public EventQueueHandler(Callbacks callbacks) {
		this.callbacks = callbacks;
		DataFromC dataFromC = new DataFromC();
		boolean statusError = Bridge.createCtx(eventQueueSize, dataFromC);
		if (statusError){
			logger.log(Level.INFO, "there was an error creating ctx on c side!");
		}
		this.eventQueue = dataFromC.eventQueue;
		this.id = dataFromC.ptrCtx;
		this.elapsedTime = new ElapsedTimeMeasurement(); 
	}

	public int addEventLoopFd(long fd, int events, long priv_data) {
		return Bridge.addEventLoopFd(getId(), fd, events, priv_data);
	}

	public int delEventLoopFd(long fd) {
		return Bridge.delEventLoopFd(getId(), fd);
	}

	public void run() {
		while (!this.stopLoop) {
			runEventLoop(1000, -1 /* Infinite */);
		}    
	}

	public int runEventLoop(int maxEvents, long timeOutMicroSec) {

		boolean is_forever = (timeOutMicroSec == -1) ? true : false;
		if (is_forever)
			logger.log(Level.INFO, "["+id+"] there are "+eventsWaitingInQ+" events in Q. requested to handle "+maxEvents+" max events, for infinite duration");
		else 
			logger.log(Level.INFO, "["+id+"] there are "+eventsWaitingInQ+" events in Q. requested to handle "+maxEvents+" max events, for a max duration of "+timeOutMicroSec/1000+" msec.");

		elapsedTime.resetStartTime();
		int eventsHandled = 0;

		while ((maxEvents > eventsHandled) && ((is_forever) || (!elapsedTime.isTimeOutMicro(timeOutMicroSec)))) {

			if (eventsWaitingInQ <= 0) { // the event queue is empty now, get more events from libxio
				eventQueue.rewind();
				eventsWaitingInQ = Bridge.runEventLoop(id, timeOutMicroSec);
			}

			// process in eventQueue pending events
			if (eventsWaitingInQ > 0) { // there are still events to be read, but they exceed maxEvents
				Event event = parseEvent(eventQueue);
				Eventable eventable;
				if (event instanceof EventNewMsg){
					eventable = getAndremoveMsgInUse(event.getId()).getClientSession();
				}else{
					eventable = eventables.get(event.getId());
				}
				eventable.onEvent(event);

				eventsHandled++;
				eventsWaitingInQ--;
			}

			logger.log(Level.INFO, "["+id+"] there are "+eventsWaitingInQ+" events in Q. handled "+eventsHandled+" events, elapsed time is "+ elapsedTime.getElapsedTimeMicro()+" usec.");
		}

		logger.log(Level.INFO, "["+id+"] returning with "+eventsWaitingInQ+" events in Q. handled "+eventsHandled+" events, elapsed time is "+ elapsedTime.getElapsedTimeMicro()+" usec.");
		return eventsHandled;
	}

	public void close() {
		while (!this.eventables.isEmpty()) {
			for (Map.Entry<Long,Eventable> entry : eventables.entrySet())
			{
				Eventable ev = entry.getValue();
				if (!ev.isClosing()){
					logger.log(Level.INFO, "closing eventable with id "+entry.getKey()); 
					ev.close();
				}
			}
			runEventLoop(1,-1);
			logger.log(Level.WARNING, "attempting to close EQH while objects "+this.eventables.keySet()+" are still listening. aborting");
			//			runEventLoop (1,0);
		}
		logger.log(Level.INFO, "no more objects listening");
		Bridge.closeCtx(id);
		this.stopLoop = true;
	}

	public void stopEventLoop() {
		this.stopLoop = true;
		Bridge.stopEventLoop(id);
	}

	protected long getId() { return id; }

	protected void addEventable(Eventable eventable) {
		logger.log(Level.INFO, "** adding "+eventable.getId()+" to map ");
		if (eventable.getId() != 0){
			eventables.put(eventable.getId(), eventable);
		}
	}

	protected void removeEventable(Eventable eventable) {
		logger.log(Level.INFO, "** removing "+eventable.getId()+" from map ");
		eventables.remove(eventable.getId());
	}

	protected void addMsgInUse(Msg msg) {
		if (msg.getId() != 0){
			msgsInUse.put(msg.getId(), msg);
		}
	}

	protected Msg getAndremoveMsgInUse(long id) {
		Msg msg = msgsInUse.get(id);
		msgsInUse.remove(id);
		return msg;
	}

	private Event parseEvent(ByteBuffer eventQueue) {
		int eventType = eventQueue.getInt();
		long id = eventQueue.getLong();

		switch (eventType) {

		case 0: //session error event
		{
			int errorType = eventQueue.getInt();
			int reason = eventQueue.getInt();
			String s = Bridge.getError(reason);
			EventSession evSes = new EventSession(eventType, id, errorType, s);
			return evSes;
		}	
		case 1: //msg error
			EventMsgError evMsgErr = new EventMsgError(eventType, id);
			return evMsgErr;

		case 2: //session established
			EventSessionEstablished evSesEstab = new EventSessionEstablished(eventType, id);
			return evSesEstab;

		case 3: //on reply
			EventNewMsg evMsg = new EventNewMsg(eventType, id);
			return evMsg;

		case 4: //on new session
			long ptrSes = eventQueue.getLong();
			String uri = readString(eventQueue);		
			String srcIP = readString(eventQueue);			

			EventNewSession evNewSes = new EventNewSession(eventType, id, ptrSes, uri, srcIP);
			return evNewSes;

		case 6: //on fd ready
			int fd = eventQueue.getInt();		
			int events = eventQueue.getInt();			
			this.callbacks.onFdReady(fd, events, 0);
			return null;

		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
			return null;
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
}
