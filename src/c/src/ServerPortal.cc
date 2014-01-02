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

#include "Utils.h"
#include "ServerPortal.h"

#define MODULE_NAME		"ServerPortal"
#define SRVPORTAL_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define SRVPORTAL_LOG_WARN(log_fmt, log_args...)  LOG_BY_MODULE(lsWARN, log_fmt, ##log_args)
#define SRVPORTAL_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)


ServerPortal::ServerPortal(const char *url, long ptrCtx)
{
	SRVPORTAL_LOG_DBG("CTOR start");

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

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, &this->port, 0, this);

	if (this->server == NULL) {
		SRVPORTAL_LOG_DBG("ERROR in binding server");
		error_creating = true;
	}
	SRVPORTAL_LOG_DBG("CTOR done (on port=%d)", this->port);
}

ServerPortal::~ServerPortal()
{
	if (error_creating) {
		return;
	}

	if (xio_unbind(this->server)) {
		SRVPORTAL_LOG_ERR("ERROR in xio_unbind: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
	}
	SRVPORTAL_LOG_DBG("DTOR done");
}

Context* ServerPortal::ctxForSessionEvent(xio_session_event eventType, struct xio_session *session)
{
	ServerSession* ses = NULL;
	switch (eventType) {
	case XIO_SESSION_CONNECTION_CLOSED_EVENT: //event created because user on this side called "close"
		SRVPORTAL_LOG_DBG("got XIO_SESSION_CONNECTION_CLOSED_EVENT");
		return NULL;

	case XIO_SESSION_CONNECTION_TEARDOWN_EVENT:
		SRVPORTAL_LOG_DBG("got XIO_SESSION_CONNECTION_TEARDOWN_EVENT");
		return NULL;

	case XIO_SESSION_NEW_CONNECTION_EVENT:
		SRVPORTAL_LOG_DBG("got XIO_SESSION_NEW_CONNECTION_EVENT");
		return NULL;

	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT: //event created "from underneath"
		SRVPORTAL_LOG_DBG("got XIO_SESSION_CONNECTION_DISCONNECTED_EVENT");
		return NULL;

	case XIO_SESSION_TEARDOWN_EVENT:
		SRVPORTAL_LOG_DBG("got XIO_SESSION_TEARDOWN_EVENT");
		//the event should also be written to buffer to let user know that the session was closed
		if (xio_session_destroy(session)) {
			SRVPORTAL_LOG_ERR("Error in xio_session_close: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
		}
		//last event for this session EVER: ses can be deleted from the map, but not deleted
		ses = delete_ses_server_for_session(session);
		ses->is_closing = true;
		return ses->getCtx();

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		SRVPORTAL_LOG_DBG("got XIO_SESSION_CONNECTION_ERROR_EVENT");
		return NULL;

	default:
		SRVPORTAL_LOG_WARN("UNHANDLED event: got event '%s' (%d)", xio_session_event_str(eventType), eventType);
		ses = delete_ses_server_for_session(session);
		ses->is_closing = true;
		return ses->getCtx();
	}
}
