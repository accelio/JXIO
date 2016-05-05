/*
 * * Copyright (C) 2013 Mellanox Technologies** Licensed under the Apache License, Version 2.0 (the "License");* you may
 * not use this file except in compliance with the License.* You may obtain a copy of the License at:**
 * http://www.apache.org/licenses/LICENSE-2.0** Unless required by applicable law or agreed to in writing, software*
 * distributed under the License is distributed on an "AS IS" BASIS,* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,*
 * either express or implied. See the License for the specific language* governing permissions and limitations under the
 * License.*
 */
package org.accelio.jxio.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.accelio.jxio.impl.LoadLibrary;
import org.accelio.jxio.EventQueueHandler;

public class Bridge {

	private static final Log                              LogFromNative  = LogFactory.getLog("LogFromNative");
	private static final Log                              LOGBridge      = LogFactory.getLog(Bridge.class
	                                                                             .getCanonicalName());
	private static final String 	                      version_jxio;
	private static final String 	                      version_xio;

	private static ConcurrentMap<Long, EventQueueHandler> mapIdEQHObject = new ConcurrentHashMap<Long, EventQueueHandler>();

	static {
		LoadLibrary.loadLibrary("libxio.so"); // Accelio library
		LoadLibrary.loadLibrary("libjxio.so"); // JXIO native library
		version_jxio = getVersionNative();
		version_xio = getVersionAccelIONative();
		setNativeLogLevel(getLogLevel());
	}

	private static native String getVersionNative();
	public static String getVersion() {
		return version_jxio;
	}

	private static native String getVersionAccelIONative();
	public static String getVersionAccelIO() {
		return version_xio;
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

	private static native void createCtxNative(int eventQueueSize, Object dataFromC);

	public static void createCtx(EventQueueHandler eqh, int eventQueueSize, EventQueueHandler.DataFromC dataFromC) {
		createCtxNative(eventQueueSize, dataFromC);
		Bridge.mapIdEQHObject.put(dataFromC.getPtrCtx(), eqh);
	}

	private static native void closeCtxNative(long ptr);

	public static void closeCtx(final long ptrCtx) {
		closeCtxNative(ptrCtx);
		Bridge.mapIdEQHObject.remove(ptrCtx);
	}

	private static native int runEventLoopNative(long ptr, long timeOutMicroSec);

	public static int runEventLoop(final long ptrCtx, final long timeOutMicroSec) {
		return runEventLoopNative(ptrCtx, timeOutMicroSec);
	}

	private static native void breakEventLoopNative(long ptrCtx);

	public static void breakEventLoop(final long ptrCtx) {
		breakEventLoopNative(ptrCtx);
	}

	private static native long startSessionClientNative(String url, long ptrCtx);

	public static long startSessionClient(final String url, final long ptrCtx) {
		return startSessionClientNative(url, ptrCtx);
	}

	private static native boolean connectSessionClientNative(long ptrSes);
	
	public static boolean connectSessionClient(final long ptrSes) {
	  return connectSessionClientNative(ptrSes);
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
		return startServerPortalNative(url, ptrCtx);
	}

	private static native void stopServerPortalNative(long ptr);

	public static void stopServerPortal(final long ptrSrv) {
		stopServerPortalNative(ptrSrv);
	}

	private static native void closeSessionServerNative(long ptrSesServer);

	public static void closeServerSession(final long ptrSesServer) {
		closeSessionServerNative(ptrSesServer);
	}

	private static native boolean forwardSessionNative(String url, long ptrSes, long ptrPortal);

	public static boolean forwardSession(final String url, final long ptrSes, final long ptrPortal) {
		return forwardSessionNative(url, ptrSes, ptrPortal);
	}

	private static native boolean acceptSessionNative(long ptrSes, long ptrPortal);

	public static boolean acceptSession(final long ptrSes, final long ptrPortal) {
		return acceptSessionNative(ptrSes, ptrPortal);
	}

	private static native long rejectSessionNative(long ptrSes, int reason, String data, int length);

	public static long rejectSession(final long ptrSes, final int reason, final String data, final int length) {
		return rejectSessionNative(ptrSes, reason, data, length);
	}

	private static native ByteBuffer createMsgPoolNative(int count, int inSize, int outSize, long[] ptrMsg);

	public static ByteBuffer createMsgPool(final int count, final int inSize, final int outSize, long[] ptrMsg) {
		return createMsgPoolNative(count, inSize, outSize, ptrMsg);
	}

	private static native void deleteMsgPoolNative(long ptrMsgPool);

	public static void deleteMsgPool(final long ptrMsgPool) {
		deleteMsgPoolNative(ptrMsgPool);
	}

	private static native int clientSendReqNative(long ptrSession, long ptrMsg, int out_size, int in_size, boolean is_mirror);

	public static int clientSendReq(final long ptrSession, final long ptrMsg, final int out_size, final int in_size, final boolean is_mirror) {
		return clientSendReqNative(ptrSession, ptrMsg, out_size, in_size, is_mirror);
	}

	private static native int serverSendResponseNative(long ptrMsg, int size, long ptrSesServer);

	public static int serverSendResponse(final long ptrMsg, final int size, final long ptrSesServer) {
		return serverSendResponseNative(ptrMsg, size, ptrSesServer);
	}

	private static native boolean discardRequestNative(long ptrMsg);

	public static boolean discardRequest(final long ptrMsg) {
		return discardRequestNative(ptrMsg);
	}

	private static native void releaseMsgServerSideNative(long ptrMsg);

	public static void releaseMsgServerSide(final long ptrMsg) {
		releaseMsgServerSideNative(ptrMsg);
	}

	private static native boolean bindMsgPoolNative(long ptrMsgPool, long ptrEQH);

	public static boolean bindMsgPool(final long ptrMsgPool, final long ptrEQH) {
		return bindMsgPoolNative(ptrMsgPool, ptrEQH);
	}

	private static native void destroyConnectionSessionServerNative(long ptrSessionServer);

	public static void destroyConnectionSessionServer(final long ptrSessionServer) {
		destroyConnectionSessionServerNative(ptrSessionServer);
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
