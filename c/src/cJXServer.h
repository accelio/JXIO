#ifndef cJXServer__H___
#define cJXServer__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "CallbackFunctions.h"
//#include "cJXCtx.h"


class cJXCtx;

class cJXServer:public Contexable{
public:
	//to move some to private?
	cJXServer(const char	*url, long ptrCtx);
	~cJXServer();

	struct xio_server	*server;

	bool errorCreating;
	int port; //indicates the actual port on which the server listens

//	cJXCtx* ctx;

};




#endif // ! cJXServer__H___
