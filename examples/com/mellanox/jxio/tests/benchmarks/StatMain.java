package com.mellanox.jxio.tests.benchmarks;

public class StatMain {
	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("StatMain $IP $PORT $NUM_SESSIONS");
			return;
		}
		Runnable test = new StatTest(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		test.run();
	}

}
