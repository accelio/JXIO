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


#ifndef Event_queue__H___
#define Event_queue__H___

#include <stdlib.h>
#include <stdio.h>
#include "Utils.h"

//TODO:: check for overflow of the buffer
class Event_queue{


public:
	Event_queue(int size);
	~Event_queue();
	char* get_buffer();
	void reset();
	void increase_offset(int increase);
	int get_offset(){return offset;}

	bool error_creating;

private:
	int offset;
	char * buf;
	int size;


};




#endif // ! Event_queue__H___
