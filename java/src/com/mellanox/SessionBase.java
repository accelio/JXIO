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
