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

public abstract class GeneralPlayer {
	protected GeneralPlayer player;

	// callback from AttachAction
	protected abstract void attach(WorkerThread workerThread);

	// callback from InitializeTimer
	protected abstract void initialize();

	// callback from TerminateTimer
	protected abstract void terminate();

	public AttachAction getAttachAction() {
		return new AttachAction();
	}

	protected class AttachAction implements WorkerThread.QueueAction {
		private final GeneralPlayer player = GeneralPlayer.this;

		public void doAction(WorkerThread workerThread) {
			this.player.attach(workerThread);
		}
	}

	protected class InitializeTimer extends TimerList.Timer {
		private final GeneralPlayer player = GeneralPlayer.this;

		public InitializeTimer(long durationMicroSec) {
			super(durationMicroSec);
		}

		@Override
		public void onTimeOut() {
			this.player.initialize();
		}
	}

	protected class TerminateTimer extends TimerList.Timer {
		private final GeneralPlayer player = GeneralPlayer.this;

		public TerminateTimer(long durationMicroSec) {
			super(durationMicroSec);
		}

		@Override
		public void onTimeOut() {
			this.player.terminate();
		}
	}
}
