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
	
	private static native boolean destroyMsgPooNative(ByteBuffer pool);
	static boolean destroyMsgPool(ByteBuffer pool){
		logger.log(Level.INFO, "invoking destroyMsgPoolNative");
		boolean ret = destroyMsgPooNative(pool);
		logger.log(Level.INFO, "finished destroyMsgPoolNative ret=" + ret);
		return ret;
	}
	
	private static native long[] startServerSessionNative(String hostname, int port);
	static long[] startServerSession(String hostname , int port){
		logger.log(Level.INFO, "invoking startServerSessionNative");
		long[] array = startServerSessionNative(hostname, port);
		logger.log(Level.INFO, "finished startServerSessionNative");
		return array;
	}
	
	
	private static native int startClientSessionNative(int session_id, char[] url, int port);
	static void startClientSession(int session_id, String url, int port){
		logger.log(Level.INFO, "invoking startClientSessionNative");
		int ret = startClientSessionNative(session_id, url.toCharArray(), port);
		logger.log(Level.INFO, "finished startClientSessionNative ret=" + ret);
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
	
	
	
	// callback method from C code 
	public static void on_event(){
		logger.log(Level.INFO, "event queue contains msgs");
	}
}

