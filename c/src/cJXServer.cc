
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








