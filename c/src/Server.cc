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
#include "Server.h"


Server::Server(const char	*url, long ptrCtx)
{

	log (lsDEBUG, "inside startServerNative method\n");

	error_creating = false;

	struct xio_server_ops server_ops;
	server_ops.on_session_event			    =  on_session_event_callback;
	server_ops.on_msg	  		        	=  on_msg_callback; //TODO: to separate into 2 different classes
	server_ops.on_msg_error				    =  NULL;
	server_ops.on_new_session		        =  on_new_session_callback;
	server_ops.on_msg_send_complete         =  NULL;

	this->session = NULL;

	struct xio_context	*ctx;

	Context *ctxClass = (Context *)ptrCtx;
	set_ctx_class(ctxClass);

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, this);
	if (server == NULL){
		log (lsERROR, "Error in binding server\n");
		error_creating = true;
	}
	log (lsDEBUG, "****** inside c-tor of server private data is %p\n",this);

	this->port = 5678; //TODO: some global shit
}

Server::~Server()
{
	if (session){
		if (xio_session_close(session)){
			log (lsERROR, "Error xio_session_close failed\n");
		}
	}
	if (xio_unbind (this->server)){
		log (lsERROR, "Error xio_unbind failed\n");
	}
}


bool Server::forward(struct xio_session *session, const char * url){
	int retVal = xio_accept (session, &url, 1, NULL, 0);
	if (retVal){
		log (lsERROR, "Error in accepting session. error %d\n", retVal);
		return false;
	}
	this->session = session;
	return true;
}



bool Server::onSessionEvent(int eventType){
	switch (eventType){
		case (XIO_SESSION_CONNECTION_CLOSED_EVENT):
			log (lsINFO, "got XIO_SESSION_CONNECTION_CLOSED_EVENT\n");
			return false;

		case(XIO_SESSION_CONNECTION_ERROR_EVENT):
			log (lsDEBUG, "got XIO_SESSION_CONNECTION_ERROR_EVENT\n");
			return true;

		case(XIO_SESSION_TEARDOWN_EVENT):
			log(lsINFO, "got XIO_SESSION_TEARDOWN_EVENT. must delete session class\n");
			delete (this);
			return true;
		default:
			log(lsINFO, "got event %d. \n", eventType);
			return true;
	}

}



