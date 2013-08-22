package com.mellanox;

import java.nio.ByteBuffer;
import java.util.logging.Level;


public class JXBridge {


    static {  
		String s1=JXBridge.class.getProtectionDomain().getCodeSource().getLocation().getPath(); 
		String s2=s1.substring(0, s1.lastIndexOf("/")+1);
		Runtime.getRuntime().load(s2 + "libjx.so");
    }
	private static JXLog logger = JXLog.getLog(JXBridge.class.getCanonicalName());

	// Native methods and their wrappers start here
    
	private static native ByteBuffer createMsgPooNative(int msg_size, int num_of_msgs);
	static ByteBuffer createMsgPool(int msg_size, int num_of_msgs){
		logger.log(Level.INFO, "invoking createMsgPoolNative");
		ByteBuffer b = createMsgPooNative(msg_size, num_of_msgs);
		logger.log(Level.INFO, "finished createMsgPoolNative");
		return b;
	}
	
	private static native boolean stopServerNative(long ptr);
	static boolean stopServer(long ptr){
		logger.log(Level.INFO, "invoking stopServerNative");
		boolean ret = stopServerNative(ptr);
		logger.log(Level.INFO, "finished stopServerNative ret=" + ret);
		return ret;
	}
	
	private static native boolean destroyMsgPooNative(ByteBuffer pool);
	static boolean destroyMsgPool(ByteBuffer pool){
		logger.log(Level.INFO, "invoking destroyMsgPoolNative");
		boolean ret = destroyMsgPooNative(pool);
		logger.log(Level.INFO, "finished destroyMsgPoolNative ret=" + ret);
		return ret;
	}
	
	private static native int getNumEventsQNative(long ptr);
	static int getNumEventsQ(long ptr){
		logger.log(Level.INFO, "invoking getNumEventsQNative");
		int ret = getNumEventsQNative(ptr);
		logger.log(Level.INFO, "finished getNumEventsQNative");
		return ret;
	}

	
	private static native long startClientSessionNative(String url, int port, long ptrCtx);
	static long startClientSession(String url, int port, long ptrCtx){
		logger.log(Level.INFO, "invoking startSessionNative");
		long p = startClientSessionNative(url, port, ptrCtx);		
		logger.log(Level.INFO, "finished startSessionNative ");
		return p;
	}
	

	


	private static native long startServerNative(String url, int port, long ptrCtx);
	static long  startServer(String url, int port, long ptrCtx){
		logger.log(Level.INFO, "invoking startServerNative");
		long ptr = startServerNative(url, port, ptrCtx);
		logger.log(Level.INFO, "finished startServerNative");
		return ptr;
	}
	
	private static native long forwardSessionNative(String url, int port, long ptrSes);
	static long  forwardSession(String url, int port, long ptrSes){
		logger.log(Level.INFO, "invoking forwardSessionNative");
		long ptr = forwardSessionNative(url, port, ptrSes);
		logger.log(Level.INFO, "finished forwardSessionNative");
		return ptr;
	}
	
	
	private static native String getErrorNative(int errorReason);
	static String getError(int errorReason){
//		logger.log(Level.FINE, "invoking getErrorNative");
		String s = getErrorNative(errorReason);
//		logger.log(Level.FINE, "finished getErrorNative. error was "+s);
		return s;
	}
	
	
	
	private static native int sendMsgNative(int session_id, int connection_id, int ctx_id, int offset);
	static void sendMsg(int session_id, int connection_id, int ctx_id, int offset){
		logger.log(Level.INFO, "invoking sendMsgNative");
		int ret = sendMsgNative(session_id, connection_id, ctx_id, offset);
		logger.log(Level.INFO, "finished sendMsgNative ret=" + ret);
	}
	

	private static native int freeMsgsNative(int[] id_array);
	static void freeMsgs(int[] id_array){
		logger.log(Level.INFO, "invoking freeMsgsNative");
		int ret = freeMsgsNative(id_array);
		logger.log(Level.INFO, "finished freeMsgsNative ret=" + ret);
	}
	
	
	
	private static native int runEventLoopNative(long ptr);
	static int runEventLoop(long ptr){
		logger.log(Level.INFO, "invoking Bridge.runEventLoopNative");
		int ret = runEventLoopNative(ptr);
		logger.log(Level.INFO, "finished Bridge.runEventLoopNative=" + ret);
		return ret;
	}
	
	
	private static native void stopEventLoopNative(long ptr);
	static void stopEventLoop(long ptr){
		logger.log(Level.INFO, "invoking stopEventLoopNative");
		stopEventLoopNative(ptr);
		logger.log(Level.INFO, "finished stopEventLoopNative");
	}
	/*
	private static native boolean closeSessionClientNative(long sesPtr);
	static boolean closeSessionClient(long sesPtr){
		logger.log(Level.INFO, "invoking Bridge.closeSessionClient");
		boolean ret = closeSessionClientNative(sesPtr);
		logger.log(Level.INFO, "finished Bridge.closeSessionClient=" + ret);
		return ret;
	}*/
	
	
	private static native void closeSessionClientNative(long sesPtr);
	static void closeSessionClient(long sesPtr){
		logger.log(Level.INFO, "invoking Bridge.closeSessionClient");
		closeSessionClientNative(sesPtr);
		logger.log(Level.INFO, "finished Bridge.closeSessionClient");
	}
	
	
	private static native boolean closeConnectionClientNative(long sesPtr);
	static boolean closeConnectionClient(long sesPtr){
		logger.log(Level.INFO, "invoking closeConnectionClientNative");
		boolean ret = closeConnectionClientNative(sesPtr);
		logger.log(Level.INFO, "finished closeConnectionClientNative=" + ret);
		return ret;
	}
	
	
	private static native boolean createCtxNative(int eventQueueSize, Object dataFromC);
	static boolean createCtx(int eventQueueSize, Object dataFromC) {
		logger.log(Level.INFO, "invoking closeConnectionClientNative");
		boolean ret = createCtxNative(eventQueueSize, dataFromC);
		logger.log(Level.INFO, "finished closeConnectionClientNative=" + ret);
		return ret;
		
	}

	private static native void closeCtxNative(long ptr);
		static void closeCtx(long ptr) {
			logger.log(Level.INFO, "invoking closeCtxNative");
			closeCtxNative(ptr);
			logger.log(Level.INFO, "finished closeCtxNative" );
			
		}
}

