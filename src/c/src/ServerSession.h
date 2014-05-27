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

#ifndef ServerSession__H___
#define ServerSession__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include <libxio.h>

class Context;


class ServerSession {
public:
	ServerSession(xio_session * session, Context* ctx);
	~ServerSession();

	Context* getCtx() {return ctx;}
	struct xio_session* get_xio_session() {return session;}
	bool get_is_closing() {return is_closing;}
	void set_is_closing(bool b) {is_closing = b;}
	//when user chooses to forward the session, connection_disconnected event is received
	bool ignore_first_disconnect();
	bool destory_first_connection();
	void set_ignore_first_disconnect();
	//when user chooses to reject the session, the event is not passed to Java, therefore after
	//session teardown the ServerSession needs to be deleted
	bool delete_after_teardown;
private:
	struct xio_session* session;
	Context* ctx;
	bool is_closing;
	bool to_ignore_first_disconnect;
	bool to_destroy_first_connection;
};

#endif
