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

#include "bullseye.h"
#include "Utils.h"
#include "Bridge.h"
#include <libxio.h>

log_severity_t g_log_threshold = DEFAULT_LOG_THRESHOLD;
log_severity_t g_xio_log_level_to_jxio_severity[XIO_LOG_LEVEL_LAST];

void log_set_threshold(log_severity_t threshold)
{
	BULLSEYE_EXCLUDE_BLOCK_START
	g_log_threshold = (lsNONE <= threshold && threshold <= lsTRACE) ? threshold : DEFAULT_LOG_THRESHOLD;
	BULLSEYE_EXCLUDE_BLOCK_END
}

void log_func(log_severity_t severity, const char *log_fmt, ...)
{
	const int SIZE = 2048;
	char _str_[SIZE];
	va_list ap;
	va_start(ap, log_fmt);
	int m = vsnprintf(_str_, SIZE, log_fmt, ap);
	va_end(ap);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (m < 0) {
		return; /*error*/
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	_str_[SIZE-1] = '\0';
	Bridge_invoke_logToJava_callback(severity, _str_);
}

void logs_from_xio_callback(const char *file, unsigned line, const char *func, unsigned level, const char *log_fmt, ...)
{
	log_severity_t severity = g_xio_log_level_to_jxio_severity[level];
	if (severity > g_log_threshold)
		return;

	const int SIZE = 2048;
	char _str_[SIZE];
	int n = snprintf(_str_, SIZE, MODULE_FILE_INFO, file, line, func);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (n < 0) {
		return; /*error*/
	}
	if (n < SIZE) {
		va_list ap;
		va_start(ap, log_fmt);
		int m = vsnprintf(_str_ + n, SIZE - n, log_fmt, ap);
		va_end(ap);
		if (m < 0) {
			return; /*error*/
		}
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	_str_[SIZE - 1] = '\0';
	Bridge_invoke_logToJava_callback(severity, _str_);
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

	int optlen = sizeof(xio_log_fn);
	const void* optval = (const void *)logs_from_xio_callback;
	xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_LOG_FN, optval, optlen);
}

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
void logs_from_xio_callback_unregister()
{
	xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_LOG_FN, NULL, 0);
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif

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

bool close_xio_connection(ServerSession* jxio_session)
{
	struct xio_session *session = jxio_session->get_xio_session();
	struct xio_connection *con = jxio_session->get_xio_connection();
	if (!con)
	{
		LOG_DBG("ERROR, con is null for jxio_session %p xio_session %p", jxio_session, session);
		return false;
	}
	LOG_DBG("closing connection %p for xio_session=%p, jxio_session=%p", con, session, jxio_session);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_disconnect(con)) {
		LOG_DBG("ERROR, xio_disconnect failed with error '%s' (%d) (xio_session=%p, conn=%p, jxio_session=%p)",
				xio_strerror(xio_errno()), xio_errno(), session, con, jxio_session);
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	LOG_DBG("successfully closed connection=%p, for xio_session=%p, jxio_session=%p", con, session, jxio_session);
	return true;
}

bool forward_session(ServerSession* jxio_session, const char * url)
{
	struct xio_session *xio_session = jxio_session->get_xio_session();

	jxio_session->set_forward(true);
	int retVal = xio_accept(xio_session, &url, 1, NULL, 0);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (retVal) {
		LOG_ERR("ERROR forwarding session=%p. error '%s' (%d)", xio_session, xio_strerror(xio_errno()), xio_errno());
		jxio_session->set_forward(false);
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	return true;
}

bool accept_session(ServerSession* jxio_session)
{
	struct xio_session *xio_session = jxio_session->get_xio_session();

	int retVal = xio_accept(xio_session, NULL, 0, NULL, 0);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (retVal) {
		LOG_ERR("ERROR accepting session=%p. error '%s' (%d)", xio_session, xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	return true;
}


bool reject_session(ServerSession* jxio_session, int reason,
		char *user_context, size_t user_context_len)
{
	struct xio_session *xio_session = jxio_session->get_xio_session();
	jxio_session->set_reject();
	LOG_DBG("before reject xio_session=%p. reason is %s (%d)", xio_session, xio_strerror(reason), reason);

	int retVal = xio_reject(xio_session, (enum xio_status)reason, user_context, user_context_len);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (retVal) {
		LOG_DBG("ERROR, rejecting session=%p. error '%s' (%d)",xio_session, xio_strerror(xio_errno()), xio_errno());
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	jxio_session->delete_after_teardown = true;
	return true;
}
