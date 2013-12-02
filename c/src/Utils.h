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
#ifndef Utils__H___
#define Utils__H___

#include <string>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <errno.h>
#include <stdarg.h> //ok
#include <time.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <pwd.h>
#include <unistd.h>
#include <execinfo.h>  // for backtrace

enum log_severity_t {
	lsNONE,
	lsFATAL,
	lsERROR,
	lsWARN,
	lsINFO,
	lsDEBUG,
	lsTRACE,
};

// THE 'log()' macro that should be used everywhere...
#define log(severity, ...) do { if (severity <= g_log_threshold)  log_func(__FILE__, __LINE__, __FUNCTION__, severity, __VA_ARGS__);} while(0)

extern log_severity_t g_log_threshold;
const log_severity_t DEFAULT_LOG_THRESHOLD = lsINFO;

void log_set_threshold(log_severity_t _threshold);

void log_func(const char* file, const int line, const char* func, log_severity_t severity, const char *fmt, ...); // should not be called directly

// helper functions to setup log collection from AccelIO into JXIO's logging
void logs_from_xio_callback_register();
void logs_from_xio_callback_unregister();
void logs_from_xio_set_threshold(log_severity_t threshold);

bool close_xio_connection(struct xio_session *session, struct xio_context *ctx);
bool forward_session(struct xio_session *session, const char * url);
bool accept_session(struct xio_session *session);
bool reject_session(struct xio_session *session, int reason,
		char *user_context, size_t user_context_len);

#endif // ! Utils__H___
