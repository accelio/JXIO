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
class ServerPortal;


class ServerSession {
public:
	ServerSession(xio_session * session, ServerPortal* portal, Context * ctx);
	~ServerSession();

	//inline functions
	void set_portal(ServerPortal *p) {this->forwardee = p;}
	struct xio_session* get_xio_session() {return session;}
	bool get_is_closing() {return is_closing;}
	void set_is_closing(bool b) {is_closing = b;}
	void set_forward(bool forward) {forward_mode = forward;}
	void set_reject() {reject_mode = true;}
	bool is_reject() {return reject_mode;}
	Context *get_ctx_forwarder() {return ctx_forwarder;}

	//when user chooses to forward the session, connection_disconnected event is received
	bool ignore_disconnect(ServerPortal* portal);
	//when user chooses to reject the session, the event is not passed to Java, therefore after
	//session teardown the ServerSession needs to be deleted
	bool delete_after_teardown;
	struct xio_connection* get_xio_connection();
	void set_xio_connection(struct xio_connection* con, ServerPortal* portal);
	ServerPortal * get_portal_session_event(void * conn_user_context, struct xio_connection* con, enum xio_session_event event);
	ServerPortal * get_portal_msg_event();
private:
	struct xio_session* session;
	struct xio_connection* first_conn;
	struct xio_connection* second_conn;
	ServerPortal *forwarder; //or accepter in case of accept
	ServerPortal *forwardee; //will be null in case of accept
	Context* ctx_forwarder;
	bool is_closing;
	bool forward_mode;
	bool reject_mode;
};

#endif
