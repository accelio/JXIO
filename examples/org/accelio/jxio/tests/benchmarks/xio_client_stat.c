/*
 * Copyright (c) 2013 Mellanox Technologies®. All rights reserved.
 *
 * This software is available to you under a choice of one of two licenses.
 * You may choose to be licensed under the terms of the GNU General Public
 * License (GPL) Version 2, available from the file COPYING in the main
 * directory of this source tree, or the Mellanox Technologies® BSD license
 * below:
 *
 *      - Redistribution and use in source and binary forms, with or without
 *        modification, are permitted provided that the following conditions
 *        are met:
 *
 *      - Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      - Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *      - Neither the name of the Mellanox Technologies® nor the names of its
 *        contributors may be used to endorse or promote products derived from
 *        this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include <time.h>
#include <errno.h>


#include "libxio.h"
#define NUM_ITER	1
#define NSEC_PER_SEC    1000000000L

/* private session data */
struct session_data {
	long			id;
	long 			num_sessions;
	void			*loop;
	struct xio_connection	*conn;
};

/*---------------------------------------------------------------------------*/
/* on_session_event							     */
/*---------------------------------------------------------------------------*/
static int on_session_event(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_user_context)
{
	struct session_data *session_data = cb_user_context;
	static int sessions_counter = 0;
//	printf("session event: %s. reason: %s\n",
//	       xio_session_event_str(event_data->event),
//	       xio_strerror(event_data->reason));

	switch (event_data->event) {
	case XIO_SESSION_ERROR_EVENT:
		printf("session event: %s. reason: %s\n",
		       xio_session_event_str(event_data->event),
		       xio_strerror(event_data->reason));
		xio_disconnect(event_data->conn);
		break;
	case XIO_SESSION_REJECT_EVENT:
	case XIO_SESSION_CONNECTION_DISCONNECTED_EVENT:
		xio_disconnect(event_data->conn);
		break;
	case XIO_SESSION_TEARDOWN_EVENT:
		sessions_counter++;
		//fprintf(stderr, "session #%d established:\n", sessions_counter);
		xio_session_close(session);

		if(sessions_counter == session_data->num_sessions) {
			//fprintf(stderr, "All sessions( %d )are established, stopping event loop\n", sessions_counter);
			xio_ev_loop_stop(session_data->loop);
			sessions_counter = 0;
		}

		break;
	default:
		break;
	};

	return 0;
}


static int on_session_established(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_user_context)
{
	struct session_data *session_data = (struct session_data *) cb_user_context;
	//fprintf(stderr, "#%lu session established\n", session_data->id);
	xio_disconnect(session_data->conn);
	return 0;
}

/*---------------------------------------------------------------------------*/
/* callbacks								     */
/*---------------------------------------------------------------------------*/
struct xio_session_ops ses_ops = {
	.on_session_event		=  on_session_event,
	.on_session_established		=  on_session_established,
	.on_msg				=  NULL, //on_response,
	.on_msg_error			=  NULL
};

/*---------------------------------------------------------------------------*/
/* main									     */
/*---------------------------------------------------------------------------*/
int main(int argc, char *argv[])
{
	struct xio_session	**sessions = NULL;
	char			url[256];
	struct xio_context	*ctx;
	struct session_data	*session_data = NULL;
	int  			j = 0, n = 0;
	struct timespec		start, end;
	int 			num_sessions;
	void 			* loop = NULL;
	/* client session attributes */
	struct xio_session_attr attr = {
		&ses_ops, /* callbacks structure */
		NULL,	  /* no need to pass the server private data */
		0
	};

	if(argc < 4) {
		fprintf(stderr, "./xio_client ${server_ip} ${port} ${num_connections}\n");
		return -1;
	}

	num_sessions = atoi(argv[3]);

	/* open default event loop */
	loop = xio_ev_loop_init();
	if (loop == NULL) {
		fprintf(stderr, "Failed to allocate event loop\n");
		return -1;
	}

	/* create thread context for the client */
	ctx = xio_ctx_open(NULL, loop, 0);
	if(ctx == NULL) {
		fprintf(stderr, "Failed to allocate thread context\n");
		xio_ev_loop_destroy(&loop);
		return -1;

	}

	session_data = malloc(num_sessions*sizeof(struct session_data));
	sessions     = malloc(num_sessions*sizeof(struct sessions *));

	if( session_data == NULL || sessions == NULL ) {
		fprintf(stderr, "Allocation failed\n");
		xio_ctx_close(ctx);
		xio_ev_loop_destroy(&loop);
		if(session_data) {
			free(session_data);
		}
		if(sessions) {
			free(sessions);
		}
		return -1;
	}

	for(n = 0; n < num_sessions; n++)
	{
		session_data[n].loop = loop;
		session_data[n].num_sessions = num_sessions;
		session_data[n].id = n;
	}

	/* create url to connect to */
	sprintf(url, "rdma://%s:%s", argv[1], argv[2]);
	
	if(clock_gettime(CLOCK_MONOTONIC, &start)) {
		fprintf(stderr, "clock_gettime() failed, errno = %d\n", errno);
	}

	for (j = 0; j< NUM_ITER; j++)
	{

		for(n = 0; n < num_sessions; n++)
		{
			sessions[n] = xio_session_open(XIO_SESSION_REQ,
					&attr, url, 0, 0, &session_data[n]);
			/* connect the session  */
			//fprintf(stderr, "Connect Session\n");
			session_data[n].conn = xio_connect(sessions[n], ctx, 0, &session_data[n]);
			if (session_data[n].conn == NULL) {
				xio_session_close(sessions[n]);
				break;
			}
		}

		/* the default xio supplied main loop */
		xio_ev_loop_run(loop);
	}

	if(clock_gettime(CLOCK_MONOTONIC, &end)) {
			fprintf(stderr, "clock_gettime() failed, errno = %d\n", errno);
	}

	/* normal exit phase */
	fprintf(stdout, "exit signaled\n");

	/* free the context */
	xio_ctx_close(ctx);

	/* destroy the default loop */
	xio_ev_loop_destroy(&loop);

	long long l_nsec = ((end.tv_sec * NSEC_PER_SEC + end.tv_nsec) - (start.tv_sec * NSEC_PER_SEC - start.tv_nsec));
	double nsec = ((end.tv_sec * NSEC_PER_SEC + end.tv_nsec) - (start.tv_sec * NSEC_PER_SEC - start.tv_nsec))*1.0/NUM_ITER;
	fprintf(stdout, "It took %lf sec for %d sessions total of %llu for %d\n",
				(double)(nsec/NSEC_PER_SEC), num_sessions, l_nsec, NUM_ITER);


	free(session_data);
	free(sessions);
	return 0;
}

