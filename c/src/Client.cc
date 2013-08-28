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

#define K_DEBUG 1

Client::Client(const char*	url, long ptrCtx)
{

	struct xio_session_ops ses_ops;
	struct xio_session_attr attr;
//	char			url[256];
	error_creating = false;

	struct xio_msg *req;

	Context *ctxClass = (Context *)ptrCtx;
	set_ctx_class(ctxClass);

	//defining structs to send to xio library
	ses_ops.on_session_event		=  on_session_event_callback;
	ses_ops.on_session_established		=  on_session_established_callback;
	ses_ops.on_msg				=  on_msg_callback;
	ses_ops.on_msg_error			=  NULL;

	attr.ses_ops = &ses_ops; /* callbacks structure */
	attr.user_context = NULL;	  /* no need to pass the server private data */
	attr.user_context_len = 0;

	this->session = xio_session_open(XIO_SESSION_REQ,
					   &attr, url, 0, this);

	if (session == NULL){
		log (lsERROR, "Error in creating session\n");
		error_creating = true;
		return;
	}

		/* connect the session  */
	this->con = xio_connect(session, ctxClass->ctx, 0, this);

	if (con == NULL){
		log (lsERROR, "Error in creating connection\n");
		goto cleanupSes;

	}

	if (ctxClass->map_session == NULL){
		ctxClass->map_session = new std::map<void*,Client*> ();
		if(ctxClass->map_session== NULL){
			log (lsERROR, "Error, Could not allocate memory\n");
			goto cleanupCon;
		}
	}

	ctxClass->map_session->insert(std::pair<void*, Client*>(session, this));

	log (lsDEBUG, "startClientSession done with \n");

	#ifdef K_DEBUG
		 req = (struct xio_msg *) malloc(sizeof(struct xio_msg));

		 // create "hello world" message
		 memset(req, 0, sizeof(req));
		 req->out.header.iov_base = strdup("hello world header request");
		 req->out.header.iov_len = strlen("hello world header request");
		 	// send first message
		 xio_send_request(con, req);
	#endif
	return;

cleanupCon:
		xio_disconnect(this->con);

cleanupSes:
		xio_session_close(this->session);
		error_creating = true;
}

Client::~Client()
{
}

bool Client::close_connection()
{
	if (xio_disconnect (this->con)){
		log(lsERROR, "xio_disconnect failed");
		return false;
	}

	log (lsDEBUG, "connection closed successfully\n");
	return true;
}

int Client::close_session()
{
	return xio_session_close(session);
}







