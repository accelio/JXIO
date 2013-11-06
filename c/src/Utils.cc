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

log_severity_t g_log_threshold = DEFAULT_LOG_THRESHOLD;

void log_set_threshold(log_severity_t _threshold)
{
	g_log_threshold = (lsNONE <= _threshold && _threshold <= lsTRACE) ? _threshold : DEFAULT_LOG_THRESHOLD;
}

void log_func(const char* file, const int line, const char* func, log_severity_t severity, const char *fmt, ...)
{
	const int SIZE = 2048;
	char str[SIZE];

	int n = snprintf(str, SIZE, "%s:%d:%s() ", file, line, func);
	if (n < 0) {
		return; //error
	}

	if (n < SIZE) {
		va_list ap;
		va_start(ap, fmt);
		int m = vsnprintf(str+n, SIZE-n, fmt, ap);
		va_end(ap);

		if (m < 0) {
			return; //error
		}
	}

	str[SIZE-1] = '\0';

	Bridge_invoke_logToJava_callback(str, severity);
}


bool close_xio_connection(struct xio_session *session, struct xio_context *ctx)
{
	xio_connection * con = xio_get_connection(session, ctx);
	if (con == NULL){
		log(lsERROR, "no connection found (xio_session=%p, xio_context=%p)", session, ctx);
		return false;
	}
	if (xio_disconnect(con)) {
		log(lsERROR, "xio_disconnect failed (xio_session=%p, xio_context=%p)", session, ctx");
		return false;
	}
	return true;

}
