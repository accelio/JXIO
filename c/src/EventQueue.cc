
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









