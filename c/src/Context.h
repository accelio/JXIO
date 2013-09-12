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
#ifndef cJXCtx__H___
#define cJXCtx__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include "Event_queue.h"
#include "Events.h"
//#include "cJXSession.h"
#include "Utils.h"
#include <map>

class Client;

class Context {
public:
	//to move some to private?
	Context(int size);
	~Context();

	int 	run_event_loop(long timeout_micro_sec);
	void 	stop_event_loop();

	int 	add_event_loop_fd(int fd, int events, void *priv_data);
	int 	del_event_loop_fd(int fd);

	static void on_event_loop_handler(int fd, int events, void *priv_data);

	Event_queue *event_queue;
	Events *events;

	bool error_creating;

	void *ev_loop;
	struct xio_context *ctx;
	int events_num;

	int timer_fd; // fd for managing timer with fd wakeup for epoll inside the xio_event_loop

	//this map is needed since in case of event Disconnected action needs to be done
	//on a session without going back to java
	std::map<void*,Contexable*>* map_session;
};

#endif // ! cJXCtx__H___
