#ifndef cJXSession__H___
#define cJXSession__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "CallbackFunctions.h"
#include "cJXCtx.h"
#include "Contexable.h"

class cJXCtx;

class cJXSession:public Contexable{
public:
	//to move some to private?
	cJXSession(const char	*url, long ptrCtx);
	~cJXSession();
	bool closeConnection();
	int closeSession();

	struct xio_session	*session;
	struct xio_connection * con;

	bool errorCreating;
//	cJXCtx* ctx;

};




#endif // ! cJXSession__H___
