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

	this->buf_size = msg_num*(in_size+out_size);

	this->x_buf = xio_alloc(buf_size);
	if (x_buf == NULL){
		goto mark_error;
	}
	this->buf = x_buf->addr;
	this->xio_mr = x_buf->mr;

	msg_ptrs = (Msg**)malloc(sizeof(Msg*)*msg_num);
	if (msg_ptrs == NULL){
		goto cleanup_buffer;
	}

	msg_list = new std::list<Msg*>;
	if (msg_list == NULL){
		goto cleanup_array;
	}

	for (int i=0; i<msg_num; i++){
		Msg *m = new Msg (buf+i*(in_size+out_size), xio_mr,  in_size, out_size, this);
		if (m == NULL){
			goto cleanup_list;
		}
		msg_list->push_front (m);
		msg_ptrs[i] = m;
	}
	return;

cleanup_list:
	while (!msg_list->empty())
		{
			Msg * msg = msg_list->front();
			msg_list->pop_front();
			delete msg;
		}
	delete (msg_list);
cleanup_array:
	free (msg_ptrs);
cleanup_buffer:
	if (xio_free(&this->x_buf)){
		log (lsERROR, "Error xio_dereg_mr failed\n");
	}
	free (this->buf);
mark_error:
	error_creating = true;
}

MsgPool::~MsgPool()
{
	if (error_creating) {
		return;
	}

	while (!msg_list->empty())
	{
	    Msg * msg = msg_list->front();
	    msg_list->pop_front();
	    delete msg;
	}

	delete (msg_list);
	if (xio_free(&this->x_buf)){
		log (lsERROR, "Error xio_free failed\n");
	}

}

Msg * MsgPool::get_msg_from_pool ()
{
	if (msg_list->empty()){
		log (lsERROR, "msg list is empty\n");
		exit (1);
	}
	Msg * msg = msg_list->front();
	msg_list->pop_front();

	return msg;
}

void MsgPool::add_msg_to_pool(Msg * msg)
{
	msg_list->push_front (msg);
}

