
#include "cJXSession.h"

#define K_DEBUG 1

cJXSession::cJXSession(const char*	url, long ptrCtx){

	struct xio_session_ops ses_ops;
	struct xio_session_attr attr;
//	char			url[256];
	errorCreating = false;

	struct xio_msg *req;

	cJXCtx *ctxClass = (cJXCtx *)ptrCtx;
	setCtxClass(ctxClass);

//	sprintf(url, "rdma://%s:%d", hostname, port);

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
		errorCreating = true;
		return;
	}

		/* connect the session  */
	this->con = xio_connect(session, ctxClass->ctx, 0, this);

	if (con == NULL){
		log (lsERROR, "Error in creating connection\n");
		goto cleanupSes;

	}


	if (ctxClass->mapSession == NULL){
		ctxClass->mapSession = new std::map<void*,cJXSession*> ();
		if(ctxClass->mapSession== NULL){
			log (lsERROR, "Error, Could not allocate memory\n");
			goto cleanupCon;
		}
	}

	ctxClass->mapSession->insert(std::pair<void*, cJXSession*>(session, this));

	printf("startClientSession done with \n");

	log (lsDEBUG, "****** inside c-tor of session private data is %p\n",this);


	//	 printf("for debugging \n");
	#ifdef K_DEBUG
		 req = (struct xio_msg *) malloc(sizeof(struct xio_msg));

		 // create "hello world" message
		 memset(req, 0, sizeof(req));
		 req->out.header.iov_base = strdup("hello world header request");
		 req->out.header.iov_len = strlen("hello world header request");
		 	// send first message
		 xio_send_request(con, req);
	//	 printf("done debugging \n");
	#endif

cleanupCon:
		xio_disconnect(this->con);

cleanupSes:
		xio_session_close(this->session);
		errorCreating = true;
}


cJXSession::~cJXSession(){

}

bool cJXSession::closeConnection(){


	if (xio_disconnect (this->con)){
		fprintf(stderr, "Error, xio_disconnect failed");
		return false;
	}

	log (lsDEBUG, "connection closed successfully\n");
	return true;
}

int cJXSession::closeSession(){
	return xio_session_close(session);
}







