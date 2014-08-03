/*
 * * Copyright (C) 2013 Mellanox Technologies** Licensed under the Apache License, Version 2.0 (the "License");* you may
 * not use this file except in compliance with the License.* You may obtain a copy of the License at:**
 * http://www.apache.org/licenses/LICENSE-2.0** Unless required by applicable law or agreed to in writing, software*
 * distributed under the License is distributed on an "AS IS" BASIS,* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,*
 * either express or implied. See the License for the specific language* governing permissions and limitations under the
 * License.*
 */
package com.mellanox.jxio.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.MsgPool;

public class Bridge {

	private static final Log                              LogFromNative  = LogFactory.getLog("LogFromNative");
	private static final Log                              LOGBridge      = LogFactory.getLog(Bridge.class
	                                                                             .getCanonicalName());

	private static ConcurrentMap<Long, EventQueueHandler> mapIdEQHObject = new ConcurrentHashMap<Long, EventQueueHandler>();

	static {
		LoadLibrary.loadLibrary("libxio.so"); // Accelio library
		LoadLibrary.loadLibrary("libjxio.so"); // JXIO native library
		setNativeLogLevel(getLogLevel());
	}

	private static int getLogLevel() {
		return ((LogFromNative.isFatalEnabled() ? 1 : 0) + (LogFromNative.isErrorEnabled() ? 1 : 0)
		        + (LogFromNative.isWarnEnabled() ? 1 : 0) + (LogFromNative.isInfoEnabled() ? 1 : 0)
		        + (LogFromNative.isDebugEnabled() ? 1 : 0) + (LogFromNative.isTraceEnabled() ? 1 : 0));
	}

	// Native methods and their wrappers start here

	private static native void setLogLevelNative(int logLevel);

	private static void setNativeLogLevel(int logLevel) {
		setLogLevelNative(logLevel);
	}

	private static native boolean createCtxNative(int eventQueueSize, Object dataFromC);

	public static boolean createCtx(EventQueueHandler eqh, int eventQueueSize, EventQueueHandler.DataFromC dataFromC) {
		boolean ret = createCtxNative(eventQueueSize, dataFromC);
		if (!ret) {
			Bridge.mapIdEQHObject.put(dataFromC.getPtrCtx(), eqh);
		}
		return ret;
	}

	private static native void closeCtxNative(long ptr);

	public static void closeCtx(final long ptrCtx) {
		closeCtxNative(ptrCtx);
		Bridge.mapIdEQHObject.remove(ptrCtx);

	}

	private static native int[] runEventLoopNative(long ptr, long timeOutMicroSec);

	public static int[] runEventLoop(final long ptrCtx, final long timeOutMicroSec) {
		int[] ret = runEventLoopNative(ptrCtx, timeOutMicroSec);
		return ret;
	}

	private static native void breakEventLoopNative(long ptrCtx);

	public static void breakEventLoop(final long ptrCtx) {
		breakEventLoopNative(ptrCtx);
	}

	private static native int addEventLoopFdNative(long ptrCtx, long fd, int events, long priv_data);

	public static int addEventLoopFd(final long ptrCtx, long fd, int events, long priv_data) {
		int ret = addEventLoopFdNative(ptrCtx, fd, events, priv_data);
		return ret;
	}

	private static native int delEventLoopFdNative(long ptrCtx, long fd);

	public static int delEventLoopFd(final long ptrCtx, long fd) {
		int ret = delEventLoopFdNative(ptrCtx, fd);
		return ret;
	}

	private static native long startSessionClientNative(String url, long ptrCtx);

	public static long startSessionClient(final String url, final long ptrCtx) {
		long p = startSessionClientNative(url, ptrCtx);
		return p;
	}

	private static native void closeSessionClientNative(long ptrSes);

	public static void closeSessionClient(final long ptrSes) {
		closeSessionClientNative(ptrSes);
	}

	private static native void deleteClientNative(long ptrSes);

	public static void deleteClient(final long ptrSes) {
		deleteClientNative(ptrSes);
	}

	private static native long[] startServerPortalNative(String url, long ptrCtx);

	public static long[] startServerPortal(final String url, final long ptrCtx) {
		long ptr[] = startServerPortalNative(url, ptrCtx);
		return ptr;
	}

	private static native void stopServerPortalNative(long ptr);

