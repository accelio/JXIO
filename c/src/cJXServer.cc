#include "cJXCtx.h"
#include "cJXServer.h"





cJXServer::cJXServer(const char	*hostname, int port, long ptrCtx){

	printf("inside startServerNative method\n");

	struct xio_server_ops server_ops;
	server_ops.on_session_event			    =  on_session_event_callback;
	server_ops.on_msg	  		        	=  on_msg_callback; //TODO: to separate into 2 different classes
	server_ops.on_msg_error				    =  NULL;
	server_ops.on_new_session		        =  on_new_session_callback;
	server_ops.on_msg_send_complete         =  NULL;



	char			url[256];
	struct xio_context	*ctx;

	cJXCtx *ctxClass = (cJXCtx *)ptrCtx;
	this->ctx = ctxClass;


		/* create url to connect to */
	sprintf(url, "rdma://%s:%d", hostname, port);

	/* bind a listener server to a portal/url */

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, this->ctx);
	if (server == NULL){
		printf("Error in binding server\n");
	}

}


cJXServer::~cJXServer(){

	if (xio_unbind (this->server)){
		fprintf(stderr, "Error, xio_unbind failed");
	}

}








