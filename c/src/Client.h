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
#ifndef Client__H___
#define Client__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "CallbackFunctions.h"
#include "Context.h"
#include "Contexable.h"


class Client:public Contexable{
public:
	//to move some to private?
	Client(const char	*url, long ptrCtx);
	~Client();
	bool close_connection();

	struct xio_session	*session;
	struct xio_connection * con;

	bool error_creating;
	bool onSessionEvent(int eventType);
//	cJXCtx* ctx;

};




#endif // ! Client__H___
