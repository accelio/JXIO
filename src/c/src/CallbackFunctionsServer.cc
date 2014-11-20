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
#include "bullseye.h"
#include "CallbackFunctionsServer.h"

//
// implementation of the XIO callbacks for server
//

int on_new_session_callback(struct xio_session *xio_session, struct xio_new_session_req *req, void *cb_prv_data)
{
	LOG_DBG("got on_new_session_callback for session=%p", xio_session);

	ServerPortal *portal = (ServerPortal*) cb_prv_data;
	Context *ctx = portal->get_ctx_class();

	ServerSession *jxio_session = new ServerSession(xio_session, portal, portal->get_ctx_class());

	/*modifying private data of the session, so we would be able to retrive the
	 * ServerSession when events are received
	 */
	struct xio_session_attr ses_attr;
	ses_attr.user_context = jxio_session;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_modify_session(xio_session, &ses_attr, XIO_SESSION_ATTR_USER_CTX)) {
		LOG_ERR("xio_modify_session for session=%p failed", xio_session);
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	LOG_DBG("xio_modify_session for session=%p to %p", xio_session, jxio_session);

	char* buf = ctx->get_buffer();
	int sizeWritten = ctx->events.writeOnNewSessionEvent(buf, portal, jxio_session, req);
	ctx->done_event_creating(sizeWritten);
	return 0;
}

int on_msg_send_complete_callback(struct xio_session *xio_session, struct xio_msg *msg, void *cb_prv_data)
{
	LOG_TRACE("got on_msg_send_complete_callback for msg=%p", msg->user_context);

	//must release the message
	Msg *msg_from_pool = (Msg*) msg->user_context;
	msg_from_pool->release_to_pool();
	LOG_TRACE("finished on_msg_send_complete_callback for msg=%p", msg->user_context);
	return 0;
}

int on_msg_callback_server(struct xio_session *xio_session, struct xio_msg *msg, int more_in_batch, void *cb_prv_data)
{
	const int msg_in_size = get_xio_msg_in_size(msg);
	const int msg_out_size = get_xio_msg_out_size(msg);

	LOG_TRACE("on_msg_callback portal=%p, num_iov=%d, len: in=%d out=%d, msg=%p", msg->user_context, msg->in.data_iov.nents, msg_in_size, msg_out_size, msg);

	ServerSession *jxio_session = (ServerSession*) cb_prv_data;
	ServerPortal *portal = jxio_session->get_portal_msg_event();

	Context *ctx = portal->get_ctx_class();
	struct xio_iovec_ex *sglist = vmsg_sglist(&msg->in);

	//checking if it's a request with a small buffer on server side

	bool need_copy = false;
	Msg* msg_from_pool;
	if (msg->user_context == NULL) {
		//first time ever that this xio_msg is received -
		//assign was not called
		need_copy = true;
		msg_from_pool = ctx->msg_pools.get_msg_from_pool(msg_in_size, msg_out_size);
	} else {
		msg_from_pool = (Msg*) msg->user_context;
		need_copy = !(msg_from_pool->was_assign_called());
	}
	if (need_copy) {
		if (msg_in_size > 0)
			memcpy(msg_from_pool->get_buf(), sglist[0].iov_base, msg_in_size);
		msg->user_context = msg_from_pool;
		msg_from_pool->set_xio_msg_req(msg);
	}

	char* buf = ctx->get_buffer();
	int sizeWritten = ctx->events.writeOnRequestReceivedEvent(buf, msg->user_context, msg_in_size, msg_out_size, jxio_session);

	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_msg_error_callback_server(struct xio_session *xio_session, enum xio_status error, enum xio_msg_direction direction, struct xio_msg *msg, void *cb_prv_data)
{
	LOG_DBG("got on_msg_error_callback for msg=%p, direction=%d. error status is %d", msg->user_context, direction, error);
	ServerSession *jxio_session = (ServerSession*) cb_prv_data;

	if (error == XIO_E_MSG_DISCARDED) {
		//since user discarded this msg, he does not need this notification
		Msg* msg_from_pool = (Msg*)msg->user_context;
		msg_from_pool->release_to_pool();
		return 0;
	}

	ServerPortal *portal = jxio_session->get_portal_msg_event();

	Context *ctx = portal->get_ctx_class();

	char* buf = ctx->get_buffer();
	//this is server side - send of the response failed
	int sizeWritten = ctx->events.writeOnMsgErrorEventServer(buf, msg->user_context, jxio_session, error);
	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_session_event_callback_server(struct xio_session *xio_session, struct xio_session_event_data *event_data, void *cb_prv_data) {

	LOG_DBG("got on_session_event_callback. event=%d, reason=%d, cb_prv_data=%p, session=%p, conn=%p",
			event_data->event, event_data->reason, cb_prv_data, xio_session, event_data->conn);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (cb_prv_data == NULL) {
		LOG_ERR("cb_prv_data is NULL making jxio_session of type ServerSession NULL");
		return 1;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	ServerSession *jxio_session = (ServerSession*) cb_prv_data;
	ServerPortal *portal  = jxio_session->get_portal_session_event(event_data->conn_user_context, event_data->conn, event_data->event);
	Context *ctx = portal->ctxForSessionEvent(event_data, jxio_session);
	if (ctx) {
		char* buf = ctx->get_buffer();
		int sizeWritten = ctx->events.writeOnSessionErrorEvent(buf, jxio_session, event_data);
		ctx->done_event_creating(sizeWritten);
		if (portal->flag_to_delete) {
			portal->deleteObject();
		}
	}
	return 0;
}

int on_buffer_request_callback(struct xio_msg *msg, void *cb_user_context)
{
	LOG_TRACE("got on_buffer_request_callback for msg=%p, user_context=%p", msg, cb_user_context);
	ServerSession *jxio_session = (ServerSession*) cb_user_context;
	ServerPortal *portal = jxio_session->get_portal_msg_event();
	Context *ctx = portal->get_ctx_class();
	const int msg_in_size = get_xio_msg_in_size(msg);
	const int msg_out_size = get_xio_msg_out_size(msg);
	Msg* msg_from_pool = ctx->msg_pools.get_msg_from_pool(msg_in_size, msg_out_size);
	msg_from_pool->set_xio_msg_fields_for_assign(msg);
	return 0;
}
