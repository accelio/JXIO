package com.mellanox.jxio.tests.random.storyrunner;

import java.util.concurrent.atomic.AtomicInteger;

public class CallbacksCounter {

	private AtomicInteger counter;
	private int           limit;
	private boolean       limitReached;
	private boolean       cyclic = false;
	private boolean       exceptionThrown;

	/**
     * Creates a new callback counter.
     * 
     * @param limit
     *            The maximal number of callback calls before an exception is thrown. The value of 0 stands for no
     *            exceptions.
     */
	public CallbacksCounter(int limit) {
		this.counter.set(0);
		this.limit = limit;
		this.limitReached = false;
		this.exceptionThrown = false;
	}

	/**
     * Creates a new callback counter.
     * 
     * @param limit
     *            The maximal number of callback calls before an exception is thrown. The value of 0 stands for no
     *            exceptions.
     * @param cyclic
     *            True is the counter will be a cyclic counter that throws an exception every time the limit is reached,
     *            false will create a one-time excepion throw counter.
     */
	public CallbacksCounter(int limit, boolean cyclic) {
		this(limit);
		this.cyclic = cyclic;
	}

	/**
     * Increment the callbacks counter.
     */
	public void increment() {
		if (this.limit != 0) {
			int ret = this.counter.incrementAndGet();
			if (ret == this.limit) {
				this.limitReached = true;
			}
		}
	}

	/**
     * @return True if the miximal number of callback calls was reached and an exception should be thrown.
     */
	public boolean isLimitReached() {
		return this.limitReached;
	}
	
	/**
     * @return True if an exception was thrown. After calling this function, this indicator resets.
     */
	public boolean wasExceptionThrown() {
		boolean wasExceptionThrown = this.exceptionThrown;
		this.exceptionThrown = false;
		return wasExceptionThrown;
	}

	/**
	 * Throws an exception if the callback counter reached its limit.
	 * 
	 * @param msg
	 *            The message to be shown in the excpetion.
	 */
	public void possiblyThrowExecption(String msg) {
		// Increment the counter
		this.increment();
		// If limit reached throw an exception
		if (this.isLimitReached()) {
			if (this.cyclic) {
				// Zero the counter and reset the limit
				this.counter.set(0);
				this.limitReached = false;
			}
			this.exceptionThrown = true;
			// Throw an exception
			throw new RuntimeException(msg);
		}
	}
}
