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

#include "MsgPools.h"

MsgPools::MsgPools()
{
	this->in_size = 0;
	this->out_size = 0;
	this->first_time = true;
	this->ctx = NULL;
}

void MsgPools::setCtx(Context* ctx)
{
	this->ctx = ctx;
}

MsgPools::~MsgPools()
{
}

bool MsgPools::add_msg_pool(MsgPool* pool)
{
	if (this->first_time){
		this->in_size = pool->get_in_size();
		this->out_size = pool->get_out_size();
	}else{
		if (this->in_size != pool->get_in_size() || this->out_size != pool->get_out_size()) {
			LOG_FATAL("New pool is not of the same size!!! should be in=%d, out=%d", this->in_size, this->out_size);
			exit(1);
		}
	}
	msg_pool_list.push_front(pool);
	this->first_time = false;
	return true;
}

Msg * MsgPools::get_msg_from_pool(int in_size, int out_size)
{
	//currently all msgPools have the same message sizes
	while (true){
		list_pools::iterator it = msg_pool_list.begin();
		while (it != msg_pool_list.end()){
			MsgPool* pool = *it;
			if (!pool->is_empty()){
				return pool->get_msg_from_pool();
			}
			it++;
		}
		LOG_DBG("there are no more buffers in MsgPools. calling the user to allocate pool with in_size=%d, out_size=%d", in_size, out_size);
		Bridge_invoke_requestForBoundMsgPool_callback(this->ctx, in_size, out_size);
	}
}
