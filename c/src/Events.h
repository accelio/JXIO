

#ifndef Events__H___
#define Events__H___

#include <arpa/inet.h>
#include <stdio.h>
#include <libxio.h>


struct eventNewSession{
	intptr_t ptrSession;
	int32_t lenUri;
	char * uri;
	int32_t ipLen;
	char *ip;
};

struct eventSessionEstablished{
};

struct eventSessionError{

	int32_t error_type;
	int32_t error_reason;
};

struct eventMsgComplete{
};

struct eventMsgError{
};

struct eventMsgReceived{
};

struct eventStruct{
	int32_t type;
	union{
		struct eventNewSession newSession;
		struct eventSessionEstablished sessionEstablished;
		struct eventSessionError sessionError;
		struct eventMsgComplete msgComplete;
		struct eventMsgError msgError;
		struct eventMsgReceived msgReceived;
	}eventSpecific;
};


class Events{
public:
	int size;
	struct eventStruct event;


	Events();

	int writeOnSessionErrorEvent(char *buf, struct xio_session *session,
			struct xio_session_event_data *event_data,
			void *cb_prv_data);
	int writeOnSessionEstablishedEvent (char *buf, struct xio_session *session,
			struct xio_new_session_rsp *rsp,
			void *cb_prv_data);
	int writeOnNewSessionEvent(char *buf, struct xio_session *session,
			struct xio_new_session_req *req,
			void *cb_prv_data);
	int writeOnMsgSendCompleteEvent(char *buf, struct xio_session *session,
			struct xio_msg *msg,
			void *cb_prv_data);
	int writeOnMsgErrorEvent(char *buf, struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context);
	int writeOnMsgReceivedEvent(char *buf, struct xio_session *session,
			struct xio_msg *msg,
			int more_in_batch,
			void *cb_prv_data);

};




#endif // ! Events__H___
