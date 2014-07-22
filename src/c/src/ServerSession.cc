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

ServerSession::ServerSession(xio_session * session, Context* ctx, xio_connection* connection)
{
	this->is_closing = false;
	this->session = session;
	this->ctx = ctx;
	this->delete_after_teardown = false;
	this->forward_mode = false;
	this->first_conn = connection;
	this->reject_mode = false;
}

ServerSession::~ServerSession() {}

bool ServerSession::ignore_disconnect(xio_connection* conn)
{
	if (!forward_mode)
		//accept mode
		return false;
	//this is forward mode
	if (this->first_conn == conn)
		return true;
	return false;
}

struct xio_connection* ServerSession::get_xio_connection()
{
	if (forward_mode)
		return this->second_conn;
	return this->first_conn;
}
