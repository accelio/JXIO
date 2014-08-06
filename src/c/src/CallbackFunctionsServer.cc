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
#include "CallbackFunctionsServer.h"

//
// implementation of the XIO callbacks for server
//

int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req, void *cb_prv_data)
{
	LOG_DBG("got on_new_session_callback for session=%p", session);

	ServerPortal *portal = (ServerPortal*)cb_prv_data;
	Context *ctx = portal->get_ctx_class();
	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnNewSessionEvent(buf, portal, session, req);
	ctx->done_event_creating(sizeWritten);
	return 0;
}

int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg, void *cb_prv_data)
{
	LOG_TRACE("got on_msg_send_complete_callback for msg=%p", msg->user_context);

	//must release the message
	Msg *msg_from_pool = (Msg*) msg->user_context;
	msg_from_pool->release_to_pool();

	LOG_TRACE("finished on_msg_send_complete_callback for msg=%p", msg->user_context);
	return 0;
}

int on_msg_callback_server(struct xio_session *session, struct xio_msg *msg,
		int more_in_batch, void *cb_prv_data)
{
	const int msg_in_size = get_xio_msg_in_size(msg);
	const int msg_out_size = get_xio_msg_out_size(msg);

	LOG_TRACE("on_msg_callback portal=%p, num_iov=%d, len: in=%d out=%d, msg=%p", msg->user_context, msg->in.data_iov.nents, msg_in_size, msg_out_size, msg);
	if (msg->status) {
		LOG_ERR("xio_msg=%p completed with error.[%s]", msg, xio_strerror(msg->status));
	}

	ServerPortal *portal = (ServerPortal*)cb_prv_data;
	Context *ctx = portal->get_ctx_class();
	struct xio_iovec_ex *sglist = vmsg_sglist(&msg->in);

	//checking if it's a request with a small buffer on server side

	bool need_copy = false;
	Msg* msg_from_pool;
	if (msg->user_context == NULL){
		//first time ever that this xio_msg is received -
		//assign was not called
		need_copy = true;
		msg_from_pool = ctx->msg_pools.get_msg_from_pool(msg_in_size, msg_out_size);
	}else{
		msg_from_pool = (Msg*)msg->user_context;
		need_copy = !(msg_from_pool->was_assign_called());
	}
	if (need_copy){
		if (msg_in_size > 0)
			memcpy(msg_from_pool->get_buf(), sglist[0].iov_base, msg_in_size);
		msg->user_context = msg_from_pool;
		msg_from_pool->set_xio_msg_req(msg);
	}

	char* buf = ctx->event_queue->get_buffer();
	int sizeWritten = ctx->events->writeOnRequestReceivedEvent(buf, msg->user_context, msg_in_size, msg_out_size, session);

	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_msg_error_callback_server(struct xio_session *session, enum xio_status error,
		struct xio_msg *msg, void *cb_prv_data)
{
	LOG_DBG("got on_msg_error_callback for msg=%p. error status is %d", msg->user_context, error);
	if (error == XIO_E_MSG_DISCARDED){
		//since user discarded this msg, he does not need this notification
		return 0;
	}

	ServerPortal *portal = (ServerPortal*)cb_prv_data;
	Context *ctx = portal->get_ctx_class();

	char* buf = ctx->event_queue->get_buffer();
	//this is server side - send of the response failed
	int sizeWritten  = ctx->events->writeOnMsgErrorEventServer(buf, msg->user_context,
						session, error);
	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_session_event_callback_server(struct xio_session *session,
		struct xio_session_event_data *event_data, void *cb_prv_data)
{

	LOG_DBG("got on_session_event_callback. event=%d, cb_prv_data=%p, session=%p, conn=%p",
			event_data->event, cb_prv_data, session, event_data->conn);
	ServerPortal *portal = (ServerPortal*)cb_prv_data;

	Context *ctx = portal->ctxForSessionEvent(event_data, session);
	if (ctx) {
		char* buf = ctx->event_queue->get_buffer();
		int sizeWritten = ctx->events->writeOnSessionErrorEvent(buf, session, event_data);
		ctx->done_event_creating(sizeWritten);
		if (portal->flag_to_delete){
			portal->deleteObject();
		}
	}
	return 0;
}

int on_buffer_request_callback(struct xio_msg *msg, void *cb_user_context)
{
	LOG_TRACE("got on_buffer_request_callback");
	ServerPortal *portal = (ServerPortal*)cb_user_context;
	Context *ctx = portal->get_ctx_class();
	const int msg_in_size = get_xio_msg_in_size(msg);
	const int msg_out_size = get_xio_msg_out_size(msg);
	Msg* msg_from_pool = ctx->msg_pools.get_msg_from_pool(msg_in_size, msg_out_size);
	msg_from_pool->set_xio_msg_fields_for_assign(msg);
	return 0;
}


