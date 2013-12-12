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

void done_event_creating(Context *ctx, int sizeWritten) {
	ctx->event_queue->increase_offset(sizeWritten);

	//need to stop the event queue only if this is the first callback
	if (!ctx->events_num) {
		log(lsDEBUG, "inside a callback - stopping the event queue\n");
		xio_ev_loop_stop(ctx->ev_loop);
	}
	ctx->events_num++;
}

// implementation of the XIO callbacks
int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req, void *cb_prv_data) {
	log(lsDEBUG, "got on_new_session_callback for session=%p\n", session);

	Contexable *cntxbl = (Contexable*) cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();
	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnNewSessionEvent(buf, cntxbl, session,
			req);
	done_event_creating(ctx, sizeWritten);

	return 0;
}

int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg, void *cb_prv_data) {
	log(lsTRACE, "got on_msg_send_complete_callback for msg=%p\n", msg->user_context);

	//must release the message
	Msg *msg_from_pool = (Msg*) msg->user_context;
	msg_from_pool->release_to_pool();

	log(lsTRACE, "finished on_msg_send_complete_callback for msg=%p\n", msg->user_context);
	return 0;
}

int on_msg_callback(struct xio_session *session, struct xio_msg *msg,
		int more_in_batch, void *cb_prv_data) {
	log(lsTRACE, "on_msg_callback is %p. num_iov = %d, len is %d msg is %p\n", msg->user_context, msg->in.data_iovlen, msg->in.data_iov[0].iov_len, msg);

	Contexable *cntxbl = (Contexable*) cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	const int msg_size = (msg->in.data_iovlen > 0) ? msg->in.data_iov[0].iov_len : 0;

	if (msg->user_context == NULL) { //it's a request with a small buffer on server side
		Msg* msg_from_pool = ctx->msg_pool->get_msg_from_pool();
		if (msg_size > 0)
			memcpy(msg_from_pool->get_buf(), msg->in.data_iov[0].iov_base, msg_size);
		msg->user_context = msg_from_pool;
		msg_from_pool->set_xio_msg_req(msg);
		log(lsTRACE, "!!!!!!!!!!!!!! xio_msg is %p\n", msg);
	}

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten;
	if (msg->type == XIO_MSG_TYPE_REQ) { //it's request
		sizeWritten = ctx->events->writeOnReqReceivedEvent(buf, msg->user_context, msg_size, session);
	} else { //it's response
		sizeWritten = ctx->events->writeOnReplyReceivedEvent(buf, msg->user_context, msg_size);
	}

	done_event_creating(ctx, sizeWritten);

	if (msg->type == XIO_MSG_TYPE_REQ) { //it's a request so it is server side

	} else { //it's response so it is client side
		xio_release_response(msg);
	}

	return 0;
}

int on_msg_error_callback(struct xio_session *session, enum xio_status error,
		struct xio_msg *msg, void *cb_prv_data) {
	log(lsDEBUG, "got on_msg_error_callback\n");
	Contexable *cntxbl = (Contexable*) cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnMsgErrorEvent(buf, msg->user_context,
			session, error, msg);
	done_event_creating(ctx, sizeWritten);

	return 0;
}

int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp, void *cb_prv_data) {

	log(lsDEBUG, "got on_session_established_callback\n");
	Contexable *cntxbl = (Contexable*) cb_prv_data;
	Context *ctx = cntxbl->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnSessionEstablishedEvent(buf, cntxbl,
			session, rsp);
	done_event_creating(ctx, sizeWritten);

	return 0;
}

int on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data, void *cb_prv_data) {

	log(lsDEBUG, "got on_session_event_callback. event=%d, cb_prv_data=%p, session=%p, conn=%p\n",
			event_data->event, cb_prv_data, session, event_data->conn);

	Contexable *cntxbl = (Contexable*) cb_prv_data;

	bool isClient = cntxbl->isClient();

	if (cntxbl->onSessionEvent(event_data->event, session)) {
		Context *ctx = cntxbl->get_ctx_class();
		char* buf = ctx->event_queue->get_buffer();
		/*in case it is a client, the java object is represented by cb_prv_data
		/in case it is a server, the java object is represented by xio_session  */
		void *ptrForJava = isClient ? (void*)cntxbl : (void*)session;
		int sizeWritten = ctx->events->writeOnSessionErrorEvent(buf, ptrForJava, event_data);
		done_event_creating(ctx, sizeWritten);
	}
	return 0;
}

int on_buffer_request_callback(struct xio_msg *msg, void *cb_user_context) {
	log(lsDEBUG, "got on_buffer_request_callback\n");

	Contexable *cntxbl = (Contexable*) cb_user_context;
	Context *ctx = cntxbl->get_ctx_class();
	Msg* msg_from_pool = ctx->msg_pool->get_msg_from_pool();
	msg_from_pool->set_xio_msg_fields_for_assign(msg);

	return 0;
}

void on_fd_ready_event_callback(Context *ctx, int fd, int events) {
	log(lsDEBUG, "got on_fd_ready_event_callback\n");

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnFdReadyEvent(buf, fd, events);
	done_event_creating(ctx, sizeWritten);
	return;
}
