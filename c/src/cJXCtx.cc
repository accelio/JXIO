#include "cJXCtx.h"

cJXCtx::cJXCtx(int eventQSize){
	error = false;
	evLoop = xio_ev_loop_init();
	if (evLoop == NULL){
		log (lsERROR, "Error, xio_ev_loop_init failed\n");
		error = true;
		return;
	}

	ctx = xio_ctx_open(NULL, evLoop);
	if (ctx == NULL){
		log (lsERROR, "Error, xio_ctx_open failed\n");
		error = true;
		return;
	}


	this->eventQueue = new EventQueue(eventQSize);
	this->events = new Events();


	this->eventsNum = 0;

}


cJXCtx::~cJXCtx(){
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
	log (lsDEBUG, "after xio_ev_loop_run\n");

	return this->eventsNum;
}

void cJXCtx::stopEventLoop(){
	xio_ev_loop_stop(evLoop);
}






