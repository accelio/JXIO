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

import java.util.logging.Level;


public class JXIOBridge {


    static {  
		String s1=JXIOBridge.class.getProtectionDomain().getCodeSource().getLocation().getPath(); 
		String s2=s1.substring(0, s1.lastIndexOf("/")+1);
		Runtime.getRuntime().load(s2 + "libjx.so");
    }
	private static JXIOLog logger = JXIOLog.getLog(JXIOBridge.class.getCanonicalName());

	// Native methods and their wrappers start here
    
	
	private static native boolean createCtxNative(int eventQueueSize, Object dataFromC);
	static boolean createCtx(int eventQueueSize, Object dataFromC) {
		logger.log(Level.INFO, "invoking createCtxNative");
		boolean ret = createCtxNative(eventQueueSize, dataFromC);
		logger.log(Level.INFO, "finished createCtxNative");
		return ret;
		
	}
	private static native void closeCtxNative(long ptr);
		static void closeCtx(long ptr) {
		logger.log(Level.INFO, "invoking closeCtxNative");
		closeCtxNative(ptr);
		logger.log(Level.INFO, "finished closeCtxNative" );
			
	}
	private static native int runEventLoopNative(long ptr, long timeOutMicroSec);
	static int runEventLoop(long ptr, long timeOutMicroSec) {
	    logger.log(Level.INFO, "invoking Bridge.runEventLoopNative");
	    int ret = runEventLoopNative(ptr, timeOutMicroSec);
	    logger.log(Level.INFO, "finished Bridge.runEventLoopNative=" + ret);
	    return ret;
	}	
	private static native void stopEventLoopNative(long ptr);
	static void stopEventLoop(long ptr) {
	    logger.log(Level.INFO, "invoking stopEventLoopNative");
	    stopEventLoopNative(ptr);
	    logger.log(Level.INFO, "finished stopEventLoopNative");
	}

	private static native long startSessionClientNative(String url, long ptrCtx);
	static long startSessionClient(String url, long ptrCtx) {
		logger.log(Level.INFO, "invoking startSessionNative");
		long p = startSessionClientNative(url, ptrCtx);		
		logger.log(Level.INFO, "finished startSessionNative ");
		return p;
	}	
	private static native void closeSessionClientNative(long sesPtr);
	static void closeSessionClient(long sesPtr) {
		logger.log(Level.INFO, "invoking Bridge.closeSessionClient");
		closeSessionClientNative(sesPtr);
		logger.log(Level.INFO, "finished Bridge.closeSessionClient");
	}

	
	private static native long [] startServerNative(String url, long ptrCtx);
	static long [] startServer(String url, long ptrCtx) {
		logger.log(Level.INFO, "invoking startServerNative");
		long ptr [] = startServerNative(url, ptrCtx);
		logger.log(Level.INFO, "finished startServerNative");
		return ptr;
	}
	private static native boolean stopServerNative(long ptr);
	static boolean stopServer(long ptr) {
		logger.log(Level.INFO, "invoking stopServerNative");
		boolean ret = stopServerNative(ptr);
		logger.log(Level.INFO, "finished stopServerNative ret=" + ret);
		return ret;
	}

	private static native long forwardSessionNative(String url, long ptrSes, long ptrServer);
	static long  forwardSession(String url, long ptrSes, long ptrServer) {
		logger.log(Level.INFO, "invoking forwardSessionNative");
		long ptr = forwardSessionNative(url, ptrSes, ptrServer);
		logger.log(Level.INFO, "finished forwardSessionNative");
		return ptr;
	}
	
	
	/*
	private static native int getNumEventsQNative(long ptr);
	static int getNumEventsQ(long ptr) {
		logger.log(Level.INFO, "invoking getNumEventsQNative");
		int ret = getNumEventsQNative(ptr);
		logger.log(Level.INFO, "finished getNumEventsQNative");
		return ret;
	}
*/
	private static native String getErrorNative(int errorReason);
	static String getError(int errorReason) {
//		logger.log(Level.FINE, "invoking getErrorNative");
		String s = getErrorNative(errorReason);
//		logger.log(Level.FINE, "finished getErrorNative. error was "+s);
		return s;
	}
}
