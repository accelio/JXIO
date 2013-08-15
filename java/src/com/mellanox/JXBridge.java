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

	
	private static native long[] startClientSessionNative(String url, int port, long ptrCtx);
	static long[] startClientSession(String url, int port, long ptrCtx){
		long ar[];
		logger.log(Level.INFO, "invoking startSessionNative");
		ar = startClientSessionNative(url, port, ptrCtx);		
		logger.log(Level.INFO, "finished startSessionNative ");
		return ar;
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
		logger.log(Level.INFO, "invoking getErrorNative");
		String s = getErrorNative(errorReason);
		logger.log(Level.INFO, "finished getErrorNative. error was "+s);
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
	
	
	private static native long[] createEQHNative();
	static long[] createEQH(){
		logger.log(Level.INFO, "invoking Bridge.createEQHNative");
		long[] ar = createEQHNative();
		logger.log(Level.INFO, "finished Bridge.createEQHNative");
		return ar;
	}
	
	private static native ByteBuffer allocateEventQNative(long ptrCtx, long ptrEvLoop, int eventQSize);
	static ByteBuffer allocateEventQ(long ptrCtx, long ptrEvLoop, int eventQSize){
		logger.log(Level.INFO, "invoking Bridge.allocateEventQNative");
		ByteBuffer b = allocateEventQNative(ptrCtx, ptrEvLoop, eventQSize);
		logger.log(Level.INFO, "finished Bridge.allocateEventQNative");
		return b;
	}
	
	
	private static native void closeEQHNative(long ptrCtx, long ptrEvLoop);
	static void closeEQH(long ptrCtx, long ptrEvLoop){
		logger.log(Level.INFO, "invoking Bridge.closeEQHNative");
		closeEQHNative(ptrCtx, ptrEvLoop);
		logger.log(Level.INFO, "finished Bridge.closeEQHNative" );
	}

	private static native int runEventLoopNative(long ptr);
	static void runEventLoop(long ptr){
		logger.log(Level.INFO, "invoking Bridge.runEventLoopNative");
		int ret = runEventLoopNative(ptr);
		logger.log(Level.INFO, "finished Bridge.runEventLoopNative=" + ret);
	}
	
	private static native boolean closeSessionClientNative(long sesPtr);
	static boolean closeSessionClient(long sesPtr){
		logger.log(Level.INFO, "invoking Bridge.closeSessionClient");
		boolean ret = closeSessionClientNative(sesPtr);
		logger.log(Level.INFO, "finished Bridge.closeSessionClient=" + ret);
		return ret;
	}
	
	private static native boolean closeConnectionClientNative(long conPtr);
	static boolean closeConnectionClient(long conPtr){
		logger.log(Level.INFO, "invoking closeConnectionClientNative");
		boolean ret = closeConnectionClientNative(conPtr);
		logger.log(Level.INFO, "finished closeConnectionClientNative=" + ret);
		return ret;
	}
	
	// callback method from C code 
	public static void on_event(){
		logger.log(Level.INFO, "event queue contains msgs");
	}
}

