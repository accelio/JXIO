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

#include "ServerPortal.h"

ServerPortal::ServerPortal(const char *url, long ptrCtx) {
	log(lsDEBUG, "inside startServerNative method\n");

	error_creating = false;

	struct xio_session_ops server_ops;
	memset(&server_ops, 0, sizeof(server_ops));
	server_ops.on_new_session = on_new_session_callback;
	server_ops.on_session_event = on_session_event_callback;
	server_ops.on_msg = on_msg_callback; //TODO: to separate into 2 different classes
	server_ops.on_msg_send_complete = on_msg_send_complete_callback;
	server_ops.assign_data_in_buf = on_buffer_request_callback;

	struct xio_context *ctx;
	Context *ctxClass = (Context *) ptrCtx;
	set_ctx_class(ctxClass);

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, &this->port, 0,
			this);

	if (this->server == NULL) {
		log(lsERROR, "Error in binding server\n");
		error_creating = true;
	}

	log(lsDEBUG, "****** port number is %d\n", this->port);
}

ServerPortal::~ServerPortal() {
	if (error_creating) {
		return;
	}

	if (xio_unbind(this->server)) {
		log(lsERROR, "Error xio_unbind failed\n");
	}
}

bool ServerPortal::accept(struct xio_session *session, const char * url) {
	log(lsDEBUG, "****** url before forward is %s. xio_session is %p\n", url, session);

	int retVal = xio_accept(session, &url, 1, NULL, 0);
	if (retVal) {
		log(lsERROR, "Error in accepting session. error %d\n", retVal);
		return false;
	}
	return true;
}

bool ServerPortal::onSessionEvent(xio_session_event eventType,
		struct xio_session *session) {
	switch (eventType) {
	case XIO_SESSION_CONNECTION_CLOSED_EVENT: //event created because user on this side called "close"
		log(lsINFO,
				"got XIO_SESSION_CONNECTION_CLOSED_EVENT. must close the session\n");
		return false;

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_ERROR_EVENT\n");
		close_xio_connection(session, this->get_ctx_class()->ctx);
		return false;

	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT: //event created "from underneath"
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_DISCONNECTED_EVENT\n");
		close_xio_connection(session, this->get_ctx_class()->ctx);
		return false;

	case XIO_SESSION_TEARDOWN_EVENT:
		log(lsINFO, "got XIO_SESSION_TEARDOWN_EVENT.\n");
		//the event should also be written to buffer to let user know that the session was closed
		return true;
	default:
		log(lsWARN,
				"UNHANDLED event: got '%s' event (%d). \n", xio_session_event_str(eventType), eventType);
		return true;
	}
}

