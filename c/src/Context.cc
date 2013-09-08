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

#include <sys/timerfd.h>

#include "Context.h"

Context::Context(int eventQSize)
{
	error_creating = false;
	this->map_session = NULL;


	this->events_num = 0;
	ev_loop = xio_ev_loop_init();
	if (ev_loop == NULL){
		log (lsERROR, "Error, xio_ev_loop_init failed\n");
		error_creating = true;
		return;

	}

	ctx = xio_ctx_open(NULL, ev_loop, 0);
	if (ctx == NULL){
		log (lsERROR, "Error, xio_ctx_open failed\n");
		goto cleanupEvLoop;
	}

	this->event_queue = new Event_queue(eventQSize);
	if (this->event_queue->error_creating){
		log (lsERROR, "Error, fail in create of EventQueue object\n");
		goto cleanupCtx;
	}
	this->events = new Events();

	this->timer_fd = timerfd_create(CLOCK_MONOTONIC, 0);
	add_event_loop_fd(this->timer_fd, XIO_POLLIN, this);

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

	if (timer_fd) {
		del_event_loop_fd(this->timer_fd);
		close(this->timer_fd);
	}

	if (this->map_session != NULL) {
		delete (this->map_session);
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

	// Set the timeout if
	if (timeout_micro_sec < 0) {
		timeout_micro_sec = 0;	// Infinite timeout on timerfd
	}
	else if (timeout_micro_sec == 0) {
		timeout_micro_sec = 1;	// Minimal timeout  on timerfd (use 1 micro sec instead on zero which is infinite)
	}

	struct itimerspec timeout;
	timeout.it_value.tv_sec = timeout_micro_sec/1000000;
	timeout.it_value.tv_nsec = (timeout_micro_sec - timeout.it_value.tv_sec*1000000) * 1000;
	timerfd_settime(this->timer_fd, 0, &timeout, NULL);

	log (lsDEBUG, "[%p] before ev_loop_run. requested timeout is %d usec\n", this, timeout_micro_sec);
	xio_ev_loop_run(this->ev_loop);
	log (lsDEBUG, "[%p] after ev_loop_run. there are %d events\n", this, this->events_num);

	return this->events_num;
}

void Context::stop_event_loop()
{
	xio_ev_loop_stop(this->ev_loop);
}

int Context::add_event_loop_fd(int fd, int events, void *data)
{
	return xio_ev_loop_add(this->ev_loop, fd, events, Context::on_event_loop_handler, data);
}

int Context::del_event_loop_fd(int fd)
{
	return xio_ev_loop_del(this->ev_loop, fd);
}

void Context::on_event_loop_handler(int fd, int events, void *data)
{
	// Timer callback - stop event loop
	log (lsDEBUG, "[%p] timeout in ev_loop_run\n", data);
	Context *ctx = (Context *)data;
	ctx->stop_event_loop();
}
