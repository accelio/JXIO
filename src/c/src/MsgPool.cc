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
#include "Utils.h"
#include "MsgPool.h"

//TODO: make sure that in and out size are aligned to 64!!!!

#define MODULE_NAME		"MsgPool"
#define MSGPOOL_LOG_ERR(log_fmt, log_args...)  LOG_BY_MODULE(lsERROR, log_fmt, ##log_args)
#define MSGPOOL_LOG_WARN(log_fmt, log_args...) LOG_BY_MODULE(lsWARN, log_fmt, ##log_args)
#define MSGPOOL_LOG_DBG(log_fmt, log_args...)  LOG_BY_MODULE(lsDEBUG, log_fmt, ##log_args)


MsgPool::MsgPool(int msg_num, int in_size, int out_size)
{
	Msg* msg = NULL;
	this->in_size = in_size;
	this->out_size = out_size;
	this->msg_num = msg_num;
	this->msg_ptrs = NULL;
	this->buf_size = (long)msg_num * (in_size + out_size);

	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_mem_alloc(buf_size, &this->reg_mem)){
		MSGPOOL_LOG_ERR("allocating & registering memory failed with xio_mem_alloc(buf=%p, buf_size=%d) (errno=%d '%s')", this->reg_mem, this->buf_size, xio_errno(), xio_strerror(xio_errno()));
		throw std::bad_alloc();
	}
	this->buf = (char*) this->reg_mem.addr;

	BULLSEYE_EXCLUDE_BLOCK_END

	msg_ptrs = new Msg*[msg_num];

	for (int i = 0; i < msg_num; i++) {
		msg = new Msg((char*) buf + i * (in_size + out_size), this->reg_mem.mr, in_size, out_size, this);
		add_msg_to_pool(msg);
		msg_ptrs[i] = msg;
	}

	MSGPOOL_LOG_DBG("CTOR done. allocated msg pool: num_msgs=%d, in_size=%d, out_size=%d", msg_num, in_size, out_size);
	return;
}

MsgPool::~MsgPool()
{
	Msg* msg = NULL;
	while ((msg = get_msg_from_pool()) != NULL) {
		delete msg;
	}

	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_mem_free(&this->reg_mem)) {
		MSGPOOL_LOG_DBG("Error xio_mem_free failed: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	delete[] msg_ptrs;
	MSGPOOL_LOG_DBG("DTOR done");
}

Msg* MsgPool::get_msg_from_pool()
{
	if (msg_list.empty()) {
		return NULL;
	}
	Msg* msg = msg_list.front();
	msg_list.pop_front();
	return msg;
}

void MsgPool::add_msg_to_pool(Msg* msg)
{
	msg_list.push_front(msg);
}
