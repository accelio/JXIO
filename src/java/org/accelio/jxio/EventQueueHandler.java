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
package org.accelio.jxio;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.Msg;
import org.accelio.jxio.MsgPool;
import org.accelio.jxio.impl.Bridge;
import org.accelio.jxio.impl.ElapsedTimeMeasurement;
import org.accelio.jxio.impl.Event;
import org.accelio.jxio.impl.EventMsgError;
import org.accelio.jxio.impl.EventNewMsg;
import org.accelio.jxio.impl.EventNewSession;
import org.accelio.jxio.impl.EventSession;
import org.accelio.jxio.impl.EventSessionEstablished;

/**
 * This class recieves events from accelio. It implements Runnable. Each EventQueueHandle should be run in a different
 * thread.
 * 
 */
public class EventQueueHandler implements Runnable {

	public static final int INFINITE_EVENTS = -1;
	public static final int INFINITE_DURATION = -1;
	private static final Log       LOG                   = LogFactory
	                                                             .getLog(EventQueueHandler.class.getCanonicalName());
	private final long             refToCObject;
	private final int              eventQueueSize        = 30000;
	private final Callbacks        callbacks;
	private int                    eventsWaitingInQ      = 0;
	private ByteBuffer             eventQueue            = null;
	private ElapsedTimeMeasurement elapsedTime           = null;
	private Map<Long, Eventable>   eventables            = new HashMap<Long, Eventable>();
	private Map<Long, Msg>         msgsPendingReply      = new HashMap<Long, Msg>();
	private Map<Long, Msg>         msgsPendingNewRequest = new HashMap<Long, Msg>();
	private volatile boolean       breakLoop             = false;
	private volatile boolean       stopLoop              = false;
	private volatile boolean       inRunLoop             = false;
	private volatile Exception     caughtException       = null;
	private final String           name;
	private final String           nameForLog;
	public volatile boolean        isClosing             = false;

	/**
     * This interface needs to be implemented and passed to EventQueueHandler in c-tor
     * 
     */
	public static interface Callbacks {
		/**
         * This callback is called on serverSide. If a request from client arrives and there are no more Msg list is
         * empty this callback is called. getAdditionalMsgPool should return a new unbinded MsgPool
         * 
         * @param inSize -
         *            size of Msg.IN
         * @param outSize -
         *            size of Msg.Out
         * @return an unbinded MsgPool
         */
		public MsgPool getAdditionalMsgPool(int inSize, int outSize);
	}

