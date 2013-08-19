
#include <string.h>
#include <map>


#include "EventQueue.h"



EventQueue::EventQueue(int size)
{
	this->buf = (char*)malloc(size * sizeof(char));
	if (this->buf== NULL){
		fprintf(stderr, "Error, Could not allocate memory for Event Queue buffer");
		return;
	}

	this->offset = 0;
	this->size = size;

}


EventQueue::~EventQueue(){
	free(this->buf);
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









