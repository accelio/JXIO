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
#include "Utils.h"
#include "Bridge.h"
#include <libxio.h>

log_severity_t g_log_threshold = DEFAULT_LOG_THRESHOLD;
log_severity_t g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_LAST];

void log_set_threshold(log_severity_t _threshold)
{
	g_log_threshold = (lsNONE <= _threshold && _threshold <= lsTRACE) ? _threshold : DEFAULT_LOG_THRESHOLD;
}

#define log_formating_and_output(_file_, _line_, _func_, _severity_, _fmt_) \
		const int SIZE = 2048; \
		char __str[SIZE]; \
		int n = snprintf(__str, SIZE, "%s:%d:%s() ", _file_, _line_, _func_); \
		if (n < 0) { \
			return; /*error*/ \
		} \
		if (n < SIZE) { \
			va_list ap; \
			va_start(ap, _fmt_); \
			int m = vsnprintf(__str+n, SIZE-n, _fmt_, ap); \
			va_end(ap); \
			if (m < 0) { \
				return; /*error*/ \
			} \
		} \
		__str[SIZE-1] = '\0'; \
		Bridge_invoke_logToJava_callback(__str, _severity_);


void log_func(const char* file, const int line, const char* func, log_severity_t severity, const char *fmt, ...)
{
	log_formating_and_output(file, line, func, severity, fmt);
}

void logs_from_xio_callback(const char *file, unsigned line, const char *func, unsigned level, const char *fmt, ...)
{
	log_severity_t severity = g_xio_log_level_to_jxio_severity[level];
	log_formating_and_output(file, line, func, severity, fmt);
}

void logs_from_xio_callback_register()
{
	// init log level/severity conversion table
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_FATAL] = lsFATAL;
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_ERROR] = lsERROR;
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_WARN] = lsWARN;
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_INFO] = lsINFO;
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_DEBUG] = lsDEBUG;
	g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_TRACE] = lsTRACE;


	xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_LOG_FN, (const void *)logs_from_xio_callback, sizeof(xio_log_fn));
}

void logs_from_xio_callback_unregister()
{
	xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_LOG_FN, NULL, 0);
}

void logs_from_xio_set_threshold(log_severity_t threshold)
{
	int xio_log_level = XIO_LOG_LEVEL_INFO;

	switch (threshold) {
	case lsFATAL:	xio_log_level = XIO_LOG_LEVEL_FATAL; break;
	case lsERROR:	xio_log_level = XIO_LOG_LEVEL_ERROR; break;
	case lsWARN:	xio_log_level = XIO_LOG_LEVEL_WARN; break;
	case lsDEBUG:	xio_log_level = XIO_LOG_LEVEL_DEBUG; break;
	case lsTRACE:	xio_log_level = XIO_LOG_LEVEL_TRACE; break;

	case lsINFO:
	default:
		xio_log_level = XIO_LOG_LEVEL_INFO; break;
	}
	xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_LOG_LEVEL, &xio_log_level, sizeof(enum xio_log_level));
}

ServerSession* delete_ses_server_for_session(xio_session* session){
	map_ses_ctx_t::iterator it;
	pthread_mutex_lock(&mutex_for_map);
	it=map_sessions.find(session);
	if (it == map_sessions.end()){
		log(lsERROR, "session=%p in map\n", session);
		pthread_mutex_unlock(&mutex_for_map);
		return NULL;
	}
	ServerSession *jxio_session = it->second;
	map_sessions.erase(it);
	pthread_mutex_unlock(&mutex_for_map);
	log(lsDEBUG, "deleting pair <jxio_session=%p, xio_session=%p>\n", jxio_session, session);

	return jxio_session;
}

void add_ses_server_for_session(xio_session * xio_session, ServerSession* jxio_session){
	log (lsDEBUG, "adding pair<xio_session=%p, jxio_session=%p>\n", xio_session, jxio_session);
	pthread_mutex_lock(&mutex_for_map);
	map_sessions.insert(pair_ses_ctx_t(xio_session, jxio_session));
	pthread_mutex_unlock(&mutex_for_map);
}



bool close_xio_connection(struct xio_session *session, struct xio_context *ctx)
{
	log (lsDEBUG, "closing connection for session=%p, context=%p\n", session, ctx);
	xio_connection * con = xio_get_connection(session, ctx);
	if (con == NULL) {
		log(lsDEBUG, "ERROR, no connection found (xio_session=%p, xio_context=%p)\n", session, ctx);
		return false;
	}
	if (xio_disconnect(con)) {
		log(lsDEBUG, "ERROR, xio_disconnect failed with error '%s' (%d) (xio_session=%p, xio_context=%p, conn=%p)\n",
				xio_strerror(xio_errno()), xio_errno(), session, ctx, con);
		return false;
	}
	log (lsDEBUG, "successfully closed connection=%p, for session=%p, context=%p\n", con, session, ctx);
	return true;
}

bool forward_session(struct xio_session *xio_session, ServerSession* jxio_session, const char * url) {
	log(lsDEBUG, "url before forward is %s. xio_session is %p\n", url, xio_session);

	int retVal = xio_accept(xio_session, &url, 1, NULL, 0);
	if (retVal) {
		log(lsDEBUG, "ERROR, accepting session=%p. error '%s' (%d)\n", xio_session, xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	add_ses_server_for_session(xio_session, jxio_session);
	return true;
}

bool accept_session(struct xio_session *xio_session, ServerSession* jxio_session) {

	log(lsDEBUG, "before accept xio_session is %p\n", xio_session);

	int retVal = xio_accept(xio_session, NULL, 0, NULL, 0);
	if (retVal) {
		log(lsDEBUG, "ERROR, accepting session=%p. error '%s' (%d)\n",xio_session, xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	add_ses_server_for_session(xio_session, jxio_session);
	return true;
}


bool reject_session(struct xio_session *session, int reason,
		char *user_context, size_t user_context_len) {

	log(lsDEBUG, "before reject xio_session=%p. reason is %d\n", session, reason);

	enum xio_status s = (enum xio_status)(reason + XIO_BASE_STATUS -1);

	int retVal = xio_reject(session, s, user_context, user_context_len);
	if (retVal) {
		log(lsDEBUG, "ERROR, rejecting session=%p. error '%s' (%d)\n",session, xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	return true;
}



