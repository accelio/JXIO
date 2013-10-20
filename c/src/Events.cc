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

#include "Events.h"


typedef enum {
	EVENT_SESSION_ERROR = 0,
	EVENT_MSG_ERROR = 1,
	EVENT_SESSION_ESTABLISHED = 2,
	EVENT_REQUEST_RECEIVED = 3,
	EVENT_REPLY_RECEIVED = 4,
	EVENT_SESSION_NEW = 5,
	EVENT_MSG_SEND_COMPLETE = 6,
	EVENT_FD_READY = 7,
	EVENT_LAST
} event_type_t;


Events::Events()
{
	this->size = 0;
}

int Events::writeOnSessionErrorEvent(char *buf, void *ptrForJava, struct xio_session *session,
			struct xio_session_event_data *event_data)
{
	this->event.type = htonl(EVENT_SESSION_ERROR);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->event.event_specific.session_error.error_type = htonl(event_data->event);
	this->event.event_specific.session_error.error_reason = htonl (event_data->reason);

	this->size = sizeof(struct event_session_error) + sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;

	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnSessionEstablishedEvent (char *buf, void *ptrForJava, struct xio_session *session,
			struct xio_new_session_rsp *rsp)
{
	this->event.type = htonl(EVENT_SESSION_ESTABLISHED);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnNewSessionEvent(char *buf, void *ptrForJava, struct xio_session *session,
			struct xio_new_session_req *req)
{
	void* p1 =  session;

	this->event.type = htonl(EVENT_SESSION_NEW);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->event.event_specific.new_session.ptr_session = htobe64(intptr_t(p1));
	this->event.event_specific.new_session.uri_len = htonl(req->uri_len);

	//copy data so far
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr +
			sizeof((struct event_new_session *)0)->ptr_session + sizeof((struct event_new_session *)0)->uri_len;

			sizeof(int64_t)*2 + sizeof(int32_t)*2;
//	this->size =  sizeof(int32_t) *2;

//	memset (buf ,0, this->size );

	memcpy(buf, &this->event, this->size);

	//copy first string
	strcpy(buf +this->size,req->uri);
	size+=req->uri_len;

	//calculate ip address
	int len;
	char * ip;

	struct sockaddr *ipStruct = (struct sockaddr *)&req->src_addr;

	if (ipStruct->sa_family == AF_INET) {
				static char addr[INET_ADDRSTRLEN];
				struct sockaddr_in *v4 = (struct sockaddr_in *)ipStruct;
				ip = (char *)inet_ntop(AF_INET, &(v4->sin_addr),
							 addr, INET_ADDRSTRLEN);
				len = strlen(ip);

	} else if (ipStruct->sa_family == AF_INET6) {
			static char addr[INET6_ADDRSTRLEN];
			struct sockaddr_in6 *v6 = (struct sockaddr_in6 *)ipStruct;
			ip = (char *)inet_ntop(AF_INET6, &(v6->sin6_addr),
						 addr, INET6_ADDRSTRLEN);
			len = INET6_ADDRSTRLEN;
	} else {
			log(lsERROR, "can not get src ip\n");
			len = strlen(ip);

	}

	this->event.event_specific.new_session.ip_len = htonl (len);
	memcpy(buf + this->size, &event.event_specific.new_session.ip_len, sizeof(int32_t));

	this->size += sizeof((struct event_new_session *)0)->ip_len;
	strcpy(buf + this->size,ip);

	this->size += len ;
	return this->size;
}

int Events::writeOnMsgSendCompleteEvent(char *buf, void *ptrForJava, struct xio_session *session,
			struct xio_msg *msg)
{
	this->event.type = htonl(EVENT_MSG_SEND_COMPLETE);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnMsgErrorEvent(char *buf, void *ptrForJava, struct xio_session *session,
            enum xio_status error, struct xio_msg *msg)
{
	this->event.type = htonl(EVENT_MSG_ERROR);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}


int Events::writeOnReqReceivedEvent(char *buf, void *ptrForJavaMsg, void *ptrForJavaSession,
		struct xio_msg *msg, int type)
{

	this->event.type = htonl(EVENT_REQUEST_RECEIVED);
	this->event.ptr = htobe64(intptr_t(ptrForJavaMsg));
	this->event.event_specific.req_received.ptr_session = htobe64(intptr_t(ptrForJavaSession));
	this->size = sizeof(struct event_req_received) +  sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnReplyReceivedEvent(char *buf, void *ptrForJavaMsg, struct xio_msg *msg, int type)
{
	this->event.type = htonl(EVENT_REPLY_RECEIVED);
	this->event.ptr = htobe64(intptr_t(ptrForJavaMsg));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}


int Events::writeOnFdReadyEvent(char *buf, int fd, int epoll_event)
{
	this->event.type = htonl(EVENT_FD_READY);
	this->event.ptr = NULL; //  The java object receiving this event will be the EQH which handles this event_queue
	this->event.event_specific.fd_ready.fd = htonl(fd);
	this->event.event_specific.fd_ready.epoll_event = htonl(epoll_event);
	this->size = sizeof(struct event_fd_ready) + sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}
