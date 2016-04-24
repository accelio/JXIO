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

import java.net.URI;

import org.accelio.jxio.ClientSession;
import org.accelio.jxio.EventName;
import org.accelio.jxio.EventQueueHandler;
import org.accelio.jxio.EventReason;
import org.accelio.jxio.Msg;
import org.accelio.jxio.WorkerCache;
import org.accelio.jxio.impl.Bridge;
import org.accelio.jxio.impl.Event;
import org.accelio.jxio.impl.EventMsgError;
import org.accelio.jxio.impl.EventNewMsg;
import org.accelio.jxio.impl.EventSession;
import org.accelio.jxio.impl.EventNameImpl;
import org.accelio.jxio.exceptions.JxioGeneralException;
import org.accelio.jxio.exceptions.JxioQueueOverflowException;
import org.accelio.jxio.exceptions.JxioSessionClosedException;

/**
 * ClientSession is the object that connects to the Server. This object initiates the connection.
 * The application uses it to send requests to the server and receive responses.
 * ClientSession receives several events on his lifetime. On each of them a method of interface
 * Callbacks is invoked. User must implement this interface and pass it in c-tor.
 * The events are:
 * 1. onSessionEstablished
 * 2. onSessionEvent
 * 3. onResponse
 * 4. onMsgError
 * 
 */
public class ClientSession extends EventQueueHandler.Eventable {

	private static final Log        LOG = LogFactory.getLog(ClientSession.class.getCanonicalName());
	private final Callbacks         callbacks;
	private final EventQueueHandler eqh;
	private final String            name;
	private final String            nameForLog;

	/**
	 * This interface needs to be implemented and passed to ClientSession in c-tor
	 * 
	 */
	public static interface Callbacks {
		/**
		 * Event triggered when response from server is received. Request and response are on the same
		 * {@link org.accelio.jxio.Msg} object. Once the user is done
		 * with the Msg he needs to call method msg.returnToParentPool()
		 * 
		 * @param msg
		 *            - the response message that was received. Msg object contains both request and Response
		 */
		public void onResponse(Msg msg);

		/**
		 * The client initiates a connection to Server in c-tor. When the connection is established,
		 * onSessionEstablished event is triggered. It is possible to call method sendRequest before receiving
		 * onSessionEstablished, however this will only add the requests to internal queue. They will be sent only after
		 * onSessionEstablished. In order to receive onSessionEstablished server must accept the session
		 * 
		 */
		public void onSessionEstablished();

		/**
		 * There are several types of session events: SESSION_CLOSED(because user called ClientSession.close(),
		 * Server initiated close or because of an internal error),
		 * SESSION_REJECTED (if Server chose to reject the session), SESSION_ERROR (due to internal error)
		 * 
		 * @param event
		 *            - the event that was triggered
		 * @param reason
		 *            - the object containing the reason for triggering event
		 */
		public void onSessionEvent(EventName event, EventReason reason);

		/**
		 * This event is triggered if there is an error in Msg send/receive. Once the user is done
		 * with the Msg he needs to call method msg.returnToParentPool()
		 * 
		 * @param msg
		 *            - send/receive of this Msg failed
		 * @param reason
		 *            - reason of the msg error
		 */
		public void onMsgError(Msg msg, EventReason reason);
	}

