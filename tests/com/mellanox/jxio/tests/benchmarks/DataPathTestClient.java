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

package com.mellanox.jxio.tests.benchmarks;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.EventName;


public class DataPathTestClient implements Runnable {

	EventQueueHandler        eqh;
	ClientSession            cs;
	MsgPool                  msgPool;

	private int  msgSize;
	private long cnt, print_cnt;
	boolean firstTime;
	long startTime;
	private final static Log LOG = LogFactory.getLog(DataPathTestClient.class.getCanonicalName());

	public DataPathTestClient(String uriString, int msg_size) {
		msgSize = msg_size;
		eqh = new EventQueueHandler();
		
		URI uri = null;
	    try {
	    	uri = new URI(uriString);
	    } catch (URISyntaxException e) {
	    	// TODO Auto-generated catch block
	    	e.printStackTrace();
	    }
		cs =  new ClientSession(eqh, uri, new DataPathClientCallbacks());
		msgPool = new MsgPool(2048, msgSize, msgSize);
		int msgKSize = msgSize/1024;
	    if(msgKSize == 0) {
	    	print_cnt = 4000000;
	    }
	    else {
	    	print_cnt = 4000000/msgKSize;
	    	System.out.println("print_cnt = " + print_cnt);
	    }
		cnt = 0;
		firstTime=true;
	}

	public void run() {
		
		for (int i=0; i<512; i++) {
			Msg msg = msgPool.getMsg();
			if(msg == null) {
				print("Cannot get new message");
				break;
			}
			
			if(! cs.sendMessage(msg)) {
				print("Error sending");
				msgPool.releaseMsg(msg);
			}
		}
		
		eqh.runEventLoop(-1, -1);
	}
	
	public void print(String str) {
		LOG.debug("********" + str);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("StatMain $IP $PORT $MSG_SIZE");
			return;
		}
		String url = "rdma://" + args[0] + ":" + Integer.parseInt(args[1]);
		Runnable test = new DataPathTestClient(url, Integer.parseInt(args[2]));
		test.run();
	}
	
	class DataPathClientCallbacks implements ClientSession.Callbacks {

		public void onMsgError() {
			// logger.log(Level.debug, "onMsgErrorCallback");
			print("onMsgErrorCallback");
		}

		public void onSessionEstablished() {
			// logger.log(Level.debug, "[SUCCESS] Session Established");
			print(" Session Established");
		}

        public void onSessionEvent(EventName session_event, String reason) {
			print("GOT EVENT " + session_event.toString() + " because of " + reason);
		}

		public void onReply(Msg msg) {
			print("Got Reply");
			if(firstTime) {
				startTime = System.nanoTime();
				firstTime = false;
			}
			
			cnt++;
			
			if(cnt == print_cnt) {				
				long delta = System.nanoTime() - startTime;
				long pps = (cnt*1000000000)/delta;
				double bw = (1.0*pps*msgSize/(1024*1024));
				//System.out.println("delta = " + delta + " cnt = " + cnt);
				System.out.println("TPS = " + pps + " BW =" + bw + " msg_size =" +msgSize);
				
				cnt = 0;
				startTime = System.nanoTime();
			}
				
			if(! cs.sendMessage(msg)) {
				msgPool.releaseMsg(msg);
			}
		}
	}
}
