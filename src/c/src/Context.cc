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

#include "Utils.h"
#include "Context.h"
#include "CallbackFunctions.h"

#define MODULE_NAME		"Context"
#define CONTEXT_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define CONTEXT_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)


Context::Context(int eventQSize)
{
	CONTEXT_LOG_DBG("CTOR start");

	error_creating = false;
	this->ctx = NULL;
	this->event_queue = NULL;
	this->events = NULL;
	this->events_num = 0;

	ev_loop = xio_ev_loop_init();
	if (ev_loop == NULL) {
		CONTEXT_LOG_ERR("ERROR, xio_ev_loop_init failed");
		error_creating = true;
		return;
	}

	ctx = xio_ctx_open(NULL, ev_loop, 0);
	if (ctx == NULL) {
		CONTEXT_LOG_ERR("ERROR, xio_ctx_open failed");
		goto cleanupEvLoop;
	}

	this->msg_pools.setCtx(this);

	this->event_queue = new Event_queue(eventQSize);
	if (this->event_queue == NULL || this->event_queue->error_creating) {
		CONTEXT_LOG_ERR("ERROR, fail in create of EventQueue object");
		goto cleanupCtx;
	}
	this->events = new Events();
	if (this->events == NULL) {
		goto cleanupEventQueue;
	}

	CONTEXT_LOG_DBG("CTOR done");
	return;

cleanupEventQueue:
	delete(this->event_queue);

cleanupCtx:
	xio_ctx_close(ctx);
	delete(this->event_queue);

cleanupEvLoop:
	xio_ev_loop_destroy(&ev_loop);
	error_creating = true;
}

Context::~Context()
{
	if (error_creating) {
		return;
	}

	delete(this->event_queue);
	delete(this->events);

	xio_ctx_close(ctx);
	// destroy the event loop
	xio_ev_loop_destroy(&ev_loop);

	CONTEXT_LOG_DBG("DTOR done");
}

int Context::run_event_loop(long timeout_micro_sec)
{
	//update offset to 0: for indication if this is the first callback called
	this->event_queue->reset();
	this->events_num = 0;

	int timeout_msec = -1; // infinite timeout as default
	if (timeout_micro_sec == -1) {
		CONTEXT_LOG_DBG("before ev_loop_run. requested infinite timeout");
	} else {
		timeout_msec = timeout_micro_sec/1000;
		CONTEXT_LOG_DBG("before ev_loop_run. requested timeout is %d msec", timeout_msec);
	}

	// enter Accelio's event loop
	xio_ev_loop_run_timeout(this->ev_loop, timeout_msec);

	CONTEXT_LOG_DBG("after ev_loop_run. there are %d events", this->events_num);

	return this->events_num;
}

void Context::break_event_loop(int is_self_thread)
{
	CONTEXT_LOG_DBG("before break event loop (is_self_thread=%d)", is_self_thread);
	xio_ev_loop_stop(this->ev_loop, is_self_thread);
	CONTEXT_LOG_DBG("after break event loop (is_self_thread=%d)", is_self_thread);
}

int Context::add_event_loop_fd(int fd, int events, void *priv_data)
{
	return xio_ev_loop_add(this->ev_loop, fd, events, Context::on_event_loop_handler, priv_data);
}

int Context::del_event_loop_fd(int fd)
{
	return xio_ev_loop_del(this->ev_loop, fd);
}

void Context::on_event_loop_handler(int fd, int events, void *data)
{
	Context *ctx = (Context *)data;

	// Pass an 'FD Ready' event to Java
	on_fd_ready_event_callback(ctx, fd, events);
}

void Context::add_msg_pool (MsgPool* msg_pool)
{
	CONTEXT_LOG_DBG("adding msg pool=%p", msg_pool);
	this->msg_pools.add_msg_pool(msg_pool);
}
