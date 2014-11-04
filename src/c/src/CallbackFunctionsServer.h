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

#ifndef CallbackFunctions__H___
#define CallbackFunctions__H___

#include <errno.h>
#include <libxio.h>
#include "Context.h"
#include "ServerPortal.h"
#include "ServerSession.h"


/*
 * this callback is called by xio library once a server recieves new session request.
 * @cb_prv_data: represents portal that holds pointer to cJXCtx class.
 * Through it we can access buffer which is shared with Java.
 */
int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data);

/*
 * this callback is called by xio library once server finishes sending a msg.
 * @cb_prv_data: represents portal that holds pointer to cJXCtx class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data);
/*
 * this callback is called by xio library once a server/client recieves new msg.
 * @cb_prv_data: represents portal that holds pointer to cJXCtx class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_msg_callback_server(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data);
/*
 * this callback is called by xio library once there is msg error.
 * @conn_user_context: represents portal that holds pointer to cJXCtx class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_msg_error_callback_server(struct xio_session *session,
            enum xio_status error,
            enum xio_msg_direction direction,
            struct xio_msg  *msg,
            void *cb_prv_data);

/*
 * this callback is called by xio library once a server/client recieve session event.
 * @cb_prv_data: represents class implementing portal that holds pointer to cJXCtx class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_session_event_callback_server(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data);

/* this callback is called by server side when a request is received from client side and
 * it needs to be supplied with buffer
 */
int on_buffer_request_callback (struct xio_msg *msg,
			void *cb_user_context);



#endif // ! CallbackFunctions__H___
