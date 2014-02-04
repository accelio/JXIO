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

#include <sys/timerfd.h>

#include "bullseye.h"
#include "Utils.h"
#include "Msg.h"
#include "MsgPool.h"

#define MODULE_NAME		"Msg"
#define MSG_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define MSG_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)
#define MSG_LOG_TRACE(log_fmt, log_args...)  LOG_BY_MODULE(lsTRACE, log_fmt, ##log_args)


Msg::Msg(char* buf, struct xio_mr* xio_mr, int in_buf_size, int out_buf_size, MsgPool* pool)
{
	this->buf = buf;
	this->xio_mr = xio_mr;
	this->in_buf_size = in_buf_size;
	this->out_buf_size = out_buf_size;
	this->xio_msg = (struct xio_msg *) calloc(1, sizeof(struct xio_msg));
	this->pool = pool;
	this->buf_out = this->buf + in_buf_size;
	this->set_xio_msg_client_fields();
}

Msg::~Msg()
{
	free(this->xio_msg);
}

struct xio_msg* Msg::get_xio_msg()
{
	return xio_msg;
}

void Msg::set_xio_msg_client_fields()
{
	this->xio_msg->user_context = this; //we will be able to recieve it back on responce from server

	this->xio_msg->out.header.iov_base = NULL;
	this->xio_msg->out.header.iov_len = 0;
	if (this->out_buf_size == 0) {
		this->xio_msg->out.data_iovlen = 0;
	} else {
		this->xio_msg->out.data_iovlen = 1;
		this->xio_msg->out.data_iov[0].iov_base = this->buf_out;
		this->xio_msg->out.data_iov[0].iov_len = this->out_buf_size;
		this->xio_msg->out.data_iov[0].mr = this->xio_mr;
	}

	this->xio_msg->in.header.iov_base = NULL;
	this->xio_msg->in.header.iov_len = 0;
	if (this->in_buf_size == 0) {
		this->xio_msg->in.data_iovlen = 0;
	} else {
		this->xio_msg->in.data_iovlen = 1;
		this->xio_msg->in.data_iov[0].iov_base = this->buf;
		this->xio_msg->in.data_iov[0].iov_len = this->in_buf_size;
		this->xio_msg->in.data_iov[0].mr = this->xio_mr;
	}
}

void Msg::set_xio_msg_server_fields()
{
	this->xio_msg->out.data_iovlen = 1;
	this->xio_msg->out.data_iov[0].iov_base = this->buf_out;
	this->xio_msg->out.data_iov[0].iov_len = this->out_buf_size;
	this->xio_msg->out.data_iov[0].mr = this->xio_mr;

	this->xio_msg->in.header.iov_base = NULL;
	this->xio_msg->in.data_iovlen = 1;
	this->xio_msg->in.data_iov[0].iov_base = this->buf;
	this->xio_msg->in.data_iov[0].iov_len = this->in_buf_size;
	this->xio_msg->in.data_iov[0].mr = this->xio_mr;
}

void Msg::set_xio_msg_fields_for_assign(struct xio_msg *msg)
{
	msg->in.data_iov[0].iov_base = this->buf;
	msg->in.data_iov[0].iov_len = this->in_buf_size;
	msg->in.data_iov[0].mr = this->xio_mr;
	msg->user_context = this;
	this->set_xio_msg_req(msg);
}

void Msg::set_xio_msg_req(struct xio_msg *msg)
{
	this->xio_msg->request = msg;
//	log (lsDEBUG, "inside set_req_xio_msg msg is %p req is %p\n",this->xio_msg,  this->xio_msg->request);
}

void Msg::set_xio_msg_out_size(const int size)
{
	if (size > 0) {
		this->xio_msg->out.data_iovlen = 1;
		this->xio_msg->out.data_iov[0].iov_len = size;
	}
	else {
		this->xio_msg->out.data_iovlen = 0;
	}
}

void Msg::reset_xio_msg_in_size()
{
	this->xio_msg->in.data_iov[0].iov_len = this->in_buf_size;
}

void Msg::release_to_pool()
{
	this->pool->add_msg_to_pool(this);
}

bool Msg::send_reply(const int size)
{
	MSG_LOG_TRACE("sending %d bytes, xio_msg is %p", size, this->get_xio_msg());
	//TODO : make sure that this function is not called in the fast path
	this->set_xio_msg_server_fields();
	set_xio_msg_out_size(size);
	//checking that client in size is big enough
	int client_in = this->get_xio_msg()->request->out.data_iov[0].iov_len;
	int server_out = this->get_xio_msg()->out.data_iov[0].iov_len;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (client_in < server_out) {
		MSG_LOG_ERR("attempting to write when client IN size=%d, server sends %d", client_in, server_out);
		return false;
	}

	if (xio_send_response(this->get_xio_msg())) {
		MSG_LOG_DBG("Got error from sending xio_msg: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	return true;
}

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
void Msg::dump(struct xio_msg *xio_msg)
{
	MSG_LOG_DBG("*********************************************");
	MSG_LOG_DBG("type:0x%x", xio_msg->type);
	MSG_LOG_DBG("status:%d", xio_msg->status);
	if (xio_msg->type == XIO_MSG_TYPE_REQ)
		MSG_LOG_DBG("serial number:%ld", xio_msg->sn);
	else if (xio_msg->type == XIO_MSG_TYPE_RSP)
		MSG_LOG_DBG("response:%p, serial number:%ld", xio_msg->request, ((xio_msg->request) ? xio_msg->request->sn : -1));

	MSG_LOG_DBG("in header: length:%d, address:%p", xio_msg->in.header.iov_len, xio_msg->in.header.iov_base);
	MSG_LOG_DBG("in data size:%d \n", xio_msg->in.data_iovlen);
	for (int i = 0; i < xio_msg->in.data_iovlen; i++)
		MSG_LOG_DBG("in data[%d]: length:%d, address:%p, mr:%p", i, xio_msg->in.data_iov[i].iov_len, xio_msg->in.data_iov[i].iov_base, xio_msg->in.data_iov[i].mr);

	MSG_LOG_DBG("out header: length:%d, address:%p", xio_msg->out.header.iov_len, xio_msg->out.header.iov_base);
	MSG_LOG_DBG("out data size:%d", xio_msg->out.data_iovlen);
	for (int i = 0; i < xio_msg->out.data_iovlen; i++)
		MSG_LOG_DBG("out data[%d]: length:%d, address:%p, mr:%p", i, xio_msg->out.data_iov[i].iov_len, xio_msg->out.data_iov[i].iov_base, xio_msg->out.data_iov[i].mr);
	MSG_LOG_DBG("*********************************************");
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif
