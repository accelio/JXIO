/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and  limitations under the License.
 **
 */
package com.mellanox.jxio.tests.random.storyrunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.MsgPool;
import com.mellanox.jxio.jxioConnection.JxioConnection;

public class JConnectionClientPlayer extends GeneralPlayer {

	private final static Log LOG       = LogFactory.getLog(JConnectionClientPlayer.class.getSimpleName());

	private final String     name;
	private final URI        uri;
	private final long       runDurationSec;
	private final long       startDelaySec;
	private final long       size;
	private final long       msgDelayMicroSec;
	private final long       msgPoolMem;
	private WorkerThread     workerThread;
	private long             startSessionTime;
	private boolean          isClosing = false;
	private Random           random;
	private int              violent_exit;
	private JxioConnection   connection;
	private InputStream      input;
	private long             bytesRead = 0;

	public JConnectionClientPlayer(int id, URI uri, long startDelaySec, long runDurationSec, long msgPoolMem,
	        int msgRate, int violent_exit, long seed, long size) {
		this.name = new String("CP[" + id + "]");
		this.uri = uri;
		this.runDurationSec = runDurationSec;
		this.startDelaySec = startDelaySec;
		this.msgDelayMicroSec = msgRate;
		this.msgPoolMem = msgPoolMem;
		this.size = size;
		this.violent_exit = violent_exit;
		this.random = new Random(seed);
		LOG.debug("new " + this.toString() + " done");
	}

	@Override
	public void attach(WorkerThread workerThread) {
		LOG.info(this.toString() + ": attaching to WorkerThread '" + workerThread.toString() + "'" + ", startDelay="
		        + startDelaySec + "sec, runDuration=" + runDurationSec + "sec");
		this.workerThread = workerThread;

		// register initialize timer
		TimerList.Timer tInitialize = new InitializeTimer(this.startDelaySec * 1000000);
		this.workerThread.start(tInitialize);
	}

	private class SendMsgTimer extends TimerList.Timer {
		private final JConnectionClientPlayer outer = JConnectionClientPlayer.this;

		public SendMsgTimer(long durationMicroSec) {
			super(durationMicroSec);
		}

		@Override
		public void onTimeOut() {
			if (isClosing)
				return;
			int randTimes = random.nextInt((int) Math
			        .ceil((double) (size - bytesRead) / JxioConnection.msgPoolBuffSize)) + 1;
			byte[] temp = new byte[JxioConnection.msgPoolBuffSize];
			for (int i = 0; i < randTimes && bytesRead < size; i++) {
				try {
					bytesRead += input.read(temp, 0, (int) Math.min(size - bytesRead, (long) temp.length));
				} catch (IOException e) {
					LOG.error("SendMsgTimer: " + outer.toString());
					e.printStackTrace();
				}
			}
			if (bytesRead >= size) {
				TimerList.Timer tTerminate = new TerminateTimer(0);
				this.outer.workerThread.start(tTerminate);
				LOG.info(name + ":Finished reading requested data, terminating");
			} else {
				// register next send timer
				sendMsgTimerStart();
			}
		}
	}

	protected void sendMsgTimerStart() {
		TimerList.Timer tSendMsg = new SendMsgTimer(this.msgDelayMicroSec);
		this.workerThread.start(tSendMsg);
	}

	@Override
	protected void initialize() {
		this.startSessionTime = System.nanoTime();
		try {
			connection = new JxioConnection(msgPoolMem, uri);
		} catch (ConnectException e) {
			if (e.getMessage().contains("SESSION_REJECT") && uri.getQuery().contains("reject=1")) {
				LOG.info(name + ": SUCCESSFULLY got rejected as expected");
				this.workerThread.start(new TerminateTimer(0));
				return;
			}else {
				LOG.error(name + ": FAILURE: " + e.getMessage());
				System.exit(1);
			}
		}
		final long timeSessionEstablished = System.nanoTime() - startSessionTime;
		if (timeSessionEstablished > 350000000 && !LOG.isDebugEnabled()) { // 100 milli-sec Debug prints slow down
			LOG.error(name + ": FAILURE: session establish took " + timeSessionEstablished / 1000 + " usec");
			System.exit(1); // Failure in test - eject!
		}
		input = connection.getInputStream();

		// register terminate timer
		TimerList.Timer tTerminate = new TerminateTimer(this.runDurationSec * 1000000);
		this.workerThread.start(tTerminate);

		sendMsgTimerStart();
	}

	@Override
	protected void terminate() {
		if (isClosing)
			return;
		isClosing = true;
		if (this.violent_exit == 1) {
			LOG.info(this.toString() + ": terminating violently. Exiting NOW");
			System.exit(0);
		}
		LOG.info(this.toString() + ": terminating.");
		connection.disconnect();
	}
	
	public String toString() {
		return name;
	}
}
