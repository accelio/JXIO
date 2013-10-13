package com.mellanox.jxio.tests.benchmarks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler;

public class StatTest implements Runnable {

	static final int         NUM_ITER = 100;
	int                      num_sessions;
	int                      port;
	String                   hostname;
	private final static Log LOG      = LogFactory.getLog(StatTest.class.getCanonicalName());

	public StatTest(String hostname, int port, int num_sessions) {
		this.hostname = hostname;
		this.port = port;
		this.num_sessions = num_sessions;

	}

	void print(String str) {
		LOG.debug("********" + str);
	}

	public void run() {

		// Setup parameters
		EventQueueHandler eqh = new EventQueueHandler();
		String url = "rdma://" + hostname + ":" + port;
		// Need to make sure that GC will not run
		StatClientSession[] clArr = new StatClientSession[NUM_ITER];

		long startTime = System.nanoTime();
		for (int i = 0; i < NUM_ITER; i++) {
			print("NUM_ITER = " + i);
			clArr[i] = new StatClientSession(eqh, url, num_sessions);

			eqh.runEventLoop(-1, -1);
		}

		long endTime = System.nanoTime();
		double sec = (double) (endTime - startTime) / 1000000000 / NUM_ITER;
		System.out.println(" It took " + sec + " seconds for " + num_sessions + " sessions");
		eqh.close();
	}
	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("java -classpath jx.jar:. StatTestMain $IP $PORT $NUM_SESSIONS");
			return;
		}
		Runnable test = new StatTest(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		test.run();
	}
}