	/**
     * Constructor of EventQueueHandler
     * 
     * @param callbacks - -
     *            implementation of Interface EventQueueHandler.Callbacks
     */
	public EventQueueHandler(Callbacks callbacks) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("EQH CTOR entry");
		}
		DataFromC dataFromC = new DataFromC();
		Bridge.createCtx(this, eventQueueSize, dataFromC);
		this.eventQueue = dataFromC.eventQueue;
		this.refToCObject = dataFromC.getPtrCtx();
		this.elapsedTime = new ElapsedTimeMeasurement();
		this.callbacks = callbacks;
		this.name = "jxio.EQH[" + Long.toHexString(this.refToCObject) + "]";
		this.nameForLog = this.name + ": ";
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "EQH CTOR done");
		}
	}

	/**
     * Entry point for Thread.start() implementation from Runnable interfaces
     */
	public void run() {
		while (!this.stopLoop && !didExceptionOccur()) {
			runEventLoop(INFINITE_EVENTS, INFINITE_DURATION);
		}
		if (didExceptionOccur()) {
			// exception occurred
			LOG.debug(this.toLogString() + " The exception that occurred was:" + this.caughtException.toString());
			throw new RuntimeException(getCaughtException());
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
     * Main progress engine thread entry point. This function will cause all depending objects callbacks to be activated
     * respectfully on new event occur. the calling thread will block for 'maxEvents' or a total duration of
     * 'timeOutMicroSec.
     * 
     * @param maxEvents :
     *            function will block until processing max events (callbacks) before returning or the timeout reached
     *            use '-1' for infinite number of events
     * @param timeOutMicroSec :
     *            function will block until max duration of timeOut (measured in micro-sec) or maxEvents reached use
     *            '-1' for infinite duration
     * @return number of events processes, '0' if timeout or '-1' if exception occurred, which in this case, the exception
     *         itself can be accessed by calling getCaughtException().
     */
	public int runEventLoop(int maxEvents, long timeOutMicroSec) {
		if (getId() == 0) {
			LOG.error(this.toLogString() + "no context opened on C side. can not run event loop");
			return 0;
		}
		if (this.inRunLoop) {
			LOG.error(this.toLogString() + "event loop is already running");
			return 0;
		}
		this.inRunLoop = true;

		boolean is_forever = (timeOutMicroSec == INFINITE_DURATION) ? true : false;
		boolean is_infinite_events = (maxEvents == INFINITE_EVENTS) ? true : false;

		this.elapsedTime.resetStartTime();
		int eventsHandledByUser = 0;

		long remainingTimeOutMicroSec = timeOutMicroSec;
		while (!this.breakLoop && !this.didExceptionOccur() && ((is_infinite_events) || (maxEvents > eventsHandledByUser))
		        && ((is_forever) || (!this.elapsedTime.isTimeOutMicro(timeOutMicroSec)))) {

			if (is_forever == false) {
				remainingTimeOutMicroSec = timeOutMicroSec - this.elapsedTime.getElapsedTimeMicro();
				if (remainingTimeOutMicroSec < 0)
					remainingTimeOutMicroSec = 0;
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace(this.toLogString() + "in loop with " + eventsWaitingInQ + " events in Q. handled " + eventsHandledByUser + " events out of "
				        + maxEvents + ", " + "elapsed time is " + String.format("%,d", this.elapsedTime.getElapsedTimeMicro()) + " usec (blocking for "
				        + ((is_forever) ? "infinite duration)" : "a max duration of " + String.format("%,d", remainingTimeOutMicroSec) + " usec)"));
			}

    		if (handleQueueEvents(remainingTimeOutMicroSec))
    			eventsHandledByUser++;
		}

		this.breakLoop = false;
		if (LOG.isTraceEnabled()) {
			LOG.trace(this.toLogString() + "returning with " + eventsWaitingInQ + " events in Q. handled "
			        + eventsHandledByUser + " events, elapsed time is " + String.format("%,d", elapsedTime.getElapsedTimeMicro()) + " usec.");
		}
		this.inRunLoop = false;
		return !didExceptionOccur() ? eventsHandledByUser : -1;
	}

	/**
     * Main progress engine thread break point. Calling this function will force the runEventLoop() function to return
     * when possible, no matter the number of events or duration it still should be blocking.
     * 
     * This function can be called from any thread context
     */
	public void breakEventLoop() {
		if (getId() == 0) {
			LOG.error(this.toLogString() + "no context opened on C side. can not break event loop");
			return;
		}
		if (this.breakLoop == false) {
			this.breakLoop = true;

			Bridge.breakEventLoop(getId());
		}
	}

	/**  Close (and stops) this EQH and release all corresponding Java and Native resources
	 * (including closing the related ServerSessions, ServerPortal and ClientSession)
	 * 
	 * This function Should be called only once no other thread is inside the runEventLoop()
	 * 
	 * @return True if EQH was closed and false otherwise
	 */
	public boolean close() {
		if (getId() == 0) {
			LOG.error(this.toLogString() + "no context opened on C side. can not close event loop");
			return false;
		}
		if (this.inRunLoop) {
			LOG.error(this.toLogString() + "can not close EQH from within runEventLoop");
			return false;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "closing EQH ");
		}
		isClosing = true;
		while (!this.eventables.isEmpty()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(this.toLogString() + "attempting to close EQH while objects " + this.eventables.keySet()
				        + " are still listening");
			}
			int waitForEvent = 0;
			Iterator<Eventable> it = this.eventables.values().iterator();
			while (it.hasNext()) {
				Eventable ev = it.next();
				if (!ev.getIsClosing()) {
					if (ev.canClose()) {
						ev.close();
						if (LOG.isDebugEnabled())
							LOG.debug(this.toLogString() + "closing eventable" + ev.toString() + " with refToCObject " + ev.getId());
					} else {
						LOG.debug(this.toLogString() + "ERROR: User is using resources. object " + ev.toString() + "can not close");
						return false;
					}
				}
				waitForEvent++;
			}
			while (waitForEvent > 0) {
				handleQueueEvents(INFINITE_DURATION);
				waitForEvent--;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "no more objects listening");
		}
		Bridge.closeCtx(getId());
		this.stopLoop = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "closing EQH is finished");
		}
		return true;
	}

	static abstract class Eventable {

		private long    id        = 0;
		private boolean isClosing = false; // indicates that this class is in the process of releasing it's resources

		final long getId() {
			return id;
		}

		void setId(final long id) {
			if (this.id == 0)
				this.id = id;
			// TODO: 'else throw' exception instead of final member 'refToCObject'
		}

		public abstract boolean close();

		public boolean getIsClosing() {
			return isClosing;
		}

		void setIsClosing(boolean isClosing) {
			this.isClosing = isClosing;
		}
		
		/*
		 * @returns true if eventable can close and false if there are resources to be released by user before close
		 */
		abstract boolean canClose();
		
		/*
		 * @returns true if a user callback was executed due to this event handling
		 */
		abstract boolean onEvent(Event ev);
	}

	long getId() {
		return refToCObject;
	}

	void addEventable(Eventable eventable) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "adding " + Long.toHexString(eventable.getId()) + " to map");
		}
		// add lock
		synchronized (eventables) {
			if (eventable.getId() != 0) {
				eventables.put(eventable.getId(), eventable);
			}
		}
	}

	void removeEventable(Eventable eventable) {
		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "removing " + Long.toHexString(eventable.getId()) + " from map");
		}
		synchronized (eventables) {
			eventables.remove(eventable.getId());
		}
	}

	void addMsgInUse(Msg msg) {
		if (msg.getId() != 0) {
			msgsPendingReply.put(msg.getId(), msg);
		}
	}

	Msg getAndremoveMsgInUse(long id) {
		Msg msg = msgsPendingReply.remove(id);
		return msg;
	}

	/**
     * Internal event queue progress helper function. This function will cause all depending objects callbacks to be activated
     * respectfully on new event occur. the calling thread will handle a single event or block for a total duration of 'timeOutMicroSec' 
     * 
     * @param timeOutMicroSec :
     *            function will block until max duration of timeOut (measured in micro-sec) or at least 1 event was handled
     * @return 'true'  if a user callback was processed, 
     *         'false' if a timeout or an internal event was handled (no user callback)
     */
	private boolean handleQueueEvents(long timeOutMicroSec) {

		// process a pending events from the eventQueue 
		if (eventsWaitingInQ > 0) {
			boolean userCallackIssued = handleEvent(eventQueue);
			eventsWaitingInQ--;
			return userCallackIssued;
		}

		// the event queue is empty now, get more events from libxio or block for maxTimeout duration
		// if (eventsWaitingInQ <= 0) {
		eventQueue.rewind();
		eventsWaitingInQ = Bridge.runEventLoop(getId(), timeOutMicroSec);
		return false;
	}

	private boolean handleEvent(ByteBuffer eventQueue) {

		Eventable eventable;
		int eventType = eventQueue.getInt();
		long id = eventQueue.getLong();
		boolean userNotified = false;
		switch (eventType) {

			case 0: // session error event
			{
				int errorType = eventQueue.getInt();
				int reason = eventQueue.getInt();
				EventSession evSes = new EventSession(eventType, id, errorType, reason);
				synchronized (eventables) {
					eventable = eventables.get(id);
				}
				if (eventable == null) {
					LOG.warn(this.toLogString() + "eventable with id " + Long.toHexString(id) + " was not found in map");
					break;
				}
				userNotified = eventable.onEvent(evSes);
			}
			break;

			case 1: // msg error server
			{

				// msg was added to msgsPendingNewRequest after sendResponce. the real lookup of the Msg is done on C
				// side. msgsPendingNewRequest is used for look up of the java object based on the id
				Msg msg = this.msgsPendingNewRequest.remove(id);
				final long session_id = eventQueue.getLong();
				final int reason = eventQueue.getInt();
				eventable = eventables.get(session_id);
				if (eventable == null) {
					LOG.warn(this.toLogString() + "eventable with id " + Long.toHexString(session_id) + " was not found in map");
					break;
				}
				EventMsgError evMsgErr = new EventMsgError(eventType, id, msg, reason);
				userNotified = eventable.onEvent(evMsgErr);
			}
			break;

			case 2: // msg error client
			{
				Msg msg = msgsPendingReply.remove(id);
				final int reason = eventQueue.getInt();
				EventMsgError evMsgErr = new EventMsgError(eventType, id, msg, reason);
				eventable = msg.getClientSession();
				userNotified = eventable.onEvent(evMsgErr);

			}
			break;

			case 3: // session established
			{
				EventSessionEstablished evSesEstab = new EventSessionEstablished(eventType, id);
				eventable = eventables.get(id);
				if (eventable == null) {
					LOG.warn(this.toLogString() + "eventable with id " + Long.toHexString(id) + " was not found in map");
					break;
				}
				userNotified = eventable.onEvent(evSesEstab);
			}
			break;

			case 4: // on request (server side)
			{
				Msg msg = this.msgsPendingNewRequest.remove(id);
				msg.resetPositions();
				final int msg_in_size = eventQueue.getInt();
				msg.getIn().limit(msg_in_size);
				int msg_out_size = eventQueue.getInt();
				if (msg_out_size > msg.getOut().capacity())
					msg_out_size = msg.getOut().capacity();
				msg.getOut().limit(msg_out_size);
				final long session_id = eventQueue.getLong();
				if (LOG.isTraceEnabled()) {
					LOG.trace(this.toLogString() + "session refToCObject " + Long.toHexString(session_id));
				}
				eventable = eventables.get(session_id);
				if (eventable == null) {
					LOG.warn(this.toLogString() + "eventable with id " + Long.toHexString(session_id) + " was not found in map");
					break;
				}
				EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);
				userNotified = eventable.onEvent(evMsg);
			}
			break;

			case 5: // on response (client side)
			{
				Msg msg = msgsPendingReply.remove(id);
				final int msg_size = eventQueue.getInt();
				msg.getIn().limit(msg_size);
				if (LOG.isTraceEnabled()) {
					LOG.trace(this.toLogString() + "got msg " + msg);
				}
				EventNewMsg evMsg = new EventNewMsg(eventType, id, msg);
				eventable = msg.getClientSession();
				if (LOG.isTraceEnabled()) {
					LOG.trace(this.toLogString() + "eventable is " + eventable);
				}
				userNotified = eventable.onEvent(evMsg);
			}
			break;

			case 6: // on new session
			{
				long ptrSes = eventQueue.getLong();
				String uri = readString(eventQueue);
				String srcIP = readString(eventQueue);

				synchronized (eventables) {
					eventable = eventables.get(id);
				}
				if (eventable == null) {
					LOG.warn(this.toLogString() + "eventable with id " + Long.toHexString(id) + " was not found in map");
					break;
				}
				EventNewSession evNewSes = new EventNewSession(eventType, id, ptrSes, uri, srcIP);
				userNotified = eventable.onEvent(evNewSes);
			}
			break;

			case 8: // on fd ready
			{
				/*
                 * int fd = eventQueue.getInt(); int events = eventQueue.getInt();
                 */
				LOG.error(this.toLogString() + "received FD Ready event - not handled");
			}
			break;

			default:
				LOG.error(this.toLogString() + "received an unknown event " + eventType);
				// TODO: throw exception
		}
		return userNotified;
	}

	private String readString(ByteBuffer buf) {
		int len = buf.getInt();
		byte b[] = new byte[len];
		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));
		return s1;
	}

	public static class DataFromC {
		private long ptrCtx;
		ByteBuffer   eventQueue;

		DataFromC() {
			ptrCtx = 0;
			eventQueue = null;
		}

		public long getPtrCtx() {
			return ptrCtx;
		}
	}

	/**
	 * This method binds MsgPool to this EQH. It is necessary for MsgPool on server side to be binded to server's EQH
	 * 
	 * @param msgPool
	 *            to be binded to this EQH
	 * @return bool that indicate if the bind was successful
	 */

	public boolean bindMsgPool(MsgPool msgPool) {
		if (getId() == 0) {
			LOG.error(this.toLogString() + "no context opened on C side. can not bind msg pool");
			return false;
		}
		if (msgPool == null || msgPool.getId() == 0) {
			LOG.error(this.toLogString() + "msgPool provided is null or id is wrong. Can not bind");
			return false;
		}
		if (msgPool.isBounded()) {
			LOG.warn(this.toLogString() + "trying to bind MsgPool " + msgPool.toString() + ", but it's already bound");
			return false;
		}
		// the messages inside the pool must be added to hashmap, so that the appropraite msg can be tracked
		// once a request arrives
		ConcurrentLinkedQueue<Msg> msgArray = msgPool.getAllMsg();
		for (Msg msg : msgArray) {
			msgsPendingNewRequest.put(msg.getId(), msg);
		}
		boolean retVal = Bridge.bindMsgPool(msgPool.getId(), this.getId());
		if (retVal) {
			msgPool.setIsBounded(true);
		}
		return retVal;
	}

	void releaseMsgBackToPool(Msg msg) {
		// msg.resetPositions();
		this.msgsPendingNewRequest.put(msg.getId(), msg);
	}

	/**
	 * This method releases MsgPool from server's EQH (opposite of bindMsgPool)
	 * 
	 * @param msgPool
	 *            to be released
	 */

	public void releaseMsgPool(MsgPool msgPool) {
		// TODO implement!
	}

	public void getAdditionalMsgPool(int inSize, int outSize) {
		if (callbacks == null) {
			String fatalErrorStr = this.toLogString() + "user did not provide callback for providing additional buffers. Aborting!!!";
			LOG.fatal(fatalErrorStr);
			throw new RuntimeException(fatalErrorStr);
		}
		MsgPool pool = this.callbacks.getAdditionalMsgPool(inSize, outSize);
		if (pool == null) {
			String fatalErrorStr = this.toLogString() + "user failed to provide buffer. Aborting!!!"; 
			LOG.fatal(fatalErrorStr);
			throw new RuntimeException(fatalErrorStr);
		}
		if (pool.getInSize() < inSize) {
			String fatalErrorStr = this.toLogString() + "user failed to provide pool with correct sizes. Expected pool with in=" + 
					inSize + ". Got: pool with in=" + pool.getInSize() + ".Aborting!!!";
			LOG.fatal(fatalErrorStr);
			throw new RuntimeException(fatalErrorStr);
		}
		this.bindMsgPool(pool);
	}

	public String toString() {
		return this.name;
	}

	private String toLogString() {
		return this.nameForLog;
	}

	public Exception getCaughtException() {
		// Return the caught exception and clear it.
		Exception returnedExeption = null;
		if (this.caughtException != null) {
			returnedExeption = new Exception(this.caughtException);
			setCaughtException(null);
		}
		return returnedExeption;
	}

	public void setCaughtException(Exception caughtException) {
		this.caughtException = caughtException;
	}

	public boolean didExceptionOccur() {
		return (this.caughtException != null);
	}

	public boolean getInRunEventLoop() {
		return inRunLoop;
	}
}
