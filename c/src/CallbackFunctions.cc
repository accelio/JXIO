
#include <string.h>
#include <map>


#include "CallbackFunctions.h"





// implementation of the XIO callbacks

int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_new_session_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	cJXCtx *ctx = cntxbl->getCtxClass();
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnNewSessionEvent(buf, cntxbl, session, req, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_new_session_callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;


	log (lsDEBUG, "the end of new session callback\n");

	return 0;
}




int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_msg_send_complete_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	cJXCtx *ctx = cntxbl->getCtxClass();

	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgSendCompleteEvent(buf, cntxbl, session, msg, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);
		
	log (lsDEBUG, "finished on_msg_send_complete_callback\n");
	return 0;
}




int on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_msg_callback\n");
//	cJXSession *ses = (cJXSession*)cb_prv_data;
//	cJXCtx *ctx = ses->ctx;;

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	cJXCtx *ctx = cntxbl->getCtxClass();

	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgReceivedEvent(buf, cntxbl, session, msg, more_in_batch, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_msg_callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;

	return 0;
}


int on_msg_error_callback(struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context)
{
	log (lsDEBUG, "got on_msg_error_callback\n");
//	cJXSession *ses = (cJXSession*)conn_user_context;
//	cJXCtx *ctx = ses->ctx;;
	Contexable *cntxbl = (Contexable*)conn_user_context;
	cJXCtx *ctx = cntxbl->getCtxClass();

//	cJXCtx *ctx = (cJXCtx*)conn_user_context;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgErrorEvent(buf, cntxbl, session, error, msg, conn_user_context);
	ctx->eventQueue->increaseOffset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_msg_error_callback - stopping the event queue\n");
			xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;
	return 0;
}



//Katya
int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data){

	log (lsDEBUG, "got on_session_established_callback\n");
//	cJXSession *ses = (cJXSession*)cb_prv_data;
//	cJXCtx *ctx = ses->ctx;
	Contexable *cntxbl = (Contexable*)cb_prv_data;
	cJXCtx *ctx = cntxbl->getCtxClass();

//	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnSessionEstablishedEvent(buf, cntxbl, session, rsp, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_session_established_callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;

	return 0;
}

int on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data){

	log (lsDEBUG, "got on_session_event_callback\n");

//	cJXSession *ses = (cJXSession*)cb_prv_data;
//	cJXCtx *ctx = ses->ctx;;
//	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	Contexable *cntxbl = (Contexable*)cb_prv_data;
	cJXCtx *ctx = cntxbl->getCtxClass();

	switch (event_data->event){
	case (XIO_SESSION_CONNECTION_CLOSED_EVENT):
		printf("got XIO_SESSION_CONNECTION_CLOSED_EVENT. must close the session\n");
//		log (lsINFO, "got XIO_SESSION_CONNECTION_CLOSED_EVENT. must close the session\n");
		//connection closed by choice - must close the session
//		ses->closeSession();

		break;
	case(XIO_SESSION_TEARDOWN_EVENT):
	{
		printf("got XIO_SESSION_TEARDOWN_EVENT. must delete session class\n");


		std::map<void*,cJXSession*>::iterator it;
		it = ctx->mapSession->find(session);

		if (it == ctx->mapSession->end()){
			printf ("error! no entry for this ctx\n");
		}else{
			//delete from map
			cJXSession * ses = it->second;
			ses->closeSession();
			delete (ses);
			ctx->mapSession->erase(it);
		}
		//the event should also be written to buffer to let user know that the session was closed
//		break;
	}
	default:

		char* buf = ctx->eventQueue->getBuffer();
		int sizeWritten = ctx->events->writeOnSessionErrorEvent(buf, cntxbl, session, event_data, cb_prv_data);
		ctx->eventQueue->increaseOffset(sizeWritten);

		//need to stop the event queue only if this is the first callback
		if (!ctx->eventsNum){
			log (lsDEBUG, "inside on_session_event_callback - stopping the event queue\n");
			xio_ev_loop_stop(ctx->evLoop);
		}
		ctx->eventsNum++;

	}


	return 0;
	//XIO_SESSION_TEARDOWN_EVENT

//	ctx = (cJXCtx*)cb_prv_data;



}














