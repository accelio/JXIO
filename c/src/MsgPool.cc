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

#include "MsgPool.h"

//TODO: make sure that in and out size are aligned to 64!!!!
MsgPool::MsgPool(int msg_num, int in_size, int out_size)
{
	error_creating = false;
	this->in_size = in_size;
	this->out_size = out_size;
	this->msg_num = msg_num;
	this->msg_list = NULL;
	this->msg_ptrs = NULL;
	this->xio_mr = NULL;

	this->buf_size = msg_num * (in_size + out_size);

	this->x_buf = xio_alloc(buf_size);
	if (this->x_buf == NULL) {
		log(lsWARN, "there was an error while allocating&registering memory via huge pages. \n");
		log(lsWARN, "You should work with Mellanox Ofed 2.0\n");
		log(lsWARN, "attempting to allocate&registering memory. THIS COULD HURT PERFORMANCE!!!!!\n");
		this->buf = (char*) malloc(this->buf_size);
		if (this->buf == NULL) {
			log(lsERROR, "allocating memory of size %ld failed. aborting\n", this->buf_size);
			goto mark_error;
		}
		this->xio_mr = xio_reg_mr(this->buf, this->buf_size);
		if (this->xio_mr == NULL) {
			free(this->buf);
			log(lsERROR, "registering memory failed. aborting\n");
			goto mark_error;
		}
	}
	else {
		this->buf = (char*) x_buf->addr;
		this->xio_mr = x_buf->mr;
	}

	msg_ptrs = (Msg**) malloc(sizeof(Msg*) * msg_num);
	if (msg_ptrs == NULL) {
		goto cleanup_buffer;
	}

	msg_list = new std::list<Msg*>;
	if (msg_list == NULL) {
		goto cleanup_array;
	}

	for (int i = 0; i < msg_num; i++) {
		Msg *m = new Msg((char*) buf + i * (in_size + out_size), xio_mr, in_size, out_size, this);
		if (m == NULL) {
			goto cleanup_list;
		}
		msg_list->push_front(m);
		msg_ptrs[i] = m;
	}
	log(lsDEBUG, "finished allocating msg pool. this=%p, msg_num=%d,  in_size=%d, out_size=%d\n", this, msg_num, in_size, out_size);

	return;

cleanup_list:
	while (!msg_list->empty()) {
		Msg * msg = msg_list->front();
		msg_list->pop_front();
		delete msg;
	}
	delete (msg_list);
cleanup_array:
	free(msg_ptrs);
cleanup_buffer:
	if (this->x_buf) { //memory was allocated using xio_alloc
		if (xio_free(&this->x_buf)) {
			log(lsERROR, "Error xio_free failed\n");
		}
	}
	else { //memory was allocated using malloc and xio_reg_mr
		if (xio_dereg_mr(&this->xio_mr)) {
			log(lsERROR, "Error xio_dereg_mr failed\n");
		}
		free(this->buf);
	}
mark_error:
	error_creating = true;
}

MsgPool::~MsgPool()
{
	if (error_creating) {
		return;
	}

	while (!msg_list->empty()) {
		Msg * msg = msg_list->front();
		msg_list->pop_front();
		delete msg;
	}

	delete (msg_list);

	if (this->x_buf) { //memory was allocated using xio_alloc
		if (xio_free(&this->x_buf)) {
			log(lsERROR, "Error xio_free failed\n");
		}
	}
	else { //memory was allocated using malloc and xio_reg_mr
		if (xio_dereg_mr(&this->xio_mr)) {
			log(lsERROR, "Error xio_dereg_mr failed\n");
		}
		free(this->buf);
	}

	free (msg_ptrs);
}

Msg* MsgPool::get_msg_from_pool()
{
	if (msg_list->empty()) {
		log(lsFATAL, "msg list is empty at MsgPool=%p. EXITING\n", this);
		exit(1);
	}
	Msg * msg = msg_list->front();
	msg_list->pop_front();

	return msg;
}

void MsgPool::add_msg_to_pool(Msg* msg)
{
	msg_list->push_front(msg);
}

