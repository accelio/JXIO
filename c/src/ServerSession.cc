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


#include "ServerSession.h"

ServerSession::ServerSession(const char *url, long ptrCtx)
{
	log (lsDEBUG, "inside startServerNative method\n");

	error_creating = false;

	struct xio_session_ops server_ops;
	memset (&server_ops, 0, sizeof(server_ops));
	server_ops.on_session_event     =  on_session_event_callback;
	server_ops.on_msg               =  on_msg_callback; //TODO: to separate into 2 different classes
	server_ops.on_msg_send_complete =  on_msg_send_complete_callback;
	server_ops.assign_data_in_buf   =  on_buffer_request_callback;

	this->session = NULL;

	struct xio_context *ctx;
	Context *ctxClass = (Context *)ptrCtx;
	set_ctx_class(ctxClass);

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, &this->port, 0, this);

	if (this->server == NULL) {
		log (lsERROR, "Error in binding server\n");
		error_creating = true;
	}
	log (lsDEBUG, "****** port number is %d\n",this->port);

	this->closingInProcess = false;
}

ServerSession::~ServerSession()
{
	if (!error_creating) {
		if (session) {
			if (xio_session_close(session)) {
				log (lsERROR, "Error xio_session_close failed\n");
			}
		}
		if (xio_unbind (this->server)){
			log (lsERROR, "Error xio_unbind failed\n");
		}
	}
}

bool ServerSession::accept(struct xio_session *session, const char * url)
{
	log (lsDEBUG, "****** url before forward is %s\n", url);

	int retVal = xio_accept (session, &url, 1, NULL, 0);
	if (retVal){
		log (lsERROR, "Error in accepting session. error %d\n", retVal);
		return false;
	}
	this->session = session;
	return true;
}

bool ServerSession::close()
{
	this->closingInProcess = true;

	// Active connection can be added or removed only if there is an active session
	if (!this->session)
		return true;

	xio_connection * con = xio_get_connection(this->session, this->get_ctx_class()->ctx);
	if (con != NULL) {
		if (xio_disconnect(con)) {
			log(lsERROR, "xio_disconnect failed");
			return false;
		}
	}
	return true;
}

bool ServerSession::onSessionEvent(xio_session_event eventType)
{
	switch (eventType) {
	case XIO_SESSION_TEARDOWN_EVENT:
		log(lsDEBUG, "got XIO_SESSION_TEARDOWN_EVENT (%d). must delete session class\n", eventType);
		delete (this);
		return true;

	case XIO_SESSION_CONNECTION_CLOSED_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_CLOSED_EVENT (%d)\n", eventType);
		if (closingInProcess) {
			close();
		}
		return false;

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		log(lsDEBUG, "got XIO_SESSION_CONNECTION_ERROR_EVENT (%d)\n", eventType);
		return true;

	case XIO_SESSION_REJECT_EVENT:
	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT:
	case XIO_SESSION_ERROR_EVENT:
	default:
		log(lsWARN, "UNHANDLED event: got '%s' event (%d). \n", xio_session_event_str(eventType), eventType);
		return true;
	}
}

bool ServerSession::send_reply(Msg *msg)
{
	log(lsDEBUG, "inside Server::send_reply xio_msg is %p. msg is %p\n", msg->get_xio_msg(), msg);
	//set_xio_msg????
	//TODO : make sure that this function is not called in the fast path
	msg->set_xio_msg_server_fields();
	msg->dump();
	int ret_val = xio_send_response(msg->get_xio_msg());
	if (ret_val){
		log(lsERROR, "Got error %d while sending xio_msg\n", ret_val);
		return false;
	}
	return true;
}
