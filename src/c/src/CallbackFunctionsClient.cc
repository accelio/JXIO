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
#include "CallbackFunctionsClient.h"

//
// implementation of the XIO callbacks for client
//


int on_msg_callback_client(struct xio_session *session, struct xio_msg *msg,
		int more_in_batch, void *cb_prv_data)
{
	const int msg_in_size = get_xio_msg_in_size(msg);
	const int msg_out_size = get_xio_msg_out_size(msg);

	LOG_TRACE("on_msg_callback client=%p, num_iov=%d, len: in=%d out=%d, msg=%p", msg->user_context, msg->in.data_iov.nents, msg_in_size, msg_out_size, msg);
	Client *client = (Client*)cb_prv_data;
	Context *ctx = client->get_ctx_class();
	struct xio_iovec_ex *sglist = vmsg_sglist(&msg->in);

	char* buf = ctx->get_buffer();
	int sizeWritten = ctx->events.writeOnResponseReceivedEvent(buf, msg->user_context, msg_in_size);
	ctx->done_event_creating(sizeWritten);
	xio_release_response(msg);

	return 0;
}

int on_msg_error_callback_client(struct xio_session *session, enum xio_status error,
		enum xio_msg_direction direction, struct xio_msg *msg, void *cb_prv_data)
{
	LOG_DBG("got on_msg_error_callback for msg=%p, direction=%d. error status is %d", msg->user_context, direction, error);
	Client *client = (Client*)cb_prv_data;
	Context *ctx = client->get_ctx_class();

	char* buf = ctx->get_buffer();
	//this is client side - send of the request failed
	int sizeWritten = ctx->events.writeOnMsgErrorEventClient(buf, msg->user_context, error);
	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp, void *cb_prv_data)
{
	LOG_DBG("got on_session_established_callback");
	Client *client = (Client*)cb_prv_data;
	Context *ctx = client->get_ctx_class();

	char* buf = ctx->get_buffer();
	int sizeWritten = ctx->events.writeOnSessionEstablishedEvent(buf, client, session, rsp);
	ctx->done_event_creating(sizeWritten);

	return 0;
}

int on_session_event_callback_client(struct xio_session *session,
		struct xio_session_event_data *event_data, void *cb_prv_data)
{
	LOG_DBG("got on_session_event_callback. event=%d, cb_prv_data=%p, session=%p, conn=%p",
			event_data->event, cb_prv_data, session, event_data->conn);
	Client *client = (Client*)cb_prv_data;

	Context *ctx = client->ctxForSessionEvent(event_data, session);
	if (ctx) {
		/*in case it is a client, the java object is represented by cb_prv_data */
		char* buf = ctx->get_buffer();
		int sizeWritten = ctx->events.writeOnSessionErrorEvent(buf, client, event_data);
		ctx->done_event_creating(sizeWritten);
	}
	return 0;
}
