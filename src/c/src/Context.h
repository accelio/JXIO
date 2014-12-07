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
#include <deque>
#include "EventQueue.h"
#include "Events.h"
#include "MsgPools.h"

class Client;
class MsgPool;

struct __attribute__ ((packed)) scheduled_event_t {
	char* queued_event;
	size_t size;
};

class Context {
public:
	//to move some to private?
	Context(size_t events_queue_size);
	~Context();

	inline xio_context* get_xio_context() { return ctx; };

	int run_event_loop(long timeout_micro_sec);
	void break_event_loop();

	void add_msg_pool(MsgPool* msg_pool);

	char* get_buffer_raw();
	char* get_buffer(bool force_scheduled = false);
	void done_event_creating(int size_written);
	inline int get_events_count() { return events_queue.get_count(); };
	void reset_events_counters() { this->events_queue.reset();	};

	inline int scheduled_events_count() { return this->scheduled_events_queue.size(); };
	void scheduled_events_add(scheduled_event_t& scheduled_event);
	int scheduled_events_process();

	static void on_event_loop_handler(int fd, int events, void *priv_data);

	Events events;
	MsgPools msg_pools;

private:
	struct xio_context *ctx;
	EventQueue events_queue;
	bool staging_scheduled_event_in_use;
	char* staging_scheduled_event_buffer;
	std::deque<scheduled_event_t> scheduled_events_queue;
};

#endif // ! Context__H___
