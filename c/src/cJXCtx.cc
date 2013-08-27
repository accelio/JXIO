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

cJXCtx::cJXCtx(int eventQSize){
	errorCreating = false;
	this->mapSession = NULL;


	this->eventsNum = 0;
	evLoop = xio_ev_loop_init();
	if (evLoop == NULL){
		log (lsERROR, "Error, xio_ev_loop_init failed\n");
		errorCreating = true;
		return;

	}

	ctx = xio_ctx_open(NULL, evLoop);
	if (ctx == NULL){
		log (lsERROR, "Error, xio_ctx_open failed\n");
		goto cleanupEvLoop;
	}

	this->eventQueue = new EventQueue(eventQSize);
	if (this->eventQueue->errorCreating){
		log (lsERROR, "Error, fail in create of EventQueue object\n");
		goto cleanupCtx;
	}
	this->events = new Events();
	return;

cleanupCtx:
	xio_ctx_close(ctx);
	delete (this->eventQueue);

cleanupEvLoop:
	xio_ev_loop_destroy(&evLoop);
	errorCreating = true;


}


cJXCtx::~cJXCtx(){
	if (errorCreating){
		return;
	}
	if (this->mapSession != NULL){
		delete (this->mapSession);
	}
	delete (this->eventQueue);
	delete (this->events);
	xio_ctx_close(ctx);
	/* destroy the event loop */
	xio_ev_loop_destroy(&evLoop);
}

int cJXCtx::runEventLoop(){
    //update offset to 0: for indication if this is the first callback called
	this->eventQueue->reset();
	this->eventsNum = 0;

	xio_ev_loop_run(this->evLoop);
	log (lsDEBUG, "after xio_ev_loop_run. there are %d evetns\n", this->eventsNum);

	return this->eventsNum;
}

void cJXCtx::stopEventLoop(){
	xio_ev_loop_stop(evLoop);
}






