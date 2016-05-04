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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.EventName;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.Msg;
import org.accelio.jxio.ServerPortal;
import org.accelio.jxio.impl.Bridge;
import org.accelio.jxio.impl.Event;
import org.accelio.jxio.impl.EventMsgError;
import org.accelio.jxio.impl.EventNewMsg;
import org.accelio.jxio.impl.EventSession;
import org.accelio.jxio.impl.EventNameImpl;
import org.accelio.jxio.exceptions.JxioGeneralException;
import org.accelio.jxio.exceptions.JxioSessionClosedException;
/**
 * ServerSession is the object which receives Msgs from Client and sends responses. This side
 * does not initiate connection. ServerSession receives several events on his lifetime.
 * On each of them a method of interface Callbacks is invoked.
 * User must implement this interface and pass it in c-tor.
 * The events are:
 * 1. onRequest
 * 2. onSessionEvent
 * 3. onMsgError
 * 
 */
public class ServerSession extends EventQueueHandler.Eventable {

	/*
	 * events that are session related will arrive on eqh that received the original
	 * onSessionNew events. msg events will arrive on the eqh to which the session was
	 * forwarded
	 */
	private final Callbacks   callbacks;
	private final String      uri;
	private final String      name;
	private final String      nameForLog;
	private EventQueueHandler eqhMsg;
	private EventQueueHandler eqhSession;
	private ServerPortal      creator;
	private int               msgsInUse;
	private boolean 		  receivedClosed = false;
	private static final Log  LOG = LogFactory.getLog(ServerSession.class.getCanonicalName());

	public static interface Callbacks {
		/**
		 * This event is triggered when a request from Client is received.
		 * Server should send the response on the same {@link org.accelio.jxio.Msg} object.
		 * 
		 * @param msg
		 *            containing Client's request
		 */
		public void onRequest(Msg msg);

		/**
		 * There are several types of session events: SESSION_CLOSED(because user called ServerSession.close(),
		 * Client initiated close or because of an internal error),
		 * SESSION_ERROR (due to internal error)
		 * 
		 * @param event
		 *            - the event that was triggered
		 * @param reason
		 *            - the object containing the reason for triggering event
		 */
		public void onSessionEvent(EventName event, EventReason reason);

		/**
		 * This event is triggered if there is an error in Msg send/receive. The method returns true
		 * if the Msg should be released automatically once onMsgError finishes and false if
		 * the user will release it later with method returnOnMsgError.
		 * 
		 * @param msg
		 *            - send/receive of this Msg failed
		 * @param reason
		 *            - reason of the msg error
		 * @return true if the Msg should be released automatically once onMsgError finishes and false
		 *         if the user will release it later with method returnOnMsgError.
		 */
		public boolean onMsgError(Msg msg, EventReason reason);
	}

