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
#ifndef Msg__H___
#define Msg__H___

#include <stdlib.h>
#include <stdio.h>
#include <libxio.h>

#include "Utils.h"

class MsgPool;

class Msg {
public:
	Msg(char * buf, struct xio_mr *xio_mr, int in_buf_size, int out_buf_size, MsgPool* pool);
	~Msg();
	void set_xio_msg_client_fields(); //this method is used by client side
	void set_xio_msg_req(struct xio_msg *msg); //this method is used by server side
	void set_xio_msg_server_fields();
	void set_xio_msg_fields_for_assign(struct xio_msg *msg); //used when assign_buffer callback is called
	void set_xio_msg_out_size(const int size);
	void reset_xio_msg_in_size();
	void* get_buf() { return buf; }
	struct xio_msg* get_xio_msg();
	void release_to_pool();
	bool send_reply(const int size);
	void dump(struct xio_msg *m); //func for debugging only

private:
	char * buf;
	char * buf_out;
	int in_size;
	int out_size;
	struct xio_mr *xio_mr;
	struct xio_msg *xio_msg;
	struct xio_msg *req;
	int serial_number;
	int in_buf_size;
	int out_buf_size;
	MsgPool* pool;
};

#endif // ! Msg__H___
