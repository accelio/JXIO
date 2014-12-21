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


Msg::Msg(char* buf, struct xio_mr* xio_mr, int in_buf_size, int out_buf_size, MsgPool* pool) :
	in_buf_size(in_buf_size), out_buf_size(out_buf_size), xio_mr(xio_mr),
	pool(pool), buf(buf), buf_out(buf + in_buf_size)
{
	memset(&this->xio_msg, 0, sizeof(this->xio_msg));
	memset(&this->xio_msg_mirror, 0, sizeof(this->xio_msg_mirror));
	this->set_xio_msg_client_fields();
	this->set_xio_msg_mirror_fields();
	this->assign_called = false;
}

Msg::~Msg()
{
}

void Msg::set_xio_msg_client_fields()
{
	//needed to retrieve back the Msg when response from server is received
	this->xio_msg.user_context = this;

	this->xio_msg.out.header.iov_base = NULL;
	this->xio_msg.out.header.iov_len = 0;
	this->xio_msg.in.sgl_type           = XIO_SGL_TYPE_IOV;
	this->xio_msg.in.data_iov.max_nents = XIO_IOVLEN;
	if (get_out_size() == 0) {
		this->xio_msg.out.data_iov.nents = 0;
	} else {
		this->xio_msg.out.data_iov.nents = 1;
		this->xio_msg.out.data_iov.sglist[0].iov_base = this->buf_out;
		this->xio_msg.out.data_iov.sglist[0].iov_len = get_out_size();
		this->xio_msg.out.data_iov.sglist[0].mr = this->xio_mr;
	}

	this->xio_msg.in.header.iov_base = NULL;
	this->xio_msg.in.header.iov_len = 0;
	this->xio_msg.out.sgl_type           = XIO_SGL_TYPE_IOV;
	this->xio_msg.out.data_iov.max_nents = XIO_IOVLEN;
	if (get_in_size() == 0) {
		this->xio_msg.in.data_iov.nents = 0;
	} else {
		xio_iovec_ex *sglist = vmsg_sglist(&this->xio_msg.in);
		this->xio_msg.in.data_iov.nents = 1;
		sglist[0].iov_base = this->buf;
		sglist[0].iov_len = get_in_size();
		sglist[0].mr = this->xio_mr;
	}
}

void Msg::set_xio_msg_mirror_fields()
{
	//needed to retrieve back the Msg when response from server is received
	this->xio_msg_mirror.user_context = this;
	this->xio_msg_mirror.out = this->xio_msg.in;
	this->xio_msg_mirror.in = this->xio_msg.out;
}

void Msg::set_xio_msg_server_fields()
{
	xio_iovec_ex *sglist = vmsg_sglist(&this->xio_msg.out);

	this->xio_msg.out.data_iov.nents = 1;
	sglist[0].iov_base = this->buf_out;
	sglist[0].iov_len = get_out_size();
	sglist[0].mr = this->xio_mr;

	sglist = vmsg_sglist(&this->xio_msg.in);
	this->xio_msg.in.header.iov_base = NULL;
	this->xio_msg.in.data_iov.nents = 1;
	sglist[0].iov_base = this->buf;
	sglist[0].iov_len = get_in_size();
	sglist[0].mr = this->xio_mr;
}

void Msg::set_xio_msg_fields_for_assign(struct xio_msg *msg)
{
	xio_iovec_ex *sglist = vmsg_sglist(&msg->in); 

	sglist[0].iov_base = this->buf;
	sglist[0].iov_len = get_in_size();
	sglist[0].mr = this->xio_mr;
	msg->user_context = this;
	this->set_xio_msg_req(msg);
	this->assign_called = true;
}

void Msg::set_xio_msg_req(struct xio_msg *msg)
{
	this->xio_msg.request = msg;
	// MSG_LOG_DBG("inside set_req_xio_msg msg is %p req is %p",this->xio_msg, this->xio_msg->request);
}

void Msg::set_xio_msg_out_size(struct xio_msg *xio_msg, const int out_size)
{
	if (out_size > 0) {
		xio_msg->out.data_iov.nents = 1;
		xio_msg->out.data_iov.sglist[0].iov_len = out_size;
	} else {
		xio_msg->out.data_iov.nents = 0;
	}
}

void Msg::set_xio_msg_in_size(struct xio_msg *xio_msg, const int in_size)
{
	if (in_size > 0) {
		xio_msg->in.data_iov.nents = 1;
		xio_msg->in.data_iov.sglist[0].iov_len = in_size;
	} else {
		xio_msg->in.data_iov.nents = 0;
	}
}

void Msg::release_to_pool()
{
	this->pool->add_msg_to_pool(this);
	this->assign_called = false;
	if (this->get_xio_msg()->request)
		this->get_xio_msg()->request->user_context = NULL;
}

int Msg::send_response(const int out_size)
{
	MSG_LOG_TRACE("sending %d bytes, xio_msg is %p", out_size, this->get_xio_msg());
	//TODO : make sure that this function is not called in the fast path
	this->set_xio_msg_server_fields();
	set_xio_msg_out_size(this->get_xio_msg(), out_size);

	int ret = xio_send_response(this->get_xio_msg());
	BULLSEYE_EXCLUDE_BLOCK_START
	if (ret)
		MSG_LOG_DBG("Got error from sending xio_msg: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
	BULLSEYE_EXCLUDE_BLOCK_END
	this->assign_called = false;
	return ret;
}

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
void Msg::dump(struct xio_msg *xio_msg)
{
	MSG_LOG_TRACE("*********************************************");
	MSG_LOG_TRACE("type:0x%x", xio_msg->type);
	if (xio_msg->type == XIO_MSG_TYPE_REQ)
		MSG_LOG_TRACE("serial number:%ld", xio_msg->sn);
	else if (xio_msg->type == XIO_MSG_TYPE_RSP)
		MSG_LOG_TRACE("response:%p, serial number:%ld", xio_msg->request, ((xio_msg->request) ? xio_msg->request->sn : -1));

	MSG_LOG_TRACE("in header: length:%d, address:%p", xio_msg->in.header.iov_len, xio_msg->in.header.iov_base);
	MSG_LOG_TRACE("in data size:%d", xio_msg->in.data_iov.nents);
	for (int i = 0; i < xio_msg->in.data_iov.nents; i++)
		MSG_LOG_TRACE("in data[%d]: length:%d, address:%p, mr:%p", i, xio_msg->in.data_iov.sglist[i].iov_len, xio_msg->in.data_iov.sglist[i].iov_base, xio_msg->in.data_iov.sglist[i].mr);

	MSG_LOG_TRACE("out header: length:%d, address:%p", xio_msg->out.header.iov_len, xio_msg->out.header.iov_base);
	MSG_LOG_TRACE("out data size:%d", xio_msg->out.data_iov.nents);
	for (int i = 0; i < xio_msg->out.data_iov.nents; i++)
		MSG_LOG_TRACE("out data[%d]: length:%d, address:%p, mr:%p", i, xio_msg->out.data_iov.sglist[i].iov_len, xio_msg->out.data_iov.sglist[i].iov_base, xio_msg->out.data_iov.sglist[i].mr);
	MSG_LOG_TRACE("*********************************************");
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif
