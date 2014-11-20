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
#include "Utils.h"

#define MODULE_NAME		"ServerSession"
#define SRVSESSION_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define SRVSESSION_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)

ServerSession::ServerSession(xio_session * session, ServerPortal* portal, Context * ctx)
{
	this->is_closing = false;
	this->session = session;
	this->forwarder = portal;
	this->ctx_forwarder = ctx;
	this->delete_after_teardown = false;
	this->forward_mode = false;
	this->first_conn = NULL;
	this->second_conn = NULL;
	this->reject_mode = false;
	this->forwardee = NULL;
}

ServerSession::~ServerSession() {}

bool ServerSession::ignore_disconnect(ServerPortal* portal)
{
	if (!forward_mode) {
		return false; // in accept mode
	}
	// in forward mode
	if (this->forwardee == portal)
		return false;
	return true;
}

struct xio_connection* ServerSession::get_xio_connection()
{
	if (forward_mode)
		return this->second_conn;
	return this->first_conn;
}

void ServerSession::set_xio_connection(struct xio_connection* con, ServerPortal* portal)
{
	if (this->forwarder == portal)
		this->first_conn = con;
	else
		this->second_conn = con;
}

ServerPortal * ServerSession::get_portal_session_event(void * conn_user_context, struct xio_connection* con, enum xio_session_event event)
{
	if (event == XIO_SESSION_TEARDOWN_EVENT) {
		if (this->forwardee)
			return this->forwardee;
		else
			return this->forwarder;
	}
	if (event == XIO_SESSION_NEW_CONNECTION_EVENT)
		return (ServerPortal*) conn_user_context;

	if (this->second_conn == con)
		return this->forwardee;
	else
		return this->forwarder;
}

ServerPortal * ServerSession::get_portal_msg_event()
{
	if (forward_mode)
		return this->forwardee;
	return this->forwarder;
}



