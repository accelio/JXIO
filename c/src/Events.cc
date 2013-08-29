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



Events::Events()
{
	this->size = 0;

}

int Events::writeOnSessionErrorEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_session_event_data *event_data,
			void *cb_prv_data)
{
	log (lsDEBUG, "****** inside writeOnSessionErrorEvent private data is %p\n",cb_prv_data);

	this->event.type = htonl (0);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->event.event_specific.session_error.error_type = htonl(event_data->event);
	this->event.event_specific.session_error.error_reason = htonl (event_data->reason);

	this->size = sizeof(struct event_session_error) + sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;




	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnSessionEstablishedEvent (char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_new_session_rsp *rsp,
			void *cb_prv_data)
{
	log (lsDEBUG, "****** inside writeOnSessionEstablishedEvent private data is %p\n",cb_prv_data);
	event.type = htonl (2);
	event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnNewSessionEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_new_session_req *req,
			void *cb_prv_data)
{

	log (lsDEBUG, "****** inside writeOnNewSessionEvent private data is %p\n",cb_prv_data);
	void* p1 =  session;

	event.type = htonl (4);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	event.event_specific.new_session.ptr_session = htobe64(intptr_t(p1));
	event.event_specific.new_session.uri_len = htonl(req->uri_len);

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

	}else if (ipStruct->sa_family == AF_INET6) {
			static char addr[INET6_ADDRSTRLEN];
			struct sockaddr_in6 *v6 = (struct sockaddr_in6 *)ipStruct;
			ip = (char *)inet_ntop(AF_INET6, &(v6->sin6_addr),
						 addr, INET6_ADDRSTRLEN);
			len = INET6_ADDRSTRLEN;
	}else{
			log(lsERROR, "can not get src ip\n");
			len = strlen(ip);

	}

	event.event_specific.new_session.ip_len = htonl (len);
	memcpy(buf + this->size, &event.event_specific.new_session.ip_len, sizeof(int32_t));

	this->size += sizeof((struct event_new_session *)0)->ip_len;
	strcpy(buf + this->size,ip);

	this->size += len ;

	return this->size;
}

int Events::writeOnMsgSendCompleteEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
			struct xio_msg *msg,
			void *cb_prv_data)
{
	event.type = htonl (5);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}

int Events::writeOnMsgErrorEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context)
{
	event.type = htonl (1);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}


int Events::writeOnMsgReceivedEvent(char *buf, Contexable *ptrForJava, struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data)
{
	event.type = htonl (3);
	this->event.ptr = htobe64(intptr_t(ptrForJava));
	this->size = sizeof((event_struct *)0)->type + sizeof((event_struct *)0)->ptr;
	memcpy(buf, &this->event, this->size);
	return this->size;
}











