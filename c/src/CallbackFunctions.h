

#ifndef CallbackFunctions__H___
#define CallbackFunctions__H___


#include <errno.h>
#include <libxio.h>
#include "cJXCtx.h"
#include "Utils.h"
#include "cJXSession.h"



//class cJXSession;

int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data);

int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data);

int on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data);

int on_msg_error_callback(struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context);

int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data);

int on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data);




#endif // ! CallbackFunctions__H___
