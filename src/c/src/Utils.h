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
#include <map>
#include "ServerSession.h"

#define LOG_FATAL(log_fmt, log_args...)	LOG_BY_FILE(lsFATAL, log_fmt, ##log_args)
#define LOG_ERR(log_fmt, log_args...)	LOG_BY_FILE(lsERROR, log_fmt, ##log_args)
#define LOG_WARN(log_fmt, log_args...)	LOG_BY_FILE(lsWARN, log_fmt, ##log_args)
#define LOG_INFO(log_fmt, log_args...)	LOG_BY_FILE(lsINFO, log_fmt, ##log_args)
#define LOG_DBG(log_fmt, log_args...)	LOG_BY_FILE(lsDEBUG, log_fmt, ##log_args)
#define LOG_TRACE(log_fmt, log_args...)	LOG_BY_FILE(lsTRACE, log_fmt, ##log_args)



#define MODULE_INFO		this
#define MODULE_HDR_INFO 	MODULE_NAME "[%p]:%d:%s() "
#define MODULE_FILE_INFO	"%s:%d:%s() "

// THE 'log()' macros that should be used everywhere...
#define LOG_BY_MODULE(severity, log_fmt, log_args...) 	do { if (severity <= g_log_threshold)  log_func(severity, MODULE_HDR_INFO  log_fmt "\n", MODULE_INFO, __LINE__, __FUNCTION__, ##log_args);} while(0)
#define LOG_BY_FILE(severity, log_fmt, log_args...) 	do { if (severity <= g_log_threshold)  log_func(severity, MODULE_FILE_INFO  log_fmt "\n", __FILE__, __LINE__, __FUNCTION__, ##log_args);} while(0)


enum log_severity_t {
	lsNONE,
	lsFATAL,
	lsERROR,
	lsWARN,
	lsINFO,
	lsDEBUG,
	lsTRACE,
};


extern log_severity_t g_log_threshold;
const log_severity_t DEFAULT_LOG_THRESHOLD = lsINFO;

void log_set_threshold(log_severity_t _threshold);

void log_func(log_severity_t severity, const char *log_fmt, ...);

// helper functions to setup log collection from AccelIO into JXIO's logging
void logs_from_xio_callback_register();
void logs_from_xio_callback_unregister();
void logs_from_xio_set_threshold(log_severity_t threshold);

bool close_xio_connection(ServerSession* jxio_session);
bool forward_session(ServerSession* jxio_session, const char * url);
bool accept_session(ServerSession* jxio_session);
bool reject_session(ServerSession* jxio_session, int reason, char *user_context, size_t user_context_len);

#endif // ! Utils__H___
