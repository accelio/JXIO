package com.mellanox;


import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.mellanox.JXBridge;


public abstract class SessionBase implements Eventable {
	
//	private EventQueueHandler eventQHandler = null;
	private long ptrEventQueue = 0;
	
	protected int type;
	
	protected String url;
	protected int port;
	
	private static JXLog logger = JXLog.getLog(SessionBase.class.getCanonicalName());
	
	protected SessionBase (long eqh, String url, int port, int type){
	}
	
	abstract public void onSessionErrorCallback(int session_event, String reason );
	abstract public void onMsgErrorCallback();
	
	
}
