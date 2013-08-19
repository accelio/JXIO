
#include "Utils.h"


void log_func(const char * func, const char * file, int line, log_severity_t severity, const char *fmt, ...)
{
    const int SIZE = 1024;
    char s1[SIZE];
    va_list ap;
    va_start(ap, fmt);
    int n = vsnprintf(s1, SIZE, fmt, ap);
    va_end(ap);


//	sprintf(full_path, "%s/hadoop-%s-udaMOFSupplier-%s.log", rdmalog_dir, user, host);
//	printf("log will go to: %s\n", full_path);
	log_file = fopen ("bla","a");



    if (n < 0) return; //error

        s1[SIZE-1] = '\0';
    	time_t _time = time(0);
    	struct tm _tm;
    	localtime_r(&_time, &_tm);

    	// log to uda's files
        static const char *severity_string[] = {
    		"FATAL",
    		"ERROR",
    		"WARN",
    		"INFO",
    		"DEBUG"
        };
	fprintf(stdout, "%02d:%02d:%02d %-5s [thr=%x %s() %s:%d] %s",
				  _tm.tm_hour, _tm.tm_min, _tm.tm_sec,
				  severity_string[severity],
				  (int)pthread_self(), func, file, line, s1);

//		fflush(log_file);

}








