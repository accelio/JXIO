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

#include "cJXCtx.h"

cJXCtx::cJXCtx(int eventQSize)
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

	ctx = xio_ctx_open(NULL, ev_loop);
	if (ctx == NULL){
		log (lsERROR, "Error, xio_ctx_open failed\n");
		goto cleanupEvLoop;
	}

	this->event_queue = new EventQueue(eventQSize);
	if (this->event_queue->error_creating){
		log (lsERROR, "Error, fail in create of EventQueue object\n");
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

cJXCtx::~cJXCtx()
{
	if (error_creating){
		return;
	}
	if (this->map_session != NULL){
		delete (this->map_session);
	}
	delete (this->event_queue);
	delete (this->events);
	xio_ctx_close(ctx);
	/* destroy the event loop */
	xio_ev_loop_destroy(&ev_loop);
}

int cJXCtx::run_event_loop()
{
    //update offset to 0: for indication if this is the first callback called
	this->event_queue->reset();
	this->events_num = 0;

	xio_ev_loop_run(this->ev_loop);
	log (lsDEBUG, "after xio_ev_loop_run. there are %d evetns\n", this->events_num);

	return this->events_num;
}

void cJXCtx::stop_event_loop()
{
	xio_ev_loop_stop(ev_loop);
}






