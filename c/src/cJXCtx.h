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
#ifndef cJXCtx__H___
#define cJXCtx__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include "EventQueue.h"
#include "Events.h"
//#include "cJXSession.h"
#include "Utils.h"
#include <map>

class cJXSession;

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
	bool errorCreating;

	void* evLoop;
	struct xio_context	*ctx;
//	int eventQSize;
//	int offset;
	int eventsNum;

	//this map is needed since in case of event Disconnected action needs to be done
	//on a session without going back to java
	std::map<void*,cJXSession*>* mapSession;


};




#endif // ! cJXCtx__H___
