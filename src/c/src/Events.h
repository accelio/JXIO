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


struct  event_new_session {
	int64_t 	ptr_session;
	int32_t 	uri_len;
	char  		uri_str[0]; //to indicate that string uri is written to buffer
	int32_t 	ip_len;
	char 		ip_str[0]; //to  indicate that ip string is written to buffer
} __attribute__ ((packed));

struct __attribute__ ((packed)) event_session_established {
};

struct __attribute__ ((packed)) event_session_error {

	int32_t 	error_type;
	int32_t 	error_reason;
};

struct __attribute__ ((packed)) event_msg_complete {
};

struct __attribute__ ((packed)) event_msg_error_server {
	int64_t         ptr_session;
	//use the ptr inside event_struct for passing the pointer to msg class in java
	int32_t         error_reason;
};

struct __attribute__ ((packed)) event_msg_error_client {
	int32_t         error_reason;
};

struct __attribute__ ((packed)) event_req_received {
	int32_t 	msg_in_size;
	int32_t 	msg_out_size;
	int64_t		ptr_session;
	//use the ptr inside event_struct for passing the pointer to msg class in java
};

struct __attribute__ ((packed)) event_res_received {
	int32_t 	msg_size;
};

struct __attribute__ ((packed)) event_fd_ready {
	int32_t 	fd;
	int32_t 	epoll_event;
};

struct  __attribute__ ((packed)) queued_event_t {
	int32_t type;
	int64_t ptr;//this will indicate on which session the event arrived
	union {
		struct event_new_session new_session;
		struct event_session_established session_established;
		struct event_session_error session_error;
		struct event_msg_complete msg_complete;
		struct event_msg_error_server msg_error_server;
		struct event_msg_error_client msg_error_client;
		struct event_req_received req_received;
		struct event_res_received res_received;
		struct event_fd_ready fd_ready;
	} event_specific;
};



class Events {
public:
	int size;

	Events();

	int writeOnSessionErrorEvent(char *buf, void *ptrForJava, struct xio_session_event_data *event_data);
	int writeOnSessionEstablishedEvent (char *buf, void *ptrForJava, struct xio_session *session,
			struct xio_new_session_rsp *rsp);
	int writeOnNewSessionEvent(char *buf, void *ptrForJava, void *serverSession,
			struct xio_new_session_req *req);
	int writeOnMsgErrorEventServer(char *buf, void *ptrForJavaMsg, void *ptrForJavaSession,
			enum xio_status error);
	int writeOnMsgErrorEventClient(char *buf, void *ptrForJava, enum xio_status error);
	int writeOnRequestReceivedEvent(char *buf, void *ptrForJavaMsg, const int32_t msg_in_size, const int32_t msg_out_size, void *ptrForJavaSession);
	int writeOnResponseReceivedEvent(char *buf, void *ptrForJavaMsg, const int32_t msg_in_size);
	int writeOnFdReadyEvent(char *buf, int fd, int event);

};

#endif // ! Events__H___
