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
#ifndef MsgPool__H___
#define MsgPool__H___


#include <stdlib.h>
#include <stdio.h>
#include <list>

#include <libxio.h>
#include "Msg.h"
#include "Bridge.h"


class MsgPool {
public:
	MsgPool (int msg_num, int in_size, int out_size);
	~MsgPool();

	Msg* get_msg_from_pool();
	void add_msg_to_pool(Msg* msg);
	bool is_empty()    { return msg_list.empty(); }
	int get_in_size()  { return in_size; }
	int get_out_size() { return out_size; }

	long            buf_size;
	char*           buf;
	Msg**           msg_ptrs;

private:
	int             msg_num;
	int             in_size;
	int             out_size;
	struct xio_reg_mem reg_mem;
	std::list<Msg*> msg_list;
};

#endif // ! MsgPool__H___
