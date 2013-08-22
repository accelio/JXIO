

#ifndef EventQueue__H___
#define EventQueue__H___

#include <stdlib.h>
#include <stdio.h>


//TODO:: check for overflow of the buffer
class EventQueue{


public:
	EventQueue(int size);
	~EventQueue();
	char* getBuffer();
	void reset();
	void increaseOffset(int increase);

	bool errorCreating;

private:
	int offset;
	char * buf;
	int size;


};




#endif // ! EventQueue__H___
