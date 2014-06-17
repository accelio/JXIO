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

#include <map>
#include <stdexcept>

#include "bullseye.h"
#include "EventQueue.h"

EventQueue::EventQueue(int size)
{
	this->offset = 0;
	this->size = size;

	error_creating = false;
	this->buf = (char*)malloc(size * sizeof(char));
	BULLSEYE_EXCLUDE_BLOCK_START
	if (this->buf== NULL) {
		LOG_DBG("ERROR, could not allocate memory for Event Queue buffer");
		error_creating = true;
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
}

EventQueue::~EventQueue()
{
	if (this->buf != NULL) {
		free(this->buf);
	}
}

void EventQueue::reset()
{
	this->offset = 0;
}

char* EventQueue::get_buffer()
{
	return this->buf + this->offset;
}

void EventQueue::increase_offset(int increase)
{
	this->offset += increase;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (this->offset > this->size) {
		const char* fatalErrorStr = "There has been overflow in EventQueue buffer. Aborting!!!";
		LOG_FATAL("%s", fatalErrorStr);
		throw std::overflow_error(fatalErrorStr);
	}
	BULLSEYE_EXCLUDE_BLOCK_END
}
