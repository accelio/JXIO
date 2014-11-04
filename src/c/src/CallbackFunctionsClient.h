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
#include "Client.h"


/*
 * this callback is called by xio library once a client recieves new msg.
 * @cb_prv_data: represents client that holds pointer to context class.
 * Through it we can access buffer which is shared with Java.
 */
int on_msg_callback_client(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data);
/*
 * this callback is called by xio library once there is msg error.
 * @conn_user_context: represents client that holds pointer to context class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_msg_error_callback_client(struct xio_session *session,
            enum xio_status error,
            enum xio_msg_direction direction,
            struct xio_msg  *msg,
            void *cb_prv_data);
/*
 * this callback is called by xio library once a client receives notice that a session is established
 * with the server.
 * @cb_prv_data: represents client that holds pointer to context class.
 *  Through it we can access buffer which is shared with Java.
 */
int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data);
/*
 * this callback is called by xio library once client recieves session event.
 * @cb_prv_data: represents client that holds pointer to context class.
 * Through it we can access buffer which is shared with Java.
 */
int on_session_event_callback_client(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data);



#endif // ! CallbackFunctions__H___
