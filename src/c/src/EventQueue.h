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


#ifndef EventQueue__H___
#define EventQueue__H___

#include <stdlib.h>
#include <stdio.h>

#define EVENTQUEUE_HEADROOM_BUFFER (1024)

class EventQueue {
public:
	EventQueue(size_t size);
	~EventQueue();
	char* get_buffer() { return buffer; };
	char* get_buffer_offset();
	void reset();
	void increase_offset(int increase);
	inline int get_count() { return count; }

private:
	const size_t size;
	char* buffer;
	int offset;
	int count;
};

#endif // ! EventQueue__H___
