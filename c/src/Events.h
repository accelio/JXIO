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


#ifndef Events__H___
#define Events__H___

#include <arpa/inet.h>
#include <stdio.h>
#include <libxio.h>
#include "Utils.h"


class Contexable;

struct  eventNewSession{
	int64_t ptrSession;
	int32_t lenUri;
	char  uri [0]; //to indicate that string uri is written to buffer
	int32_t ipLen;
	char ip [0]; //to  indicate that ip string is written to buffer
}__attribute__ ((packed));

struct __attribute__ ((packed)) eventSessionEstablished{
};

struct __attribute__ ((packed)) eventSessionError{

	int32_t error_type;
	int32_t error_reason;
};

struct __attribute__ ((packed)) eventMsgComplete{
};

struct __attribute__ ((packed)) eventMsgError{
};

struct __attribute__ ((packed)) eventMsgReceived {
};

struct  eventStruct{
	int32_t type;
	int64_t ptr;//this will indicate on which session the event arrived
	union{
		struct eventNewSession newSession;
		struct eventSessionEstablished sessionEstablished;
		struct eventSessionError sessionError;
		struct eventMsgComplete msgComplete;
		struct eventMsgError msgError;
		struct eventMsgReceived msgReceived;
	} eventSpecific;
}__attribute__ ((packed));

/*
struct __attribute__ ((packed)) vma_datagram_t {
        void*           datagram_id;            // datagram identifier
        size_t          sz_iov;                 // number of fragments
        struct iovec    iov[];                  // fragments size+data
};
*/




class Events{
public:
	int size;
	struct eventStruct event;


	Events();

	int writeOnSessionErrorEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_session_event_data *event_data,
			void *cb_prv_data);
	int writeOnSessionEstablishedEvent (char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_new_session_rsp *rsp,
			void *cb_prv_data);
	int writeOnNewSessionEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_new_session_req *req,
			void *cb_prv_data);
	int writeOnMsgSendCompleteEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_msg *msg,
			void *cb_prv_data);
	int writeOnMsgErrorEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context);
	int writeOnMsgReceivedEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_msg *msg,
			int more_in_batch,
			void *cb_prv_data);

};




#endif // ! Events__H___
