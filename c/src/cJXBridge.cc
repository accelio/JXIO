#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include "cJXBridge.h"


static jclass cls;
static JavaVM *cached_jvm;
static jmethodID jmethodID_on_event; // handle to java cb method


// globals
char* buf;
xio_mr* mr;

/* server private data */
struct hw_server_data {
		struct xio_msg  rsp;	/* global message */
};

// JNI inner functions implementations

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void* reserved)
{
	printf("in cJXBridge - JNI_OnLoad\n");

	cached_jvm = jvm;
	JNIEnv *env;
	if ( jvm->GetEnv((void **)&env, JNI_VERSION_1_4)) { //direct buffer requires java 1.4
		return JNI_ERR; /* JNI version not supported */
	}

	cls = env->FindClass("com/mellanox/JXBridge");
	if (cls == NULL) {
		fprintf(stderr, "in cJXBridge - java class was NOT found\n");
		return JNI_ERR;
	}

	jmethodID_on_event = env->GetStaticMethodID(cls, "on_event", "()V");
	if (jmethodID_on_event == NULL) {
		fprintf(stderr,"in cJXBridge - on_event() callback method was NOT found\n");
		return JNI_ERR;
	}

	printf("in cJXBridge -  java callback methods were found and cached\n");
	return JNI_VERSION_1_4;  //direct buffer requires java 1.4
}



extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void* reserved)
{
	// NOTE: We never reached this place
	return;
}


// callback back to JAVA
int invoke_on_event_callback(){
	JNIEnv *jniEnv;
	if (cached_jvm->GetEnv((void **)&jniEnv, JNI_VERSION_1_4)) {
		printf("Error getting JNIEnv In C++ method");
		return 1;
	}
	printf("before jniEnv->CallStaticVoidMethod... on_event\n");
	jniEnv->CallStaticVoidMethod(cls, jmethodID_on_event);
	printf("after jniEnv->CallStaticVoidMethod...on_event\n");
	return 0;
}



// implementation of the XIO callbacks

int on_new_session_callback(struct xio_session *session,
		struct xio_new_session_req *req,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}


int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}



int on_session_redirected_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}



int on_msg_send_complete_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}



int on_msg_hdr_avail_callback(struct xio_session *session,
		struct xio_msg *msg,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}


int on_msg_callback(struct xio_session *session,
		struct xio_msg *msg,
		int more_in_batch,
		void *cb_prv_data)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}


int on_msg_error_callback(struct xio_session *session,
            enum xio_status error,
            struct xio_msg  *msg,
            void *conn_user_context)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}


int on_session_event_callback(struct xio_session *session,
        struct xio_session_event_data *data,
        void *cb_user_context)
{
	// here we will build and enter the new event to the event queue
	
	// and after calling the callback to the JAVA
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
		return 1;
		}
		
	return 0;
}

// implementation of the native method
//
extern "C" JNIEXPORT jobject JNICALL Java_com_mellanox_JXBridge_createMsgPooNative(JNIEnv *env, jclass cls, jint msg_size, jint num_of_msgs)
{
	printf("inside createMsgPooNative method\n");
	
	int total_size = num_of_msgs * msg_size;
	buf = (char*)malloc(total_size * sizeof(char));
	if(buf== NULL){
		fprintf(stderr, "Error, Could not allocate memory for Msg pool");
		return NULL;
	}

	mr = xio_reg_mr(buf, total_size);
	if(mr == NULL){
		fprintf(stderr, "Error, Could not register memory for Msg pool");
		return NULL;
	}
	printf("requested memory was successfuly allocated and regitered\n");

	jobject jbuf = env->NewDirectByteBuffer(buf, total_size );
	
	if(invoke_on_event_callback()){
		printf("Error invoking the callback to JAVA");
	}	
	
	printf("finished createMsgPooNative method\n");
	return jbuf;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_JXBridge_destroyMsgPoolNative()
{
	if(xio_dereg_mr(&mr) != 0){
		fprintf(stderr, "Error, Could not free the registered memory");
	}
}



extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_JXBridge_startClientSessioNative(JNIEnv *env, jclass cls, jint session_id, jchar address[], jint port) {
	return 0;
}


extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_JXBridge_CloseSessioNative(JNIEnv *env, jint session_id) {
	// i need to build a session manager here, in order to identify the session object by its id and close it.
	int retval = xio_session_close(NULL);
	if (retval != 0) {
		fprintf(stderr, "session close failed");
	}
	return retval;
}


extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_JXBridge_sendMsgNative(JNIEnv *env, jint session_id, jcharArray data, jint lenght) {
	return 0;
}


extern "C" JNIEXPORT jlongArray JNICALL Java_com_mellanox_JXBridge_startServerSessionNative(JNIEnv *env, jclass cls, jstring hostname, jint port) {

	printf("inside startServerSessioNative method\n");

	struct xio_server_ops server_ops;	
	server_ops.on_session_event			    =  on_session_event_callback;
	server_ops.on_msg	  		        	=  on_msg_callback;
	server_ops.on_msg_error				    =  on_msg_error_callback;
	server_ops.on_new_session		        =  on_new_session_callback;
	server_ops.on_msg_send_complete         =  on_msg_send_complete_callback;


	struct xio_server	*server;	/* server portal */
	char			url[256];
	struct xio_context	*ctx;
	void			*loop;

	/* open default event loop */
	loop	= xio_ev_loop_init();

	/* create thread context for the client */
	ctx	= xio_ctx_open(NULL, loop);

	/* create url to connect to */
	sprintf(url, "rdma://%s:%d", hostname, port);
	/* bind a listener server to a portal/url */
	server = xio_bind(ctx, &server_ops, url, NULL);
	if (server == NULL){
		printf("Error in binding server\n");
		return NULL;
	}
	
	printf("Server is now bind to address\n");
	printf("finished startServerSessioNative method\n");
	jlongArray arr =  env->NewLongArray(3);
	return arr;
}


JNIEnv *JX_attachNativeThread()
{
    JNIEnv *env;
	if (! cached_jvm) {
		printf("cached_jvm is NULL");
	}
    jint ret = cached_jvm->AttachCurrentThread((void **)&env, NULL);

	if (ret < 0) {
		printf("cached_jvm->AttachCurrentThread failed ret=%d", ret);
	}
	printf("completed successfully env=%p", env);
    return env; // note: this handler is valid for all functions in this thread
}








