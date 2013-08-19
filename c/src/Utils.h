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
	lsFATAL,
	lsERROR,
	lsWARN,
	lsINFO,
	lsDEBUG,
};

static bool log_to_unique_file = true;
static FILE *log_file = NULL;

void log_func(const char * func, const char * file, int line, log_severity_t severity, const char *fmt, ...); // should not be called directly






#define log(severity, ...) log_func(__func__, __FILE__, __LINE__, severity, __VA_ARGS__)
// log backtrace at the desired severity + 'return' value is the backtrace
// TIP: use severity=lsNONE to skip log and only get ret value
//std::string print_backtrace(const char *label = NULL, log_severity_t severity = lsTRACE);
//#define log(severity, ...) printf ("")
#endif // ! Utils__H___
