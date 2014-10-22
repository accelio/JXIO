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

#ifndef ServerPortal__H___
#define ServerPortal__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>


#include "CallbackFunctionsServer.h"
#include "Events.h"
class Context;


class ServerPortal {
public:
	//to move some to private?
	ServerPortal(const char *url, long ptrCtx);
	~ServerPortal();

	//this method will return ctx if the event should be written to event queue. Otherwise will return null
	Context* ctxForSessionEvent(struct xio_session_event_data * event,
			ServerSession* ses);
	void deleteObject();
	//this method writes that the portal was closed to the event queue and deletes this
	void writeEventAndDelete(bool force_scheduled = false);
	void scheduleWriteEventAndDelete();
	void writeEventForwardCompleted(ServerSession* ses);
	struct xio_server* server;
	bool is_closing;
	int sessions; //indicates how many sessions are listening on this server
	uint16_t port; //indicates the actual port on which the server listens
	bool flag_to_delete;
	Context* get_ctx_class() {return ctx_class;}
private:
	Context *ctx_class;
};

#endif // ! ServerManager
