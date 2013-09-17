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
#include <string.h>
#include <map>


#include "CallbackFunctions.h"

void done_event_creating(Context *ctx, int sizeWritten)
{
	ctx->event_queue->increase_offset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->events_num){
		log (lsDEBUG, "inside a callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->ev_loop);
	}
	ctx->events_num++;
}


// implementation of the XIO callbacks
int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_new_session_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();
	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnNewSessionEvent(buf, cntxbl, session, req, cb_prv_data);

	done_event_creating(ctx, sizeWritten);

	log (lsDEBUG, "the end of new session callback\n");

	return 0;
}

int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_msg_send_complete_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnMsgSendCompleteEvent(buf, cntxbl, session, msg, cb_prv_data);

	done_event_creating(ctx, sizeWritten);
		
	log (lsDEBUG, "finished on_msg_send_complete_callback\n");
	return 0;
}

int on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data)
{
	log (lsDEBUG, "got on_msg_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
//	int sizeWritten = ctx->events->writeOnMsgReceivedEvent(buf, cntxbl, session, msg, more_in_batch, cb_prv_data);
	int sizeWritten = ctx->events->writeOnMsgReceivedEvent(buf, msg->user_context, session, msg, more_in_batch, cb_prv_data);
	done_event_creating(ctx, sizeWritten);

	if (msg->type == XIO_MSG_TYPE_REQ){ //it's a request so it is server side

	}else{//it's response so it is client side
		xio_release_response (msg);
	}

	return 0;
}

int on_msg_error_callback(struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context)
{
	log (lsDEBUG, "got on_msg_error_callback\n");
	Contexable *cntxbl = (Contexable*)conn_user_context;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
//	int sizeWritten = ctx->events->writeOnMsgErrorEvent(buf, cntxbl, session, error, msg, conn_user_context);
	int sizeWritten = ctx->events->writeOnMsgErrorEvent(buf, msg->user_context, session, error, msg, conn_user_context);
	done_event_creating(ctx, sizeWritten);

	return 0;
}

int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data)
{

	log (lsDEBUG, "got on_session_established_callback\n");
	Contexable *cntxbl = (Contexable*)cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnSessionEstablishedEvent(buf, cntxbl, session, rsp, cb_prv_data);
	done_event_creating(ctx, sizeWritten);

	return 0;
}

int on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data)
{

	log (lsDEBUG, "got on_session_event_callback\n");

	Contexable *cntxbl = (Contexable*)cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	if (cntxbl->onSessionEvent(event_data->event)){
		char* buf = ctx->event_queue->get_buffer();
		int sizeWritten = ctx->events->writeOnSessionErrorEvent(buf, cntxbl, session, event_data, cb_prv_data);
		done_event_creating(ctx, sizeWritten);
	}
	return 0;
}














