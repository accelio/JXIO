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


#include "EventQueue.h"



EventQueue::EventQueue(int size)
{
	errorCreating = false;
	this->buf = (char*)malloc(size * sizeof(char));
	if (this->buf== NULL){
		fprintf(stderr, "Error, Could not allocate memory for Event Queue buffer");
		errorCreating = true;
		return;
	}

	this->offset = 0;
	this->size = size;

}


EventQueue::~EventQueue(){
	if (this->buf!= NULL){
		free(this->buf);
	}
}

void EventQueue::reset(){
	this->offset = 0;
}

char* EventQueue::getBuffer(){
	return this->buf + this->offset;
}

void EventQueue::increaseOffset(int increase){
	this->offset += increase;
}









