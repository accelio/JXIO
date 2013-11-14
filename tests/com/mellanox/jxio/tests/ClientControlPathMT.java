package com.mellanox.jxio.tests;


import java.nio.ByteBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mellanox.jxio.*;

public class ClientControlPathMT {
private final static Log LOG = LogFactory.getLog(ClientControlPathMT.class.getCanonicalName());

	
	public static void main(String[] args) {
		System.out.println(LOG.getClass().toString());
		String url = args[0];
		String port = args[1];
		String combined_url = "rdma://"+url+":"+port;
		MySesClient ses;
		EventQueueHandler eventQHndl;
		
		eventQHndl = new EventQueueHandler();

		//MsgPool pool = new MsgPool(2, 20, 10000);
		MsgPool pool = new MsgPool(1, 4, 4);
		Msg msg = pool.getMsg();
		ByteBuffer o = msg.getOut();
		o.putInt(7);
		//		
		System.out.println("^^^^^ msg before send is " + o.getInt(0));

		ses = new MySesClient(eventQHndl, combined_url);
		ses.sendMsg(msg);

		Thread t = new Thread (eventQHndl);
		t.start();


		Thread.currentThread();
		try {
		    Thread.sleep(5000);
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}

		System.out.println("***********************after sleep1");

		try {
		    t.sleep(5000);
		    System.out.println("***********************after sleep2");
		} catch (InterruptedException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}
		ses.close();
		pool.deleteMsgPool();
		System.out.println("***********************here");
		try {
		    eventQHndl.stop();
		    t.join();
		    System.out.println("***********************after join");
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}


		eventQHndl.close();
	}
}