	/**
	 * Constructor of ClientSession.
	 * 
	 * @param eqh
	 *            - EventQueueHAndler on which the events
	 *            (onResponse, onSessionEstablished etc) of this client will arrive
	 * @param uri
	 *            - URI of the server to which this Client will connect
	 *            of the server
	 * @param callbacks
	 *            - implementation of Interface ClientSession.Callbacks
	 */
	public ClientSession(EventQueueHandler eqh, URI uri, Callbacks callbacks) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("CS CTOR entry");
		}
		this.eqh = eqh;
		this.callbacks = callbacks;
		if (!uri.getScheme().equals("rdma") && !uri.getScheme().equals("tcp")) {
			LOG.fatal("mal formatted URI: " + uri);
		}
		String uriStr = uri.toString();
		long cacheId = eqh.getId();
		if (uri.getPath().compareTo("") == 0) {
			uriStr += "/";
		}
		if (uri.getQuery() == null) {
			uriStr += "?" + WorkerCache.CACHE_TAG + "=" + cacheId;
		} else {
			uriStr += "&" + WorkerCache.CACHE_TAG + "=" + cacheId;
		}
		final long id = Bridge.startSessionClient(uriStr, eqh.getId());
		this.name = "jxio.CS[" + Long.toHexString(id) + "]";
		this.nameForLog = this.name + ": ";
		if (id == 0) {
			LOG.error(this.toLogString() + "there was an error creating session");
			return;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "connecting to " + uriStr);
		}
		this.setId(id);
		this.eqh.addEventable(this);

		if(!Bridge.connectSessionClient(this.getId())) {
		  LOG.error(this.toLogString() + "there was an error connecting session");
		  return;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "CS CTOR done");
		}
	}

	/**
	 * This method sends the request to server.
	 * <p>
	 * The send is asynchronous, therefore even if the function returns, this does not mean that the msg reached the
	 * server or even was sent to the server. The size send to Server is the current position of the OUT ByteBuffer
	 * 
	 * @param msg
	 *            - Msg to be sent to Server
	 * @throws JxioSessionClosedException
	 *             if session already closed. In case exception is thrown, msg needs to be returned to pool
	 * @throws JxioQueueOverflowException
	 *             if the queue overflowed when trying to send
	 * @throws JxioGeneralException
	 *             if send failed for any other reason *
	 */
	public void sendRequest(Msg msg) throws JxioGeneralException, JxioSessionClosedException, JxioQueueOverflowException {
		if (this.getIsClosing()) {
			LOG.debug(this.toLogString() + "Trying to send message while session is closing");
			throw new JxioSessionClosedException("sendRequest");
		}
		if (this.getId() == 0)
		{
			LOG.warn(this.toLogString() + "can not send, error in session create");
			throw new JxioGeneralException(EventReason.CONNECT_ERROR, "sendRequest");
		}
		final int in_size = msg.getIsMirror() ? msg.getMirror(false).getOut().limit() : msg.getIn().capacity();
		int ret = Bridge.clientSendReq(this.getId(), msg.getId(), msg.getOut().position(), in_size, msg.getIsMirror());
		if (ret > 0) {
			if (ret == EventReason.SESSION_DISCONNECTED.getIndex()) {
				LOG.debug(this.toLogString() + "message send failed because the session is already closed!");
				throw new JxioSessionClosedException("sendRequest");
			} else if (ret == EventReason.TX_QUEUE_OVERFLOW.getIndex()) {
				LOG.debug(this.toLogString() + "queue overflow occurred!");
				throw new JxioQueueOverflowException("sendRequest");
			} else {
				LOG.debug(this.toLogString() + "there was an error sending the message because of reason " + ret);
				LOG.debug(this.toLogString() + "unhandled exception. reason is " + ret);
				throw new JxioGeneralException(ret, "sendRequest");
			}
		}
		msg.setClientSession(this);
		// only if the send was successful the msg needs to be added to the "pending response" list
		eqh.addMsgInUse(msg);
	}

	/**
	 * This method closes the ClientSession.
	 * <p>
	 * The method is asynchronous: the ClientSession will be closed only when it receives event SESSION_CLOSED
	 * 
	 * @return true if there was a successful call to close of Client object on C side and false otherwise
	 */
	public boolean close() {
		if (this.getIsClosing()) {
			LOG.warn(this.toLogString() + "attempting to close client that is already closed or being closed");
			return false;
		}
		if (getId() == 0) {
			LOG.error(this.toLogString() + "closing Session with empty id");
			return false;
		}	
		setIsClosing(true);

		Bridge.closeSessionClient(getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug(this.toLogString() + "close() Done");
		}
		return true;
	}

	/**
	 * @returns true if a user callback was executed due to this event handling
	 */
	boolean onEvent(Event ev) {
		switch (ev.getEventType()) {

			case 0: // session error event
				if (ev instanceof EventSession) {

					int errorType = ((EventSession) ev).getErrorType();
					int reason = ((EventSession) ev).getReason();
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toLogString() + "Received Session Event: Type=" + errorType + ", Reason=" + reason);
					}
					EventNameImpl eventName = EventNameImpl.getEventByIndex(errorType);
					switch (eventName) {
						// Internal event
						case CONNECTION_CLOSED:
						case CONNECTION_DISCONNECTED:
						case CONNECTION_REFUSED:
							this.setIsClosing(true);
							return false;

						case SESSION_CLOSED:
							this.setIsClosing(true);
							break;

						// Internal event
						case SESSION_TEARDOWN:
							// now we are officially done with this session and it can be deleted from the EQH
							eqh.removeEventable(this);
							Bridge.deleteClient(this.getId());
							return false;

						default:
							break;
					}
					EventName eventNameForApp = EventName.getEventByIndex(eventName.getPublishedIndex());
					EventReason eventReason = EventReason.getEventByXioIndex(reason);
					try {
						callbacks.onSessionEvent(eventNameForApp, eventReason);
					} catch (Exception e) {
						eqh.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onSessionEvent] Callback exception occurred. Event was "
						        + eventName.toString());
					}
					return true;
				}
				break;

			case 2: // msg error
				EventMsgError evMsgErr;
				if (ev instanceof EventMsgError) {
					evMsgErr = (EventMsgError) ev;
					Msg msg = evMsgErr.getMsg();
					int reason = evMsgErr.getReason();
					if (LOG.isDebugEnabled()) {
						LOG.debug(this.toLogString() + "Received Msg Error Event: Msg=" + msg.toString() + ", Reason=" + reason);
					}
					EventReason eventReason = EventReason.getEventByXioIndex(reason);
					try {
						callbacks.onMsgError(msg, eventReason);
					} catch (Exception e) {
						eqh.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onMsgError] Callback exception occurred. Msg was "
						        + msg.toString());
					}
					return true;
				} else {
					LOG.error(this.toLogString() + "Event is not an instance of EventMsgError");
				}
				break;

			case 3: // session established
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.toLogString() + "Received Session Established Event");
				}
				try {
					callbacks.onSessionEstablished();
				} catch (Exception e) {
					eqh.setCaughtException(e);
					LOG.debug(this.toLogString() + "[onSessionEstablished] Callback exception occurred.");
				}
				return true;

			case 5: // on response
				EventNewMsg evNewMsg;
				if (ev instanceof EventNewMsg) {
					evNewMsg = (EventNewMsg) ev;
					Msg msg = evNewMsg.getMsg();
					if (LOG.isTraceEnabled()) {
						LOG.trace(this.toLogString() + "Received Msg Event: OnResponce Msg=" + msg.toString());
					}
					try {
						callbacks.onResponse(msg);
					} catch (Exception e) {
						eqh.setCaughtException(e);
						LOG.debug(this.toLogString() + "[onResponse] Callback exception occurred. Msg was "
						        + msg.toString());
					}
					return true;
				} else {
					LOG.error(this.toLogString() + "Event is not an instance of EventNewMsg");
				}

				break;

			default:
				break;
		}
		LOG.error(this.toLogString() + "Received an un-handled event " + ev.getEventType());
		return false;
	}

	boolean canClose() {
		return true;
	}

	public String toString() {
		return this.name;
	}

	private String toLogString() {
		return this.nameForLog;
	}
}
