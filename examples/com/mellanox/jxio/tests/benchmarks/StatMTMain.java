package com.mellanox.jxio.tests.benchmarks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler;

public class StatMTMain {
	
	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println("StatMTMain $IP $PORT $NUM_SESSIONS $NUM_THREADS");
			return;
		}
		
		String IP = args[0];
		int port  = Integer.parseInt(args[1]);
		int num_sessions = Integer.parseInt(args[2]);
		int num_threads = Integer.parseInt(args[3]);
		Thread[] t_arr = new Thread[num_threads];
		StatTest[] mt_arr = new StatTest[num_threads];
		
		for (int i=0; i<num_threads; i++) {
			mt_arr[i] = new StatTest(IP, port, (num_sessions/num_threads));
			t_arr[i] = new Thread(mt_arr[i]);
			t_arr[i].start();
        }
	}
}
