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

#include "Context.h"
#include "CallbackFunctions.h"

Context::Context(int eventQSize)
{
	error_creating = false;
	this->ctx = NULL;
	this->event_queue = NULL;
	this->events = NULL;
	this->msg_pool = NULL;
	this->events_num = 0;

	ev_loop = xio_ev_loop_init();
	if (ev_loop == NULL) {
		log(lsDEBUG, "ERROR, xio_ev_loop_init failed\n");
		error_creating = true;
		return;
	}

	ctx = xio_ctx_open(NULL, ev_loop, 0);
	if (ctx == NULL) {
		log(lsDEBUG, "ERROR, xio_ctx_open failed\n");
		goto cleanupEvLoop;
	}

	this->event_queue = new Event_queue(eventQSize);
	if (this->event_queue->error_creating) {
		log(lsDEBUG, "ERROR, fail in create of EventQueue object\n");
		goto cleanupCtx;
	}
	this->events = new Events();

	return;

cleanupCtx:
	xio_ctx_close(ctx);
	delete (this->event_queue);

cleanupEvLoop:
	xio_ev_loop_destroy(&ev_loop);
	error_creating = true;
}

Context::~Context()
{
	if (error_creating) {
		return;
	}

	delete (this->event_queue);
	delete (this->events);
	xio_ctx_close(ctx);
	// destroy the event loop
	xio_ev_loop_destroy(&ev_loop);
}

int Context::run_event_loop(long timeout_micro_sec)
{
	//update offset to 0: for indication if this is the first callback called
	this->event_queue->reset();
	this->events_num = 0;

	int timeout_msec = -1; // infinite timeout as default
	if (timeout_micro_sec == -1) {
		log (lsDEBUG, "[%p] before ev_loop_run. requested infinite timeout\n", this);
	} else {
		timeout_msec = timeout_micro_sec/1000;
		log (lsDEBUG, "[%p] before ev_loop_run. requested timeout is %d msec\n", this, timeout_msec);
	}

	// enter Accelio's event loop
	xio_ev_loop_run_timeout(this->ev_loop, timeout_msec);

	log (lsDEBUG, "[%p] after ev_loop_run. there are %d events\n", this, this->events_num);

	return this->events_num;
}

void Context::break_event_loop()
{
	xio_ev_loop_stop(this->ev_loop);
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
	this->msg_pool = msg_pool;
}
