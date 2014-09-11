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

Context::Context(int eventQSize)
{
	CONTEXT_LOG_DBG("CTOR start");

	error_creating = false;
	this->ctx = NULL;
	this->event_queue = NULL;
	this->events = NULL;
	this->events_num = 0;

	ctx = xio_context_create(NULL, 0, -1);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (ctx == NULL) {
		CONTEXT_LOG_ERR("ERROR, xio_context_create failed");
		error_creating = true;
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	this->msg_pools.setCtx(this);

	this->event_queue = new EventQueue(eventQSize);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (this->event_queue == NULL || this->event_queue->error_creating) {
		CONTEXT_LOG_ERR("ERROR, fail in create of EventQueue object");
		goto cleanupCtx;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	this->events = new Events();
	BULLSEYE_EXCLUDE_BLOCK_START
	if (this->events == NULL) {
		goto cleanupEventQueue;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	CONTEXT_LOG_DBG("CTOR done");
	return;

cleanupEventQueue:
	delete(this->event_queue);
	this->internal_event_queue.clear();

cleanupCtx:
	xio_context_destroy(ctx);
	if (this->event_queue)
		delete(this->event_queue);
	error_creating = true;

}

Context::~Context()
{
	if (error_creating) {
		return;
	}

	delete(this->event_queue);
	this->internal_event_queue.clear();
	delete(this->events);

	xio_context_destroy(ctx);

	CONTEXT_LOG_DBG("DTOR done");
}

int Context::run_event_loop(long timeout_micro_sec)
{
	if (!this->internal_event_queue.empty()) {
		CONTEXT_LOG_DBG("there are %d internal events. no need to call ev_loop_run", internal_event_queue.size());
		while (!this->internal_event_queue.empty()) {
			// Write internal events to event queue
                        this->internal_event_queue.front()->writeEventAndDelete();
			this->internal_event_queue.pop_front();
		}
		return this->events_num;
	}

	int timeout_msec = -1; // infinite timeout as default
	if (timeout_micro_sec == -1) {
		CONTEXT_LOG_TRACE("before ev_loop_run. requested infinite timeout");
	} else {
		timeout_msec = timeout_micro_sec/1000;
		CONTEXT_LOG_TRACE("before ev_loop_run. requested timeout is %d msec", timeout_msec);
	}

	// enter Accelio's event loop
	xio_context_run_loop(this->ctx, timeout_msec);

	CONTEXT_LOG_TRACE("after ev_loop_run. there are %d events", this->events_num);

	return this->events_num;
}

void Context::break_event_loop(int is_self_thread)
{
	CONTEXT_LOG_TRACE("before break event loop (is_self_thread=%d)", is_self_thread);
	xio_context_stop_loop(this->ctx, is_self_thread);
	CONTEXT_LOG_TRACE("after break event loop (is_self_thread=%d)", is_self_thread);
}


void Context::add_msg_pool(MsgPool* msg_pool)
{
	CONTEXT_LOG_DBG("adding msg pool=%p", msg_pool);
	this->msg_pools.add_msg_pool(msg_pool);
}

void Context::reset_counters()
{
	//update offset to 0: for indication if this is the first callback called
	this->event_queue->reset();
	this->events_num = 0;
}


void Context::done_event_creating(int sizeWritten)
{
	this->event_queue->increase_offset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!this->events_num) {
		LOG_TRACE("inside a callback - stopping the event queue");
		this->break_event_loop(1); // always 'self thread = true' since JXIO break from within callback
	}
	this->events_num++;
}
