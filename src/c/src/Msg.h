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
#ifndef Msg__H___
#define Msg__H___

#include <stdlib.h>
#include <stdio.h>
#include <libxio.h>

class MsgPool;

#define vmsg_sglist(vmsg)					\
		(((vmsg)->sgl_type == XIO_SGL_TYPE_IOV) ?	\
		 (vmsg)->data_iov.sglist :			\
		 (((vmsg)->sgl_type ==  XIO_SGL_TYPE_IOV_PTR) ?	\
		 (vmsg)->pdata_iov.sglist : NULL))


class Msg {
public:
	Msg(char * buf, struct xio_mr *xio_mr, int in_buf_size, int out_buf_size, MsgPool* pool);
	~Msg();
	void set_xio_msg_client_fields(); //this method is used by client side
	void set_xio_msg_mirror_fields();
	void set_xio_msg_server_fields();
	void set_xio_msg_fields_for_assign(struct xio_msg *msg); //used when assign_buffer callback is called
	void set_xio_msg_req(struct xio_msg *msg); //this method is used by server side
	void set_xio_msg_out_size(struct xio_msg *xio_msg, const int out_size);
	void set_xio_msg_in_size(struct xio_msg *xio_msg, const int in_size);
	void release_to_pool();
	void* get_buf() { return buf; }
	struct xio_msg* get_xio_msg() { return &xio_msg; }
	struct xio_msg* get_mirror_xio_msg() { return &xio_msg_mirror; }
	int send_response(const int size);
	int get_in_size() { return this->in_buf_size; }
	int get_out_size() { return this->out_buf_size; }
	bool was_assign_called() { return this->assign_called; }
	void dump(struct xio_msg *m); //func for debugging only

private:
	char * buf;
	char * buf_out;
	struct xio_mr *xio_mr;
	struct xio_msg xio_msg;
	struct xio_msg xio_msg_mirror;
	int in_buf_size;
	int out_buf_size;
	MsgPool* pool;
	bool assign_called;
};



inline const int get_xio_msg_in_size(struct xio_msg *msg)
{
	struct xio_iovec_ex *sglist = vmsg_sglist(&msg->in);
	const int msg_in_size = (msg->in.data_iov.nents > 0) ? sglist[0].iov_len : 0;
	return msg_in_size;
}

inline const int get_xio_msg_out_size(struct xio_msg *msg)
{
	struct xio_iovec_ex *sglist = vmsg_sglist(&msg->out);
	const int msg_out_size = (msg->out.data_iov.nents > 0) ? sglist[0].iov_len : 0;
	return msg_out_size;
}


#endif // ! Msg__H___
