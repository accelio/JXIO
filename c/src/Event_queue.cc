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
#include <string.h>
#include <map>


#include "Event_queue.h"

Event_queue::Event_queue(int size)
{
	this->offset = 0;
	this->size = size;

	error_creating = false;
	this->buf = (char*)malloc(size * sizeof(char));
	if (this->buf== NULL){
		log(lsERROR, "Could not allocate memory for Event Queue buffer\n");
		error_creating = true;
		return;
	}

}


Event_queue::~Event_queue()
{
	if (this->buf!= NULL){
		free(this->buf);
	}
}

void Event_queue::reset()
{
	this->offset = 0;
}

char* Event_queue::get_buffer()
{
	return this->buf + this->offset;
}

void Event_queue::increase_offset(int increase)
{
	this->offset += increase;
}









