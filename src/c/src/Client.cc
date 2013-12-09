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

#include "Client.h"

Client::Client(const char* url, long ptrCtx) {
	struct xio_session_ops ses_ops;
	struct xio_session_attr attr;
	error_creating = false;

	struct xio_msg *req;

	Context *ctxClass = (Context *) ptrCtx;
	set_ctx_class(ctxClass);

	//defining structs to send to xio library
	ses_ops.on_session_event = on_session_event_callback;
	ses_ops.on_session_established = on_session_established_callback;
	ses_ops.on_msg = on_msg_callback;
	ses_ops.on_msg_error = NULL;

	attr.ses_ops = &ses_ops; /* callbacks structure */
	attr.user_context = NULL; /* no need to pass the server private data */
	attr.user_context_len = 0;

	this->session = xio_session_open(XIO_SESSION_REQ, &attr, url, 0, 0, this);

	if (session == NULL) {
		log(lsERROR, "Error in creating session of Client=%p, ctxClass=%p\n", this, ctxClass);
		error_creating = true;
		return;
	}

	/* connect the session  */
	this->con = xio_connect(session, ctxClass->ctx, 0, NULL, this);

	if (con == NULL) {
		log(lsERROR, "Error in creating connection in Client=%p. ctxClass=%p\n", this, ctxClass);
		goto cleanupSes;

	}

	log(lsDEBUG, "c-tor of Client %p finished.\n", this);

	return;

//cleanupCon:
//	xio_disconnect(this->con);

	cleanupSes: xio_session_close(this->session);
	error_creating = true;
	return;
}

Client::~Client() {
	if (xio_session_close(session)) {
		log(lsERROR, "Error xio_session_close failed. client=%p\n", this);
	}
	log(lsDEBUG, "done deleting Client=%p.\n", this);
}

bool Client::close_connection() {
	if (xio_disconnect(this->con)) {
		log(lsERROR, "xio_disconnect failed. client=%p\n", this);
		return false;
	}

	log(lsDEBUG, "connection closed successfully. client=%p\n", this);
	return true;
}

bool Client::onSessionEvent(xio_session_event eventType,
		struct xio_session *session) {
	switch (eventType) {
	case XIO_SESSION_CONNECTION_CLOSED_EVENT: //event created because user on this side called "close"
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_CLOSED_EVENT in client=%p\n", this);
		return false;

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_ERROR_EVENT in client=%p\n", this);
		close_connection();
		return false;

	case XIO_SESSION_NEW_CONNECTION_EVENT:
		log(lsDEBUG, "got XIO_SESSION_NEW_CONNECTION_EVENT in client=%p\n", this);
		return false;

	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT: //event created "from underneath"
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_DISCONNECTED_EVENT in client=%p\n", this);
		close_connection();
		return false;

	case XIO_SESSION_TEARDOWN_EVENT:
		log(lsDEBUG, "got XIO_SESSION_TEARDOWN_EVENT. must delete session class in client=%p\n", this);
		delete (this);
		//the event should also be written to buffer to let user know that the session was closed
		return true;

	case XIO_SESSION_REJECT_EVENT:
		log(lsDEBUG, "got XIO_SESSION_REJECT_EVENT. must delete session class in client=%p\n", this);
		return true;

	case XIO_SESSION_ERROR_EVENT:
	default:
		log(lsWARN, "UNHANDLED event: got '%s' event (%d).in client=%p\n",  xio_session_event_str(eventType), eventType, this);
		return true;
	}
}

bool Client::send_msg(Msg *msg) {
	log(lsTRACE, "##################### sending msg=%p in client=%p\n", msg, this);
	int ret_val = xio_send_request(this->con, msg->get_xio_msg());
	if (ret_val) {
		log(lsERROR, "Got error %d while sending xio_msg in client=%p\n", ret_val, this);
		return false;
	}
	return true;
}
