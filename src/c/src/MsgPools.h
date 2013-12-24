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
#ifndef MsgPools__H___
#define MsgPools__H___


#include <stdlib.h>
#include <stdio.h>
#include <list>

#include "Utils.h"
#include "MsgPool.h"

class Context;

typedef std::list<MsgPool*> list_pools;

class MsgPools {
public:
	MsgPools ();
	~MsgPools();
	//returns true if msg was allocated successfully and false otherwise
	bool add_msg_pool(MsgPool *p);
	Msg * get_msg_from_pool(int in_size, int out_size);
	list_pools msg_pool_list;
	void setCtx(Context* ctx);

private:
	int in_size;
	int out_size;
	bool first_time;
	Context* ctx;

};

#endif // ! MsgPools__H___
