#ifndef cJXSession__H___
#define cJXSession__H___

#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "CallbackFunctions.h"



class cJXSession{
public:
	//to move some to private?
	cJXSession(const char	*hostname, int port, long ptrCtx);
	~cJXSession();
	bool closeConnection();

	struct xio_session	*session;
	struct xio_connection * con;

	cJXCtx* ctx;




};




#endif // ! cJXSession__H___
