#ifndef cJXBridge__H___
#define cJXridge__H___

#include <libxio.h>



// wrappers arround java callbck methods
int xio_on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data);

int xio_on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data);

int xio_on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_private_data);

int xio_on_session_redirected_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_private_data);

int xio_on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_private_data);

int xio_on_msg_hdr_avail_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_private_data);

int xio_on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_private_data);

int xio_on_msg_error_callback(struct xio_session *session,
		enum xio_status error,
		struct xio_msg  *msg,
		void *cb_private_data);


#endif // ! cJxBridge__H___
