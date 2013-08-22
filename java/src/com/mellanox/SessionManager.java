package com.mellanox;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class SessionManager implements Eventable{
	private EventQueueHandler eventQHndl = null;
	private long id = 0;
	static protected int sizeEventQ = 10000;
	
	private static JXLog logger = JXLog.getLog(SessionManager.class.getCanonicalName());
	
	public abstract void onSession(long ptrSes, String uri, String srcIP);
	public abstract void onSessionError(int errorType, String reason);
	
	public SessionManager(String url){
		eventQHndl = new EventQueueHandler (sizeEventQ);
		id = JXBridge.startServer(url, eventQHndl.getID());
		if (id == 0){
			logger.log(Level.SEVERE, "could not start server");
		}
		eventQHndl.addEventable (this); 
		eventQHndl.runEventLoop(1, 0);
	}
	
	public boolean close(){
		eventQHndl.removeEventable (this); //TODO: fix this
		JXBridge.stopServer(id);
		return true;
	}
	
	public void forward(SessionServer ses, long ptrSes){
		JXBridge.forwardSession(ses.url, ptrSes);
	}
	
	
	public void run(){
		//event loop that waits for on_session events
		while (true) {
			System.out.println("SessionManager: before run event loop");
			eventQHndl.runEventLoop(1, 0);
		}
	}
	
	public long getId(){ return id;} //getId()
	
	
	public void onEvent (int eventType, Event ev){
		switch (eventType){
		
		case 0: //session error event
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventSession){
				int errorType = ((EventSession) ev).errorType;
				String reason = ((EventSession) ev).reason;
				this.onSessionError(errorType, reason);
			}
			break;
			
		case 4: //on new session
			logger.log(Level.INFO, "received session error event");
			if (ev  instanceof EventNewSession){
				long ptrSes = ((EventNewSession) ev).ptrSes;
				String uri = ((EventNewSession) ev).uri;		
				String srcIP = ((EventNewSession) ev).srcIP;
				this.onSession(ptrSes, uri, srcIP);
			}
			break;
		
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
	}
	
/*
	private String readString (ByteBuffer buf){
		int len = buf.getInt();
		byte b[] = new byte[len+1];
		
		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));

		return s1;
	}
	*/
	
	
	/*amir's code
	
	private static AtomicInteger id_counter ;
	private static JXSessionBase[] sessions ;
	private static SessionManager manager = null ;
	
	
	private SessionManager(int array_size){
		
		id_counter = new AtomicInteger(0);
		sessions = new JXSessionBase[array_size];
	}
	
	public static SessionManager getSessionManager(){
		if(manager==null){
			manager = new SessionManager(1000);
		}
		return manager;
	}
	
	public synchronized  int setSessionEntry(JXSessionBase s){
		int session_id = id_counter.getAndIncrement();
		if(session_id == sessions.length){
			enlargeArray();
		}
		sessions[session_id] = s;
		return session_id;
	}
	
	public void deleteSessionEntry(int session_id)
	{}
	
	public JXSessionBase getSession(int session_id){
		return sessions[session_id];
	}
	
	
	// to be implemented
	private void enlargeArray()
	{}

	*/
}
