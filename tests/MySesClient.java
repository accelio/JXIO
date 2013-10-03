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

import java.util.logging.Level;

import com.mellanox.jxio.*;

public class MySesClient {

    private static Log logger = Log.getLog(MySesClient.class.getCanonicalName());

    EventQueueHandler eqh = null;
    ClientSession client;


    public MySesClient(EventQueueHandler eqh, String uri) {
	this.eqh = eqh;	
	this.client = new ClientSession (eqh, uri, new MySesClientCallbacks());
    }	    


	public boolean close() {
		this.client.close();
		logger.log(Level.INFO, "[SUCCESS] Session client successfully closed!" );
		return true;
	}

    public void sendMsg (Msg m){
	client.sendMessage(m);
    }



    class MySesClientCallbacks implements ClientSession.Callbacks {

	public void onMsgError(){	
	    logger.log(Level.INFO, "onMsgErrorCallback");
	}

	public void onSessionEstablished(){
	    logger.log(Level.INFO, "[SUCCESS] Session Established! Bring the champagne!");
	}

	public void onSessionError(int session_event, String reason ){

	    String event;
	    switch (session_event)
	    {
	    case 0:
		event = "SESSION_REJECT";
		break;
	    case 1:
		event = "SESSION_TEARDOWN";
		break;
	    case 2:
		event = "CONNECTION_CLOSED";
		// This is fine - connection closed by choice
		// there are two options: close session or reopen it
		break;
	    case 3:
		event = "CONNECTION_ERROR";
		//				this.close(); //through the bridge calls connection close
		break;
	    case 4:
		event = "SESSION_ERROR";
		break;
	    default:
		event = "UNKNOWN_EVENT";
		break;
	    }

	    logger.log(Level.SEVERE, "[EVENT] GOT EVENT " + event + " because of " + reason);

	}

	public void onReply(Msg msg){	
	    logger.log(Level.INFO, "[SUCCESS] Got a message! Bring the champagne!");
	    logger.log(Level.INFO, "num is " + msg.getIn().getInt());
	}


    }
}
