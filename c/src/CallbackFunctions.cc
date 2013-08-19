
#include <string.h>
#include <map>


#include "CallbackFunctions.h"





// implementation of the XIO callbacks

int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data)
{

	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	char* buf = ctx->eventQueue->getBuffer();

	int sizeWritten = ctx->events->writeOnNewSessionEvent(buf, session, req, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);


	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_new_session_callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;


	log (lsDEBUG, "the end of new session callback");

	return 0;
}




int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data)
{
	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgSendCompleteEvent(buf, session, msg, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);
		
	return 0;
}




int on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data)
{


	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgReceivedEvent(buf, session, msg, more_in_batch, cb_prv_data);
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
	cJXCtx *ctx = (cJXCtx*)conn_user_context;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnMsgErrorEvent(buf, session, error, msg, conn_user_context);
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

	cJXCtx *ctx = (cJXCtx*)cb_prv_data;
	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnSessionEstablishedEvent(buf, session, rsp, cb_prv_data);
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


	cJXCtx * ctx;
	
	if (event_data->event == XIO_SESSION_CONNECTION_CLOSED_EVENT){
		log (lsINFO, "got XIO_SESSION_CONNECTION_CLOSED_EVENT. must close the session");
		//connection closed by choice - must close the session
//		cJXSession* ses = (cJXSession*)cb_prv_data;

		int ret_val = xio_session_close(session);
		if (!ret_val){
			log (lsERROR, "error in closing session");
		}
		return 0;
	}

	ctx = (cJXCtx*)cb_prv_data;


	char* buf = ctx->eventQueue->getBuffer();
	int sizeWritten = ctx->events->writeOnSessionErrorEvent(buf, session, event_data, cb_prv_data);
	ctx->eventQueue->increaseOffset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->eventsNum){
		log (lsDEBUG, "inside on_session_event_callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->evLoop);
	}
	ctx->eventsNum++;

}














