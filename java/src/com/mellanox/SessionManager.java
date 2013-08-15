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
	
	public SessionManager(String url, int port){
		eventQHndl = new EventQueueHandler (sizeEventQ);
		logger.log(Level.INFO, "url is "+url+" port is "+port);
		id = JXBridge.startServer(url, port, eventQHndl.getID());
		if (id == 0){
			logger.log(Level.SEVERE, "could not start server");
		}
		eventQHndl.addEventHandler (this); 
		eventQHndl.runEventLoop(1, 0);
	}
	
	public void close(){
		JXBridge.stopServer(id);
	}
	
	public void forward(SessionServer ses, long ptrSes){
		JXBridge.forwardSession(ses.url, ses.port, ptrSes);
	}
	
	
	public void run(){
		//event loop that waits for on_session events
		while (true) {
			System.out.println("SessionManager: before run event loop");
			eventQHndl.runEventLoop(1, 0);
		}
	}
	
	
	
	public void onEvent (int eventType, ByteBuffer buffer){
		switch (eventType){
		
		case 0: //session error event
			int errorType = buffer.getInt();
			int reason = buffer.getInt();
			String s = JXBridge.getError(reason);
			logger.log(Level.INFO, "received session error event "+errorType+" due to "+s);
			onSessionError(errorType, s);
			break;
			
		case 4: //on new session
			long ptrSes = buffer.getLong();
			String uri = readString(buffer);
			String ip = getIP(uri);			
			String srcIP = readString(buffer);
			
			onSession(ptrSes, ip, srcIP);
			break;
		
		default:
			logger.log(Level.SEVERE, "received an unknown event "+ eventType);
		}
	}
	
	private String getIP(String uri) {
//		rdma://36.0.0.121:1234
		int begin = uri.lastIndexOf("/");
		int end = uri.lastIndexOf(":");
		String ip = uri.substring(begin+1, end);
		logger.log(Level.INFO, "katya ip is "+ip);
		return ip;
	}

	private String readString (ByteBuffer buf){
		int len = buf.getInt();
		byte b[] = new byte[len+1];
		
		buf.get(b, 0, len);
		String s1 = new String(b, Charset.forName("US-ASCII"));

		return s1;
	}
	
	
	
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
