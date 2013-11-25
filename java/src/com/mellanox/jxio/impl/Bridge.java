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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Bridge {

	private static final Log LogFromNative = LogFactory.getLog("LogFromNative");

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

	public static boolean createCtx(int eventQueueSize, Object dataFromC) {
		boolean ret = createCtxNative(eventQueueSize, dataFromC);
		return ret;
	}

	private static native void closeCtxNative(long ptr);

	public static void closeCtx(long ptr) {
		closeCtxNative(ptr);
	}

	private static native int runEventLoopNative(long ptr, long timeOutMicroSec);

	public static int runEventLoop(long ptr, long timeOutMicroSec) {
		int ret = runEventLoopNative(ptr, timeOutMicroSec);
		return ret;
	}

	private static native void breakEventLoopNative(long ptr);

	public static void breakEventLoop(long ptr) {
		breakEventLoopNative(ptr);
	}

	private static native int addEventLoopFdNative(long ptr, long fd, int events, long priv_data);

	public static int addEventLoopFd(long ptr, long fd, int events, long priv_data) {
		int ret = addEventLoopFdNative(ptr, fd, events, priv_data);
		return ret;
	}

	private static native int delEventLoopFdNative(long ptr, long fd);

	public static int delEventLoopFd(long ptr, long fd) {
		int ret = delEventLoopFdNative(ptr, fd);
		return ret;
	}

	private static native long startSessionClientNative(String url, long ptrCtx);

	public static long startSessionClient(String url, long ptrCtx) {
		long p = startSessionClientNative(url, ptrCtx);
		return p;
	}

	private static native void closeSessionClientNative(long sesPtr);

	public static void closeSessionClient(long sesPtr) {
		closeSessionClientNative(sesPtr);
	}

	private static native long[] startServerPortalNative(String url, long ptrCtx);

	public static long[] startServerPortal(String url, long ptrCtx) {
		long ptr[] = startServerPortalNative(url, ptrCtx);
		return ptr;
	}

	private static native boolean stopServerPortalNative(long ptr);

	public static boolean stopServerPortal(long ptr) {
		boolean ret = stopServerPortalNative(ptr);
		return ret;
	}

	private static native boolean closeServerSessionNative(long ptr);

	public static boolean closeServerSession(long ptr) {
		boolean ret = closeServerSessionNative(ptr);
		return ret;
	}

	private static native long forwardSessionNative(String url, long ptrSes);

	public static long forwardSession(String url, long ptrSes) {
		long ptr = forwardSessionNative(url, ptrSes);
		return ptr;
	}

	private static native long acceptSessionNative(long ptrSes);

	public static long acceptSession(long ptrSes) {
		long ptr = acceptSessionNative(ptrSes);
		return ptr;
	}
	
	private static native ByteBuffer createMsgPoolNative(int count, int inSize, int outSize, long[] ptrMsg);

	public static ByteBuffer createMsgPool(int count, int inSize, int outSize, long[] ptrMsg) {
		ByteBuffer b = createMsgPoolNative(count, inSize, outSize, ptrMsg);
		return b;
	}

	private static native void deleteMsgPoolNative(long ptrMsgPool);

	public static void deleteMsgPool(long ptrMsgPool) {
		deleteMsgPoolNative(ptrMsgPool);
	}
	
	private static native boolean clientSendReqNative(long ptrSession, long ptrMsg);

	public static boolean clientSendReq(long ptrSession, long ptrMsg) {
		boolean ret = clientSendReqNative(ptrSession, ptrMsg);
		return ret;
	}

	private static native boolean serverSendReplyNative(long ptrMsg);

	public static boolean serverSendReply(long ptrMsg) {
		boolean ret = serverSendReplyNative(ptrMsg);
		return ret;
	}

	private static native boolean bindMsgPoolNative(long ptrMsgPool, long ptrEQH);

	public static boolean bindMsgPool(long ptrMsgPool, long ptrEQH) {
		boolean ret = bindMsgPoolNative(ptrMsgPool, ptrEQH);
		return ret;
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
			case 4:
				LogFromNative.info(log_message);
				break;
			case 3:
				LogFromNative.warn(log_message);
				break;
			case 2:
				LogFromNative.error(log_message);
				break;
			case 1:
				LogFromNative.fatal(log_message);
				break;
			default:
				LogFromNative.info(log_message);
				break;
		}
	}
}
