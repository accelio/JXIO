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
#ifndef Context__H___
#define Context__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include "Event_queue.h"
#include "Events.h"
#include "MsgPools.h"

class Client;
class MsgPool;

class Context {
public:
	//to move some to private?
	Context(int size);
	~Context();

	int run_event_loop(long timeout_micro_sec);
	void break_event_loop(int is_self_thread);
	int add_event_loop_fd(int fd, int events, void *priv_data);
	int del_event_loop_fd(int fd);
	void add_msg_pool(MsgPool* msg_pool);
	static void on_event_loop_handler(int fd, int events, void *priv_data);

	Event_queue *event_queue;
	Events *events;
	bool error_creating;
	void *ev_loop;
	struct xio_context *ctx;
	int events_num;
	MsgPools msg_pools;
};

#endif // ! Context__H___
