package com.mellanox;

import java.util.logging.Level;

public class ClientJXSession extends JXSessionBase{

	private static JXLog logger = JXLog.getLog(ClientJXSession.class.getCanonicalName());

	public ClientJXSession(JXMsgPool pool){
		super(pool, false);
	}

	// callbacks

	public void on_session_established_callback(){
		logger.log(Level.INFO, "new session created");
	}

	public void on_msg_received(final int msg_id, final char[] c){
		logger.log(Level.INFO, "new request arrived");
	}

	public void onRespRec(int msg_id, char[] c){
	}
}
