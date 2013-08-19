#ifndef cJXCtx__H___
#define cJXCtx__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include "EventQueue.h"
#include "Events.h"
#include "Utils.h"

class cJXCtx{
public:
	//to move some to private?
	cJXCtx(int size);
	~cJXCtx();

	int runEventLoop();

	void stopEventLoop();


	EventQueue * eventQueue;
	Events *events;



//	char* byteBuffer;
	bool error;

	void* evLoop;
	struct xio_context	*ctx;
//	int eventQSize;
//	int offset;
	int eventsNum;

};




#endif // ! cJXCtx__H___
