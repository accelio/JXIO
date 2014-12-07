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

#include "bullseye.h"
#include "Utils.h"
#include "Context.h"
#include "ServerPortal.h"

#define MODULE_NAME		"Context"
#define CONTEXT_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define CONTEXT_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)
#define CONTEXT_LOG_TRACE(log_fmt, log_args...)  LOG_BY_MODULE(lsTRACE, log_fmt, ##log_args)

Context::Context(size_t events_queue_size) : events_queue(events_queue_size)
{
	CONTEXT_LOG_DBG("CTOR start");

	staging_scheduled_event_buffer = new char[(sizeof(scheduled_event_t) + EVENTQUEUE_HEADROOM_BUFFER)];
	staging_scheduled_event_in_use = false;

	this->ctx = xio_context_create(NULL, 0, -1);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (ctx == NULL) {
		CONTEXT_LOG_ERR("ERROR on xio_context_create() (errno=%d '%s')", xio_errno(), xio_strerror(xio_errno()));
		delete[] staging_scheduled_event_buffer;
		throw std::bad_alloc();
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	this->msg_pools.setCtx(this);

	CONTEXT_LOG_DBG("CTOR done");
	return;
}

Context::~Context()
{
	while (scheduled_events_count() > 0) {
		delete this->scheduled_events_queue.front().queued_event;
		this->scheduled_events_queue.pop_front();
	}
	delete[] staging_scheduled_event_buffer;

	xio_context_destroy(ctx);

	CONTEXT_LOG_DBG("DTOR done");
}

int Context::run_event_loop(long timeout_micro_sec)
{
	reset_events_counters();

	if (scheduled_events_count() == 0) {

		int timeout_msec = -1; // infinite timeout as default
		if (timeout_micro_sec == -1) {
			CONTEXT_LOG_TRACE("before ev_loop_run. requested infinite timeout");
		} else {
			timeout_msec = timeout_micro_sec/1000;
			CONTEXT_LOG_TRACE("before ev_loop_run. requested timeout is %d msec", timeout_msec);
		}

		// enter Accelio's event loop
		xio_context_run_loop(this->ctx, timeout_msec);
	}

	scheduled_events_process();

	CONTEXT_LOG_TRACE("after ev_loop_run. there are %d events", get_events_count());
	return get_events_count();
}

void Context::break_event_loop()
{
	CONTEXT_LOG_TRACE("before break event loop");
	xio_context_stop_loop(this->ctx);
	CONTEXT_LOG_TRACE("after break event loop");
}

void Context::add_msg_pool(MsgPool* msg_pool)
{
	CONTEXT_LOG_DBG("adding msg pool=%p", msg_pool);
	this->msg_pools.add_msg_pool(msg_pool);
}

char* Context::get_buffer_raw()
{
	return events_queue.get_buffer();
}

char* Context::get_buffer(bool force_scheduled /*=false*/)
{
	char* buf = NULL;
	if (force_scheduled != true)
		buf = events_queue.get_buffer_offset();
	if (buf == NULL) {
		staging_scheduled_event_in_use = true;
		return staging_scheduled_event_buffer;
	}
	return buf;
}

void Context::done_event_creating(int size_written)
{
	if (staging_scheduled_event_in_use == true) {
		staging_scheduled_event_in_use = false;

		scheduled_event_t scheduled_event;
		scheduled_event.queued_event = new char[size_written];
		scheduled_event.size = size_written;
		memcpy(scheduled_event.queued_event, staging_scheduled_event_buffer, size_written);
		scheduled_events_add(scheduled_event);
	}
	else {
		this->events_queue.increase_offset(size_written);

		// need to stop the event queue only if this is the first callback
		if (get_events_count() == 1) {
			CONTEXT_LOG_TRACE("inside a callback - stopping the event queue");
			this->break_event_loop();
		}
	}
}

void Context::scheduled_events_add(scheduled_event_t& scheduled_event)
{
	CONTEXT_LOG_TRACE("adding scheduled event (queue size = %d)", scheduled_events_count());
	this->scheduled_events_queue.push_back(scheduled_event);
	this->break_event_loop();
}

int Context::scheduled_events_process()
{
	CONTEXT_LOG_TRACE("going to process %d scheduled events from queue", scheduled_events_count());
	char* events_queue_buf = NULL;
	while (scheduled_events_count() > 0 && ((events_queue_buf = events_queue.get_buffer_offset()) != NULL)) {

		scheduled_event_t scheduled_event = this->scheduled_events_queue.front();

		// Write internal events to event queue
		memcpy(events_queue_buf, scheduled_event.queued_event, scheduled_event.size);
		this->events_queue.increase_offset(scheduled_event.size);

		delete scheduled_event.queued_event;
		this->scheduled_events_queue.pop_front();
	}
	return get_events_count();
}
