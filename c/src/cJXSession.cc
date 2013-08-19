#include "cJXCtx.h"
#include "cJXSession.h"





cJXSession::cJXSession(const char*	hostname, int port, long ptrCtx){

	struct xio_session_ops ses_ops;
	struct xio_session_attr attr;
	char			url[256];


	cJXCtx *ctxClass = (cJXCtx *)ptrCtx;
	this->ctx = ctxClass;

	sprintf(url, "rdma://%s:%d", hostname, port);

		//defining structs to send to xio library
	ses_ops.on_session_event		=  on_session_event_callback;
	ses_ops.on_session_established		=  on_session_established_callback;
	ses_ops.on_msg				=  on_msg_callback;
	ses_ops.on_msg_error			=  NULL;

	attr.ses_ops = &ses_ops; /* callbacks structure */
	attr.user_context = NULL;	  /* no need to pass the server private data */
	attr.user_context_len = 0;

	this->session = xio_session_open(XIO_SESSION_REQ,
					   &attr, url, 0, ctxClass);

	if (session == NULL){
		printf("Error in creating session\n");
		return;
	}

		/* connect the session  */
	this->con = xio_connect(session, ctxClass->ctx, 0, ctxClass);

	if (con == NULL){
		printf("Error in creating connection\n");
		return;
	}


	printf("startClientSession done with \n");


	//	 printf("for debugging \n");
	#ifdef K_DEBUG
		 struct xio_msg *req = (struct xio_msg *) malloc(sizeof(struct xio_msg));

		 // create "hello world" message
		 memset(req, 0, sizeof(req));
		 req->out.header.iov_base = strdup("hello world header request");
		 req->out.header.iov_len = strlen("hello world header request");
		 	// send first message
		 xio_send_request(con, req);
	//	 printf("done debugging \n");
	#endif

}


cJXSession::~cJXSession(){

}

bool cJXSession::closeConnection(){


	if (xio_disconnect (this->con)){
		fprintf(stderr, "Error, xio_disconnect failed");
		return false;
	}

	return true;
}









