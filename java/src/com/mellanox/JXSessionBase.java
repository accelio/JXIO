package com.mellanox;


import com.mellanox.JXBridge;

public class JXSessionBase {
	
	private JXMsgPool pool;
	private int id;

	
	public JXSessionBase(JXMsgPool _pool, boolean _isServer){
		pool = _pool;
		id = SessionManager.getSessionManager().setSessionEntry(this);
	}

	public void startSession(String hostname, int port) {
			JXBridge.startServerSession(hostname, port);
	}
	
	// send a message and return true on success or false otherwise
	public void send(byte[] b){
		}
	
	
	public int setSessionOpts() {
		return 0;
	}

	public int closeSession() {
		return 0;
	}

	// callback method to be implemented by derived classes 
	
	public void on_session_event_callback()
	{}

	public void on_new_session_callback()
	{}

	public void on_session_established_callback()
	{}

	public void on_session_redirected_callback()
	{}

	public void on_msg_send_complete_callback(char[] c)
	{}

	public void on_msg_received(int msg_id, char[] c)
	{}

	public void on_msg_error_callback()
	{}
	

}
