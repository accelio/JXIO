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






