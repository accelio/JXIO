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
package com.mellanox.jxio.impl;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Bridge {

    static {  
		String s1=Bridge.class.getProtectionDomain().getCodeSource().getLocation().getPath(); 
		String s2=s1.substring(0, s1.lastIndexOf("/")+1);
		Runtime.getRuntime().load(s2 + "libjx.so");
    }
    private static final Log LOG = LogFactory.getLog(Bridge.class.getCanonicalName());

	// Native methods and their wrappers start here	
	private static native boolean createCtxNative(int eventQueueSize, Object dataFromC);
	public static boolean createCtx(int eventQueueSize, Object dataFromC) {
		LOG.trace("invoking createCtxNative");
		boolean ret = createCtxNative(eventQueueSize, dataFromC);
		LOG.trace("finished createCtxNative");
		return ret;
	}
	private static native void closeCtxNative(long ptr);
	public static void closeCtx(long ptr) {
		LOG.trace("invoking closeCtxNative");
		closeCtxNative(ptr);
		LOG.trace("finished closeCtxNative" );
	}
	private static native int runEventLoopNative(long ptr, long timeOutMicroSec);
	public static int runEventLoop(long ptr, long timeOutMicroSec) {
	    LOG.trace("invoking Bridge.runEventLoopNative");
	    int ret = runEventLoopNative(ptr, timeOutMicroSec);
	    LOG.trace("finished Bridge.runEventLoopNative=" + ret);
	    return ret;
	}	
	private static native void breakEventLoopNative(long ptr);
	public static void breakEventLoop(long ptr) {
	    LOG.trace("invoking breakEventLoopNative");
	    breakEventLoopNative(ptr);
	    LOG.trace("finished breakEventLoopNative");
	}
	private static native int addEventLoopFdNative(long ptr, long fd, int events, long priv_data);
	public static int addEventLoopFd(long ptr, long fd, int events, long priv_data) {
	    LOG.trace("invoking addEventLoopFdNative");
	    int ret = addEventLoopFdNative(ptr, fd, events, priv_data);
	    LOG.trace("finished addEventLoopFdNative");
	    return ret;
	}
	private static native int delEventLoopFdNative(long ptr, long fd);
	public static int delEventLoopFd(long ptr, long fd) {
	    LOG.trace( "invoking delEventLoopFdNative");
	    int ret = delEventLoopFdNative(ptr, fd);
	    LOG.trace("finished delEventLoopFdNative");
	    return ret;
	}

	private static native long startSessionClientNative(String url, long ptrCtx);
	public static long startSessionClient(String url, long ptrCtx) {
		LOG.trace("invoking startSessionNative");
		long p = startSessionClientNative(url, ptrCtx);		
		LOG.trace("finished startSessionNative ");
		return p;
	}	
	private static native void closeSessionClientNative(long sesPtr);
	public static void closeSessionClient(long sesPtr) {
		LOG.trace("invoking Bridge.closeSessionClient");
		closeSessionClientNative(sesPtr);
		LOG.trace("finished Bridge.closeSessionClient");
	}
	
	private static native long  startServerManagerNative(String url, long ptrCtx);
	public static long  startServerManager(String url, long ptrCtx) {
		LOG.trace( "invoking startServerNative");
		long ptr  = startServerManagerNative(url, ptrCtx);
		LOG.trace( "finished startServerNative");
		return ptr;
	}
	private static native boolean stopServerManagerNative(long ptr);
	public static boolean stopServerManager(long ptr) {
		LOG.trace( "invoking stopServerNative");
		boolean ret = stopServerManagerNative(ptr);
		LOG.trace( "finished stopServerNative ret=" + ret);
		return ret;
	}
	

	private static native long [] startServerSessionNative(String url, long ptrCtx);
	public static long [] startServerSession(String url, long ptrCtx) {
		LOG.trace( "invoking startServerNative");
		long ptr [] = startServerSessionNative(url, ptrCtx);
		LOG.trace( "finished startServerNative");
		return ptr;
	}
	private static native boolean stopServerSessionNative(long ptr);
	public static boolean stopServerSession(long ptr) {
		LOG.trace( "invoking stopServerNative");
		boolean ret = stopServerSessionNative(ptr);
		LOG.trace( "finished stopServerNative ret=" + ret);
		return ret;
	}

	private static native long forwardSessionNative(String url, long ptrSes, long ptrServer);
	public static long forwardSession(String url, long ptrSes, long ptrServer) {
		LOG.trace("invoking forwardSessionNative");
		long ptr = forwardSessionNative(url, ptrSes, ptrServer);
		LOG.trace("finished forwardSessionNative");
		return ptr;
	}

	private static native ByteBuffer createMsgPoolNative(int count, int inSize, int outSize, long [] ptrMsg);
	public static ByteBuffer  createMsgPool(int count, int inSize, int outSize, long [] ptrMsg) {
		LOG.trace("invoking createMsgPoolNative");
		ByteBuffer b = createMsgPoolNative(count, inSize, outSize, ptrMsg);
		LOG.trace("finished createMsgPoolNative");
		return b;
	}

	private static native boolean sendMsgNative(long ptrSession, int sessionType, long ptrMsg);
	public static boolean  sendMsg(long ptrSession, int sessionType, long ptrMsg) {
		LOG.trace("invoking sendMsgNative");
		boolean ret = sendMsgNative(ptrSession, sessionType, ptrMsg);
		LOG.trace("finished sendMsgNative ret= "+ ret);
		return ret;
	}
private static native boolean bindMsgPoolNative(long ptrMsgPool, long ptrEQH);
	public static boolean bindMsgPool(long ptrMsgPool, long ptrEQH) {
		LOG.trace("invoking sendMsgNative");
		boolean ret = bindMsgPoolNative(ptrMsgPool, ptrEQH);
		LOG.trace("finished sendMsgNative ret= "+ ret);
		return ret;
	}

	private static native String getErrorNative(int errorReason);
	public static String getError(int errorReason) {
//		logger.trace("invoking getErrorNative");
		String s = getErrorNative(errorReason);
//		logger.trace("finished getErrorNative. error was "+s);
		return s;
	}
}