	public static void stopServerPortal(final long ptrSrv) {
		stopServerPortalNative(ptrSrv);
	}

	private static native boolean closeSessionServerNative(long ptrSesServer);

	public static boolean closeServerSession(final long ptrSesServer) {
		boolean ret = closeSessionServerNative(ptrSesServer);
		return ret;
	}

	private static native long forwardSessionNative(String url, long ptrSes, long ptrPortal, long ptrPortalForwarder);

	public static long forwardSession(final String url, final long ptrSes, final long ptrPortal,
	        final long ptrPortalForwarder) {
		long ptr = forwardSessionNative(url, ptrSes, ptrPortal, ptrPortalForwarder);
		return ptr;
	}

	private static native long acceptSessionNative(long ptrSes, long ptrPortal);

	public static long acceptSession(final long ptrSes, final long ptrPortal) {
		long ptr = acceptSessionNative(ptrSes, ptrPortal);
		return ptr;
	}

	private static native long rejectSessionNative(long ptrSes, int reason, String data, int length);

	public static long rejectSession(final long ptrSes, final int reason, final String data, final int length) {
		long ptr = rejectSessionNative(ptrSes, reason, data, length);
		return ptr;
	}

	private static native ByteBuffer createMsgPoolNative(int count, int inSize, int outSize, long[] ptrMsg);

	public static ByteBuffer createMsgPool(final int count, final int inSize, final int outSize, long[] ptrMsg) {
		ByteBuffer b = createMsgPoolNative(count, inSize, outSize, ptrMsg);
		return b;
	}

	private static native void deleteMsgPoolNative(long ptrMsgPool);

	public static void deleteMsgPool(final long ptrMsgPool) {
		deleteMsgPoolNative(ptrMsgPool);
	}

	private static native int clientSendReqNative(long ptrSession, long ptrMsg, int size, boolean isMirror);

	public static int clientSendReq(final long ptrSession, final long ptrMsg, final int size, boolean isMirror) {
		int ret = clientSendReqNative(ptrSession, ptrMsg, size, isMirror);
		return ret;
	}

	private static native int serverSendResponseNative(long ptrMsg, int size, long ptrSesServer);

	public static int serverSendResponse(final long ptrMsg, final int size, final long ptrSesServer) {
		int ret = serverSendResponseNative(ptrMsg, size, ptrSesServer);
		return ret;
	}

	private static native boolean discardRequestNative(long ptrMsg);

	public static boolean discardRequest(final long ptrMsg) {
		boolean ret = discardRequestNative(ptrMsg);
		return ret;
	}

	private static native void releaseMsgServerSideNative(long ptrMsg);

	public static void releaseMsgServerSide(final long ptrMsg) {
		releaseMsgServerSideNative(ptrMsg);
	}

	private static native boolean bindMsgPoolNative(long ptrMsgPool, long ptrEQH);

	public static boolean bindMsgPool(final long ptrMsgPool, final long ptrEQH) {
		boolean ret = bindMsgPoolNative(ptrMsgPool, ptrEQH);
		return ret;
	}

	private static native void deleteSessionServerNative(long ptrSessionServer);

	public static void deleteSessionServer(final long ptrSessionServer) {
		deleteSessionServerNative(ptrSessionServer);
	}

	// callback from C++
	static public void requestForBoundMsgPool(long ptrEQH, int inSize, int outSize) {
		EventQueueHandler eqh = mapIdEQHObject.get(ptrEQH);
		if (eqh == null) {
			String fatalErrorStr = "no EventQueueHandler with id " + ptrEQH + " is found. Aborting!!!";
			LOGBridge.fatal(fatalErrorStr);
			throw new RuntimeException(fatalErrorStr);
		}
		eqh.getAdditionalMsgPool(inSize, outSize);
	}

	// this method is called by JNI in order to log messages to JXIO log
	static public void logToJava(String log_message, int severity) {

		switch (severity) {
			case 6:
				LogFromNative.trace(log_message);
				break;
			case 5:
				LogFromNative.debug(log_message);
				break;
			// case 4: combined with 'default'

			case 3:
				LogFromNative.warn(log_message);
				break;
			case 2:
				LogFromNative.error(log_message);
				break;
			case 1:
				LogFromNative.fatal(log_message);
				break;
			case 4:
			default:
				LogFromNative.info(log_message);
				break;
		}
	}
}
