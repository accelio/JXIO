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

#include "bullseye.h"
#include "Utils.h"
#include "Client.h"

#define MODULE_NAME		"Client"
#define CLIENT_LOG_ERR(log_fmt, log_args...)	LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define CLIENT_LOG_WARN(log_fmt, log_args...)	LOG_BY_MODULE(lsWARN, log_fmt, ##log_args)
#define CLIENT_LOG_DBG(log_fmt, log_args...)	LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)
#define CLIENT_LOG_TRACE(log_fmt, log_args...)	LOG_BY_MODULE(lsTRACE, log_fmt, ##log_args)


Client::Client(const char* url, long ptrCtx)
{
	CLIENT_LOG_DBG("CTOR start. create client to connect to %s", url);

	struct xio_session_ops ses_ops;
	struct xio_session_attr attr;
	this->is_closing = false;
	this->con = NULL;

	struct xio_msg *req;

	Context *ctxClass = (Context *) ptrCtx;
	this->ctx_class = ctxClass;

	//defining structs to send to xio library
	ses_ops.on_session_event = on_session_event_callback_client;
	ses_ops.on_session_established = on_session_established_callback;
	ses_ops.on_msg = on_msg_callback_client;
	ses_ops.on_msg_error = on_msg_error_callback_client;

	attr.ses_ops = &ses_ops; /* callbacks structure */
	attr.user_context = NULL; /* no need to pass the server private data */

	struct xio_session_params params;
	memset(&params, 0, sizeof(params));
	params.type = XIO_SESSION_CLIENT;
	params.ses_ops = &ses_ops;
	params.user_context = this;
	params.uri = (char*)url;
	this->session = xio_session_create(&params);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (session == NULL) {
		CLIENT_LOG_ERR("Error in creating session for Context=%p (errno=%d '%s')", this->ctx_class, xio_errno(), xio_strerror(xio_errno()));
		throw std::bad_alloc();
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	CLIENT_LOG_DBG("CTOR done");
}

bool Client::create_connection()
{
	struct xio_connection_params cparams;
	memset(&cparams, 0, sizeof(cparams));
	cparams.session = session;
	cparams.ctx = this->ctx_class->get_xio_context();
	cparams.conn_user_context = this;

	this->con = xio_connect(&cparams);
 	BULLSEYE_EXCLUDE_BLOCK_START
 	if (con == NULL) {
		CLIENT_LOG_ERR("Error in creating connection for Context=%p", this->ctx_class);
		xio_session_destroy(this->session);
		return false;
 	}
 	BULLSEYE_EXCLUDE_BLOCK_END
	CLIENT_LOG_DBG("create connection done");
	return true;
}

Client::~Client()
{
	CLIENT_LOG_DBG("DTOR done");
}

bool Client::close_connection()
{
	if (this->is_closing) {
		CLIENT_LOG_DBG("trying to close connection while already closing");
		return true;
	}
	this->is_closing = true;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_disconnect(this->con)) {
		CLIENT_LOG_ERR("Error xio_disconnect failure: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	CLIENT_LOG_DBG("connection closed successfully");
	return true;
}

Context* Client::ctxForSessionEvent(struct xio_session_event_data * event, struct xio_session *session)
{
	Context *ctx;
	switch (event->event) {
	case XIO_SESSION_CONNECTION_CLOSED_EVENT: //event created because user on this side called "close"
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_CLOSED_EVENT. Reason=%s (%d)", xio_strerror(event->reason), event->reason);
		this->is_closing = true;
		return this->get_ctx_class();

	case XIO_SESSION_CONNECTION_TEARDOWN_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_TEARDOWN_EVENT. Reason=%s (%d)", xio_strerror(event->reason), event->reason);
		xio_connection_destroy(event->conn);
		return this->get_ctx_class();

	case XIO_SESSION_NEW_CONNECTION_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_NEW_CONNECTION_EVENT");
		return NULL;

	case XIO_SESSION_CONNECTION_ESTABLISHED_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_ESTABLISHED_EVENT");
		return NULL;

	case XIO_SESSION_CONNECTION_REFUSED_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_REFUSED_EVENT. Reason=%s (%d)", xio_strerror(event->reason), event->reason);
		this->is_closing = true;
		return this->get_ctx_class();

	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT: //event created "from underneath"
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_DISCONNECTED_EVENT. Reason=%s (%d)", xio_strerror(event->reason), event->reason);
		this->is_closing = true;
		return this->get_ctx_class();

	case XIO_SESSION_TEARDOWN_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_TEARDOWN_EVENT. must delete session");
		if (!this->is_closing) {
			CLIENT_LOG_ERR("Got session teardown without getting connection/close/disconnected/rejected/refused");
		}
		//the event should also be written to buffer to let user know that the session was closed
		BULLSEYE_EXCLUDE_BLOCK_START
		if (xio_session_destroy(session)) {
			CLIENT_LOG_ERR("Error xio_session_close failure: '%s' (%d) ", xio_strerror(xio_errno()), xio_errno());
		}
		BULLSEYE_EXCLUDE_BLOCK_END
		/* Session teardown will pass to Java as internal event (user will not
		 * get a callback. This is required so ClientSession will be removed from
		 * EQH.eventables only when no more Accelio events will arrive (prevents Thread closure
		 * before all accelio events were received).
		 */
		return this->get_ctx_class();

	case XIO_SESSION_CONNECTION_ERROR_EVENT:
		CLIENT_LOG_DBG("got XIO_SESSION_CONNECTION_ERROR_EVENT. Reason=%s (%d)", xio_strerror(event->reason), event->reason);
		close_connection();
		return NULL;

	BULLSEYE_EXCLUDE_BLOCK_START
	case XIO_SESSION_ERROR_EVENT:
	default:
		CLIENT_LOG_WARN("UNHANDLED event: got event '%s' (%d)",  xio_session_event_str(event->event), event->event);
		return this->get_ctx_class();
	BULLSEYE_EXCLUDE_BLOCK_END
	}
}

int Client::send_msg(Msg *msg, const int out_size, const int in_size, const bool is_mirror)
{
	if (this->is_closing) {
		CLIENT_LOG_DBG("attempting to send a message while client session is closing");
		return XIO_E_SESSION_DISCONNECTED;
	}
	CLIENT_LOG_TRACE("sending msg=%p, out_size=%d, in_size=%d", msg, out_size, in_size);

	struct xio_msg *xio_msg;
	if (is_mirror)
		xio_msg = msg->get_mirror_xio_msg();
	else
		xio_msg = msg->get_xio_msg();

	msg->set_xio_msg_out_size(xio_msg, out_size);
	msg->set_xio_msg_in_size(xio_msg, in_size);
	int ret_val = xio_send_request(this->con, xio_msg);
	if (ret_val) {
		CLIENT_LOG_TRACE("Error in sending xio_msg: '%s' (%d) (ret_val=%d)", xio_strerror(xio_errno()), xio_errno(), ret_val);
		return xio_errno();
	}
	return 0;
}
