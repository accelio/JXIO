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
		error_creating = true;
	}
	this->buf = x_buf->addr;
	this->xio_mr = x_buf->mr;

	msg_list = new std::list<Msg*>;
	//Katya: to check if fails!!!!!
	for (int i=0; i<msg_num; i++){
		Msg *m = new Msg (buf+i*(in_size+out_size), xio_mr,  in_size, out_size);
		msg_list->push_front (m);
	}
}

MsgPool::~MsgPool()
{
	if (error_creating) {
		return;
	}
	if (xio_free(&this->x_buf)){
		log (lsERROR, "Error xio_free failed\n");
	}

	while (!msg_list->empty())
	{
	    Msg * msg = msg_list->front();
	    msg_list->pop_front();
	    delete msg;
	}

	delete (msg_list);
}

Msg * MsgPool::getMsgFromPool ()
{
	if (msg_list->empty()){
		log (lsERROR, "msg list is empty\n");
		exit (1);
	}
	Msg * msg = msg_list->front();
	msg_list->pop_front();

	return msg;
}

void MsgPool::addMsgToPool(Msg * msg)
{
	msg_list->push_front (msg);
}

