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
#include "cJXServer.h"


cJXServer::cJXServer(const char	*url, long ptrCtx){

	printf("inside startServerNative method\n");

	errorCreating = false;

	struct xio_server_ops server_ops;
	server_ops.on_session_event			    =  on_session_event_callback;
	server_ops.on_msg	  		        	=  on_msg_callback; //TODO: to separate into 2 different classes
	server_ops.on_msg_error				    =  NULL;
	server_ops.on_new_session		        =  on_new_session_callback;
	server_ops.on_msg_send_complete         =  NULL;


	struct xio_context	*ctx;

	cJXCtx *ctxClass = (cJXCtx *)ptrCtx;
	setCtxClass(ctxClass);

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, this);
	if (server == NULL){
		log (lsERROR, "Error in binding server\n");
		errorCreating = true;
	}
	log (lsDEBUG, "****** inside c-tor of server private data is %p\n",this);

	this->port = 5678; //TODO: some global shit


}


cJXServer::~cJXServer(){

	if (xio_unbind (this->server)){
		log (lsERROR, "Error xio_unbind failed\n");
	}

}








