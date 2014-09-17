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

#include <stdexcept>

#include "bullseye.h"
#include "Utils.h"
#include "EventQueue.h"

EventQueue::EventQueue(size_t size) : size(size)
{
	this->buffer = new char[size]; // might throw a std::bad_alloc() exception
	reset();
}

EventQueue::~EventQueue()
{
	reset();
	delete this->buffer;
}

void EventQueue::reset()
{
	//update offset to 0: for indication if this is the first callback called
	this->offset = 0;
	this->count = 0;
}

char* EventQueue::get_buffer_offset()
{
	if (this->offset + EVENTQUEUE_HEADROOM_BUFFER > this->size) {
		return NULL;
	}
	return this->buffer + this->offset;
};

void EventQueue::increase_offset(int increase)
{
	this->offset += increase;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (this->offset > this->size) {
		char fatalErrorStr[256];
		sprintf(fatalErrorStr, "EventQueue buffer overflow ERROR (there are already %d events in queue). Aborting!!!", get_count());
		LOG_FATAL("%s", fatalErrorStr);
		throw std::overflow_error(fatalErrorStr);
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	this->count++;
}
