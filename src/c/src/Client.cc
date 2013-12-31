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
	this->error_creating = false;
	this->is_closing = false;

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

Client::~Client() {}

bool Client::close_session() {
	if (xio_session_close(session)) {
		log(lsERROR, "Error '%s' (%d) xio_session_close failed. client=%p\n", xio_strerror(xio_errno()), xio_errno(), this);
		return false;
	}

	log(lsDEBUG, "session closed successfully. client=%p\n", this);
	return true;
}

bool Client::close_connection() {
	if (xio_disconnect(this->con)) {
		log(lsERROR, "xio_disconnect failed with error '%s' (%d). client=%p\n", xio_strerror(xio_errno()), xio_errno(), this);
		return false;
	}

	log(lsDEBUG, "connection closed successfully. client=%p\n", this);
	return true;
}

Context* Client::ctxForSessionEvent(xio_session_event eventType, struct xio_session *session) {
	Context *ctx;
	switch (eventType) {
	case XIO_SESSION_CONNECTION_CLOSED_EVENT: //event created because user on this side called "close"
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_CLOSED_EVENT in client=%p\n", this);
		return NULL;

	case XIO_SESSION_CONNECTION_TEARDOWN_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_TEARDOWN_EVENT. \n");
		return NULL;

	case XIO_SESSION_NEW_CONNECTION_EVENT:
		log(lsDEBUG, "got XIO_SESSION_NEW_CONNECTION_EVENT in client=%p\n", this);
		return NULL;

	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT: //event created "from underneath"
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_DISCONNECTED_EVENT in client=%p\n", this);
		close_connection();
		return NULL;

	case XIO_SESSION_TEARDOWN_EVENT:
		log(lsDEBUG, "got XIO_SESSION_TEARDOWN_EVENT. must delete session class in client=%p\n", this);
		this->is_closing = true;
		//the event should also be written to buffer to let user know that the session was closed
		close_session();
		return this->get_ctx_class();

	case XIO_SESSION_REJECT_EVENT:
		log(lsDEBUG, "got XIO_SESSION_REJECT_EVENT. must delete session class in client=%p\n", this);
		return this->get_ctx_class();

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_ERROR_EVENT in client=%p\n", this);
		close_connection();
		return NULL;

	case XIO_SESSION_ERROR_EVENT:
	default:
		log(lsWARN, "UNHANDLED event: got '%s' event (%d).in client=%p\n",  xio_session_event_str(eventType), eventType, this);
		return this->get_ctx_class();
	}
}

bool Client::send_msg(Msg *msg, const int size) {
	if (this->is_closing){
		log(lsDEBUG, "attempting to send a message while client session is closing.\n");
		return false;
	}
	log(lsTRACE, "##################### sending msg=%p, size=%d in client=%p\n", msg, size, this);
	msg->set_xio_msg_out_size(size);
	msg->reset_xio_msg_in_size();
	int ret_val = xio_send_request(this->con, msg->get_xio_msg());
	if (ret_val) {
		log(lsERROR, "Got error '%s' (%d) while sending xio_msg in client=%p\n", xio_strerror(xio_errno()), xio_errno(), this);
		return false;
	}
	return true;
}