	/**
	 * Constructor of ServerSession. This object should be created after ServerPortal receives
	 * callback onNewSession.
	 * 
	 * @param sessionKey
	 *            - was received in ServerPortal's callback onNewSession
	 * @param callbacks
	 *            - implementation of Interface ServerSession.Callbacks
	 */
	public ServerSession(SessionKey sessionKey, Callbacks callbacks) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("SS CTOR entry");
		}
		this.callbacks = callbacks;
		setId(sessionKey.getSessionPtr());
		this.uri = sessionKey.getUri();
		this.name = "jxio.SS[" + Long.toHexString(getId()) + "]";
		this.nameForLog = this.name + ": ";

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "listening to " + sessionKey.getUri());

			LOG.debug(this.toLogString() + "SS CTOR done");
		}
	}

	/**
	 * This method closes the ServerSession.
	 * <p>
	 * The method is asynchronous: the ServerSession will be closed only when it receives event SESSION_CLOSED
	 * 
	 * @return true if there was a successful call to close of the ServerSession object on C side and false otherwise
	 */
	public boolean close() {
		if (this.getIsClosing()) {
			LOG.warn(this.toLogString() + "attempting to close server session that is already closed or being closed");
			return false;
		}
		if (getId() == 0) {
			LOG.error(this.toLogString() + "closing ServerSession with empty id");
			return false;
		}
		Bridge.closeServerSession(getId());
		setIsClosing(true);
		
		if (LOG.isDebugEnabled())
			LOG.debug(this.toLogString() + "close() Done successfully");

		return true;
	}

	/**
	 * This method sends the response to client.
	 * <p>
	 * The send is asynchronous, therefore even if the function returns, this does not mean that the msg reached the client or even was sent to the
	 * client. The size send to Client is the current position of the OUT ByteBuffer
	 * 
	 * @param msg
	 *            - Msg to be sent to Client
	 * @throws JxioSessionClosedException if session already closed. In case exception is thrown, discardRequest 
	 * must be called for this message after SESSION_CLOSED	event is officially recieved. 
	 * @throws JxioGeneralException if send failed for any other reason
	 */
	public void sendResponse(Msg msg) throws JxioGeneralException, JxioSessionClosedException {
		if (this.getIsClosing()) {
			LOG.debug(this.toLogString() + "Trying to send message " + msg + "while session is closing");
			throw new JxioSessionClosedException("sendResponse");
		}
		if (this.getId() == 0){
			LOG.warn(this.toLogString() + "Trying to send message on non established session");
			throw new JxioGeneralException(EventReason.JXIO_GENERAL_ERROR, "sendResponse");
		}
		int ret = Bridge.serverSendResponse(msg.getId(), msg.getOut().position(), this.getId());
		if (ret > 0) {
			if (ret != EventReason.SESSION_DISCONNECTED.getIndex()) {
				LOG.debug(this.toLogString() + "there was an error sending the message because of reason " + ret);
				LOG.debug(this.toLogString() + "unhandled exception. reason is " + ret);
				throw new JxioGeneralException(ret, "sendResponse");
			} else {
				setIsClosing(true);
				LOG.debug(this.toLogString() + "message send failed because the session is already closed!");
				throw new JxioSessionClosedException("sendResponse");
			}
		}	
		this.msgsInUse--;
		/*
		 * this message should be released back to pool.
		 * even though the message might not reached the client yet, it's ok since this pool is
		 * used only for matching of id to object. the actual release to pool is done on c side
		 */
		this.eqhMsg.releaseMsgBackToPool(msg);
	}

	/**
	 * This method releases Msg to pool after onMsgError. In case the user returns false in
	 * onMsgError he needs to release the Msg back to pool once he is done with it (using returnOnMsgError)
	 * 
	 * @param msg
	 *            - msg to be released back to pool
	 */
	public void returnOnMsgError(Msg msg) {
		// the user finished with the Msg. It can now be released on C side
		Bridge.releaseMsgServerSide(msg.getId());
		this.eqhMsg.releaseMsgBackToPool(msg);
		this.msgsInUse--;
	}
	
	/** 
	 * This method releases Msg to pool once session is closing (getIsClosing()==true or 
	 * after SESSION_CLOSED event). This method should be called
	 * for msgs that were not in pool in time of SESSION_CLOSED event arrival 
	 * @param msg - msg to be discarded
	 * @return true if msg was discarded and false otherwise
	 */
	public boolean discardRequest(Msg msg) {
		if (this.msgsInUse == 0 || !this.getIsClosing())
			return false; //to avoid release twice of session server
		Bridge.discardRequest(msg.getId());
		this.eqhMsg.releaseMsgBackToPool(msg);
		this.msgsInUse--;
		if ((this.msgsInUse == 0) && this.getReceivedClosed()) {
			if (LOG.isDebugEnabled()) 
				LOG.debug(this.toLogString() + "all msgs were discarded. Can delete SessionServer");
			Bridge.destroyConnectionSessionServer(this.getId());
		}
		return true;
	}

	final String getUri() {
		return uri;
	}

	void setEventQueueHandlers(EventQueueHandler eqhS, EventQueueHandler eqhM) {
		this.eqhMsg = eqhM;
		this.eqhMsg.addEventable(this);
		this.eqhSession = eqhS;
		this.eqhSession.addEventable(this); // if eqhS==eqhM, EventQueueHandler.eventables will contain only
		                                              // one value
	}

	void setPortal(ServerPortal p) {
		this.creator = p;
	}

	boolean onEvent(Event ev) {
		switch (ev.getEventType()) {
			case 0: // session event
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toLogString() + "Received Session Event: Type=" + errorType + ", Reason=" + reason);
					}
					EventNameImpl eventName = EventNameImpl.getEventByIndex(errorType);
					switch (eventName) {
						// Internal events
						case FORWARD_COMPLETED:
							if (LOG.isDebugEnabled())
								LOG.debug(this.toLogString() + "got forward completed");
							if (eqhSession != eqhMsg)
								eqhSession.removeEventable(this);
							return false;
						
						case CONNECTION_CLOSED:
						case CONNECTION_DISCONNECTED:
							this.setIsClosing(true);
							return false;

						case SESSION_CLOSED:
							this.setIsClosing(true);
							this.setReceivedClosed(true);
							// now that the user knows session is closed, object holding session state can be deleted
							if (this.msgsInUse == 0) {
								if (LOG.isDebugEnabled())
									LOG.debug(this.toLogString() + "there are no msgs in use, can delete SessionServer");
								Bridge.destroyConnectionSessionServer(this.getId());
							} else {
								if (LOG.isDebugEnabled())
									LOG.debug(this.toLogString() + "there are still " + this.msgsInUse + " msgs in use. Can not delete SessionServer");
							}
							EventName eventNameForApp = EventName.getEventByIndex(eventName.getPublishedIndex());
							EventReason eventReason = EventReason.getEventByXioIndex(reason);
							try {
								callbacks.onSessionEvent(eventNameForApp, eventReason);
							} catch (Exception e) {
								eqhMsg.setCaughtException(e);
								LOG.debug(this.toLogString() + "[onSessionEvent] Callback exception occurred. Event was " + eventName.toString());
							}
							return true;

						//internal event
						case SESSION_TEARDOWN:
							// now we are officially done with this session and it can  be deleted from the EQH
							eqhMsg.removeEventable(this); 
							// need to delete this Session from the set in ServerPortal
							this.creator.removeSession(this);
							Bridge.deleteSessionServer(this.getId());
							return false;
						
						default:
							break;
						}
					LOG.error(this.toLogString() + "Received an un-hadnled event type = " + eventName);
				}
				break;

			case 1: // msg error
				EventMsgError evMsgErr;
				if (ev instanceof EventMsgError) {
					evMsgErr = (EventMsgError) ev;
					Msg msg = evMsgErr.getMsg();
					int reason = evMsgErr.getReason();
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toLogString() + "Received Msg Error Event: Msg=" + msg.toString() + ", Reason=" + reason);
					}
					EventReason eventReason = EventReason.getEventByXioIndex(reason);
					this.msgsInUse++;
					try {
						if (callbacks.onMsgError(msg, eventReason)) {
							// the user is finished with the Msg and it can be released
							this.returnOnMsgError(msg);
						}
					} catch (Exception e) {
						eqhMsg.setCaughtException(e);
						LOG.debug(this.toLogString() + " [onMsgError] Callback exception occurred. Msg was " + msg.toString());
					}
					return true;
				}
				LOG.error(this.toLogString() + "Event is not an instance of EventMsgError");
				break;

			case 4: // on request
				EventNewMsg evNewMsg;
				if (ev instanceof EventNewMsg) {
					evNewMsg = (EventNewMsg) ev;
					Msg msg = evNewMsg.getMsg();
					if (LOG.isTraceEnabled()) {
						LOG.trace(this.toLogString() + "Received Msg Event: onRequest Msg=" + msg.toString());
					}
					this.msgsInUse++;
					try {
						callbacks.onRequest(msg);
					} catch (Exception e) {
						eqhMsg.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onRequest] Callback exception occurred. Msg was " + msg.toString());
					}
					return true;
				}
				LOG.error(this.toLogString() + "Event is not an instance of EventNewMsg");
				break;

			default:
				break;
		}
		LOG.error(this.toLogString() + "Received an un-handled event " + ev.getEventType());
		return false;
	}

	boolean canClose() {
		if (this.msgsInUse == 0)
			return true;
		LOG.warn(this.toLogString() + "can't be closed. there are " + this.msgsInUse + " waiting to be discarded");
		return false;
	}
	
	public String toString() {
		return this.name;
	}

	private String toLogString() {
		return this.nameForLog;
	}

	private void setReceivedClosed(boolean receivedClosed) {
		this.receivedClosed = receivedClosed;
	}

	private boolean getReceivedClosed() {
		return receivedClosed;
	}

	/**
	 * This class holds the ID of a session. It is passed to user on onNewSession callback
	 * and passed to ServerSession's constructor. It contains id of the session request (long) and
	 * uri that the client wishes to connect to.
	 */
	public static class SessionKey {
		private final long sessionPtr;
		private final String uri;

		/**
		 * Returns id of the session request
		 * 
		 * @return id of the session request
		 */
		public long getSessionPtr() {
			return sessionPtr;
		}

		/**
		 * Returns uri that the client wishes to connect to
		 * 
		 * @return uri that the client wishes to connect to
		 */
		public String getUri() {
			return uri;
		}

		SessionKey(long sessionPtr, String uri) {
			this.sessionPtr = sessionPtr;
			this.uri = uri;
		}

	}
}
