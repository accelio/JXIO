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
package com.mellanox;

import com.mellanox.JXIOEvent.*;
import com.mellanox.JXIOBridge;
import com.mellanox.JXIOEventQueueHandler;



public abstract class JXIOEventQueueHandlerEx extends JXIOEventQueueHandler {

	// Callback function to deliver the ready FD events
	public abstract void onFdReady(long fd, int events, long priv_data);

	public int addEventLoopFd(long fd, int events, long priv_data) {
		return JXIOBridge.addEventLoopFd(getId(), fd, events, priv_data);
	}
	
	public int delEventLoopFd(long fd) {
		return JXIOBridge.delEventLoopFd(getId(), fd);
	}
}
