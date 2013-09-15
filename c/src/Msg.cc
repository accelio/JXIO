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

#include "Msg.h"

Msg::Msg(void * buf, struct xio_mr 	*xio_mr, int in_buf_size, int out_buf_size)
{
	this->buf = buf;
	this->xio_mr = xio_mr;
	this->in_buf_size = in_buf_size;
	this->out_buf_size = out_buf_size;
}

Msg::~Msg()
{

}

void Msg::set_xio_msg()
{
	this->xio_msg->user_context = this; //we will be able to recieve it back on responce from server

	this->xio_msg->out.header.iov_base = NULL;
	this->xio_msg->out.header.iov_len = 0;
	this->xio_msg->out.data_iovlen = 1;
	this->xio_msg->out.data_iov[0].iov_base = this->buf + in_buf_size;
	this->xio_msg->out.data_iov[0].iov_len = this->out_buf_size;
	this->xio_msg->out.data_iov[0].mr = this->xio_mr;

	this->xio_msg->in.header.iov_base = NULL;
	this->xio_msg->in.data_iovlen = 1;
	this->xio_msg->in.data_iov[0].iov_base = this->buf;
	this->xio_msg->in.data_iov[0].iov_len = this->in_buf_size;
	this->xio_msg->in.data_iov[0].mr = this->xio_mr;
}
