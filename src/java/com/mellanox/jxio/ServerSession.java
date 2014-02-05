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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.impl.Bridge;
import com.mellanox.jxio.impl.Event;
import com.mellanox.jxio.impl.EventMsgError;
import com.mellanox.jxio.impl.EventNewMsg;
import com.mellanox.jxio.impl.EventSession;

/**
 * ServerSession is the object which receives Msgs from Client and sends responses. This side
 * does not initiate connection. ServerSession receives several events on his lifetime.
 * On each of them a method of interface Callbacks is invoked.
 * User must implement this interface and pass it in c-tor.
 * The events are:
 * 1. onRequest.
 * 2. onSessionEvent.
 * 3. onMsgError.
 * 
 */
public class ServerSession extends EventQueueHandler.Eventable {

	private final Callbacks   callbacks;
	/*
	 * events that are session related will arrive on eqh that received the original
	 * onSessionNew events. msg events will arrive on the eqh to which the session was
	 * forwarded
	 */
	private EventQueueHandler eventQHandlerMsg;
	private EventQueueHandler eventQHandlerSession;
	private long              ptrSesServer;
	private ServerPortal      creator;
	final String              uri;
	private static final Log  LOG = LogFactory.getLog(ServerSession.class.getCanonicalName());

	public static interface Callbacks {
		/**
		 * This event is triggered when a request from Client is received.
		 * Server should send the reply on the same {@link com.mellanox.jxio.Msg} object.
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
		 * @param session_event
		 *            - the event that was triggered
		 * @param reason
		 *            - the object containing the reason for triggerring session_event
		 */
		public void onSessionEvent(EventName session_event, EventReason reason);

		/**
		 * This event is triggered if there is an error in Msg send/receive. The method returns true 
		 * if the Msg should be released automatically once onMsgError finishes and false if
		 * the user will release it later with method returnOnMsgError. 
		 * 
		 * @param msg - send/receive of this Msg failed
		 * @param reason - reason of the msg error 
		 * @return true if the Msg should be released automatically once onMsgError finishes and false
		 * if the user will release it later with method returnOnMsgError. 
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
		this.callbacks = callbacks;
		setId(sessionKey.getSessionPtr());
		this.uri = sessionKey.getUri();
		if (LOG.isDebugEnabled()) {
			LOG.debug("id as recieved from C is " + getId());
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
			LOG.warn("attempting to close server session that is already closed or being closed");
			return false;
		}
		if (getId() == 0) {
			LOG.error("closing ServerSession with empty id");
			return false;
		}
		setIsClosing(true);
		Bridge.closeServerSession(ptrSesServer);

		return true;
	}

	/**
	 * This method sends the response to client.
	 * <p>
	 * The send is asynchronous, therefore even if the function returns, this does not mean that the msg reached the
	 * client or even was sent to the client. The size send to Client is the current position of the OUT ByteBuffer
	 * 
	 * @param msg
	 *            - Msg to be sent to Client
	 * @return true if queuing of the msg was successful and false otherwise
	 */
	public boolean sendResponse(Msg msg) {
		if (this.getIsClosing()) {
			LOG.warn("Trying to send message while session is closing");
			return false;
		}
		boolean ret = Bridge.serverSendResponse(msg.getId(), msg.getOut().position(), ptrSesServer);
		if (!ret) {
			LOG.debug("there was an error sending the message");
		}
		this.eventQHandlerMsg.releaseMsgBackToPool(msg);
		/*
		 * this message should be released back to pool.
		 * even though the message might not reached the client yet, it's ok since this pool is
		 * used only for matching of id to object. the actual release to pool is done on c side
		 */
		return ret;
	}

	
	/** This method releases Msg to pool after onMsgError. In case the user returns false in
	 * onMsgError he needs to release the Msg back to pool once he is done with it (using returnOnMsgError)
	 * 
	 * @param msg - msg to be released back to pool
	 */
	public void returnOnMsgError (Msg msg){
		//the user finished with the Msg. It can now be released on C side 
		Bridge.releaseMsgServerSide(msg.getId());
		this.eventQHandlerMsg.releaseMsgBackToPool(msg);
	}
	
	void setEventQueueHandlers(EventQueueHandler eqhS, EventQueueHandler eqhM) {
		this.eventQHandlerMsg = eqhM;
		this.eventQHandlerMsg.addEventable(this);
		this.eventQHandlerSession = eqhS;
		this.eventQHandlerSession.addEventable(this); // if eqhS==eqhM, EventQueueHandler.eventables will contain only
		                                              // one value
	}

	void setPortal(ServerPortal p) {
		this.creator = p;
	}

	void onEvent(Event ev) {
		switch (ev.getEventType()) {
			case 0: // session event
				if (LOG.isDebugEnabled()) {
					LOG.debug("received session event");
				}
				if (ev instanceof EventSession) {
					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					EventName eventName = EventName.getEventByIndex(errorType);
					if (eventName == EventName.SESSION_CLOSED) {
						removeFromEQHs(); // now we are officially done with this session and it can
						this.setIsClosing(true);// be deleted from the EQH
						// need to delete this Session from the set in ServerPortal
						this.creator.removeSession(this);
						// now that the user knows session is closed, object holding session state can be deleted
						Bridge.deleteSessionServer(this.ptrSesServer);
					}
					callbacks.onSessionEvent(eventName, EventReason.getEventByIndex(reason));

				}
				break;

			case 1: // msg error
				if (LOG.isDebugEnabled()){
					LOG.debug("received msg error event");
				}
				EventMsgError evMsgErr;
				if (ev instanceof EventMsgError) {
					evMsgErr = (EventMsgError) ev;
					Msg msg = evMsgErr.getMsg();
					int reason = evMsgErr.getReason();
					if (callbacks.onMsgError(msg, EventReason.getEventByIndex(reason))){
						//the user is finished with the Msg and it can be released
						this.returnOnMsgError(msg);
					}
				} else {
					LOG.error("Event is not an instance of EventMsgError");
				}
				break;

			case 4: // on request
				if (LOG.isTraceEnabled()) {
					LOG.trace("received msg event");
				}
				EventNewMsg evNewMsg;
				if (ev instanceof EventNewMsg) {
					evNewMsg = (EventNewMsg) ev;
					Msg msg = evNewMsg.getMsg();
					callbacks.onRequest(msg);
				} else {
					LOG.error("Event is not an instance of EventNewMsg");
				}

				break;

			default:
				LOG.error("received an unknown event " + ev.getEventType());
		}
	}

	private void removeFromEQHs() {
		eventQHandlerSession.removeEventable(this);
		if (eventQHandlerSession != eventQHandlerMsg) {
			eventQHandlerMsg.removeEventable(this);
		}
	}

	void setPtrServerSession(long ptrSesServer) {
		this.ptrSesServer = ptrSesServer;

	}

	/**
	 * This class holds the ID of a session. It is passed to user on onNewSession callback
	 * and passed to ServerSession's constructor. It contains id of the session request (long) and
	 * uri that the client wishes to connect to.
	 */
	public static class SessionKey {
		private final long   sessionPtr;
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
