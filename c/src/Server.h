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

#ifndef Server__H___
#define Server__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "CallbackFunctions.h"


class Context;

class Server : public Contexable {
public:
	//to move some to private?
	Server(const char	*url, long ptrCtx);
	~Server();

	bool forward(struct xio_session *, const char * url);
	bool send_reply (Msg *msg);
	bool onSessionEvent(int eventType);
	bool close();

	struct xio_server* server;
	struct xio_session* session;

	bool error_creating;
	uint16_t port; //indicates the actual port on which the server listens
	bool closingInProcess;
};

#endif // ! cJXServer__H___
