#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <map>
#include <jni.h>

#include "cJXBridge.h"


static jclass cls;
static JavaVM *cached_jvm;
static jmethodID jmethodID_on_event; // handle to java cb method



struct bufferEventQ{
	char* buf;
	int offset;
	void* evLoop;
};

// globals
char* buf;
xio_mr* mr;
std::map<void*,bufferEventQ*>* mapContextEventQ;

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

	//Katya - some init code
	mapContextEventQ = new std::map<void*,bufferEventQ*> ();
	if(mapContextEventQ== NULL){
		fprintf(stderr, "Error, Could not allocate memory ");
		return NULL;
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

extern "C" JNIEXPORT jlongArray JNICALL Java_com_mellanox_JXBridge_createEQHNative(JNIEnv *env, jclass cls)
{
	void * ev_loop;
	struct xio_context	*ctx;
	jlongArray dataToJava;
	jlong temp[2];

	ev_loop = xio_ev_loop_init();
	if (ev_loop == NULL){
		fprintf(stderr, "Error, ev_loop_init failed");
		return 0;
	}

	ctx = xio_ctx_open(NULL, ev_loop);
	if (ctx == NULL){
		fprintf(stderr, "Error, ev_loop_init failed");
		return 0;
	}

	dataToJava = env->NewLongArray(2);
	 if (dataToJava == NULL) {
		 printf("Error in allocating array via jni\n");
		 return NULL;
	 }
	 // fill a temp structure to use to populate the java long array

	 temp[0] = (jlong)(intptr_t) ev_loop;
	 temp[1] = (jlong)(intptr_t) ctx;

	 // move from the temp structure to the java structure
	 env->SetLongArrayRegion(dataToJava,0, 2, temp);
	 printf("createEQHNative done\n");
	 return dataToJava;

}


//Katya
extern "C" JNIEXPORT jobject JNICALL Java_com_mellanox_JXBridge_allocateEventQNative(JNIEnv *env, jclass cls, jlong ptrCtx, jlong ptrEvLoop, jint event_size, jint num_of_events)
{

	struct xio_context *ctx;
	struct bufferEventQ* beq;
	int total_size = num_of_events * event_size;


	//allocating struct for event queue
	beq = (bufferEventQ*)malloc(total_size * sizeof(bufferEventQ));

	if (beq == NULL){
		fprintf(stderr, "Error, Could not allocate memory ");
		return NULL;
	}

	//allocating buffer that will hold the event queue
	beq->buf = (char*)malloc(total_size * sizeof(char));
	if (beq->buf== NULL){
		fprintf(stderr, "Error, Could not allocate memory for Event Queue buffer");
		return NULL;
	}

	beq->offset = 0;

	void * evLoop = (void *)ptrEvLoop;
	beq->evLoop = evLoop;


	ctx = (struct xio_context *)ptrCtx;
	//inserting into map

	mapContextEventQ->insert(std::pair<void*, bufferEventQ*>(ctx, beq));

	jobject jbuf = env->NewDirectByteBuffer(beq->buf, total_size );
	printf("allocateEventQNative done\n");

	return jbuf;
}



//Katya
extern "C" JNIEXPORT void JNICALL Java_com_mellanox_JXBridge_closeEQHNative(JNIEnv *env, jclass cls, jlong ptrCtx, jlong ptrEvLoop)
{
	void* ev_loop;

	struct xio_context *ctx = (struct xio_context *)ptrCtx;

	printf("beginning of closeEQH\n");
	std::map<void*,bufferEventQ*>::iterator it = mapContextEventQ->find(ctx);
	struct bufferEventQ* beq = it->second;
	//delete from map
	mapContextEventQ->erase(it);
	//free memory
	free(beq->buf);
	free(beq);

	xio_ctx_close(ctx);

	ev_loop = (void*) ptrEvLoop;
	/* destroy the event loop */
	xio_ev_loop_destroy(&ev_loop);
	printf("end of closeEQH\n");
}



//Katya
extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_JXBridge_runEventLoopNative(JNIEnv *env, jclass cls, jlong ptr)
{
	int ret_val;
	void *evLoop = (void *)ptr;

	printf("runEventLoopNative 1");

	ret_val = xio_ev_loop_run(evLoop);
	if (!ret_val){
		printf("event_loop run failed");
	}
	printf("runEventLoopNative 2");
	return ret_val;

}



//Katya
extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_JXBridge_closeSesConNative(JNIEnv *env, jclass cls, jlong ptrSes, jlong ptrCon)
{

	int ret_val1, ret_val2;
	struct xio_connection *con;
	struct xio_session *session;

	printf("beginning of closeSesCon\n");

	con = (struct xio_connection *)ptrCon;

	ret_val1 = xio_disconnect (con);

	if (ret_val1){
		fprintf(stderr, "Error, xio_disconnect failed");
	}

	session = (struct xio_session *)ptrSes;

	ret_val2 = xio_session_close (session);

	if (ret_val2){
		fprintf(stderr, "Error, xio_session_close failed");
	}

	if (ret_val1 || ret_val2){
		return false;
	}
	printf("end of closeSesCon\n");
	return true;

}



/* amir's
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
*/


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



//Katya
int on_session_established_callback(struct xio_session *session,
		struct xio_new_session_rsp *rsp,
		void *cb_prv_data){

	struct xio_context * ctx;
	struct bufferEventQ* beq;
	std::map<void*,bufferEventQ*>::iterator it;
	jint event;

	printf("got callback!!!!!!\n");



	ctx = (xio_context*)cb_prv_data;
	it = mapContextEventQ->find(ctx);

	event = 0;

	beq = it->second;
	xio_ev_loop_stop(beq->evLoop);

	memcpy(beq->buf + beq->offset, &event, sizeof(event));//TODO: to make number of event enum
	beq->offset += 64; //TODO: static variable??? pass it from java

	printf("the end of on_session_established_callback\n");
	return 0;
}

int on_session_event_callback(struct xio_session *session,
		struct xio_session_event_data *event_data,
		void *cb_prv_data){


	struct xio_context * ctx;
	struct bufferEventQ* beq;
	std::map<void*,bufferEventQ*>::iterator it;

	jint event, error_type, len; //int32_t
	printf("the beginning of on_session_event_callback\n");

	ctx = (xio_context*)cb_prv_data;
	it = mapContextEventQ->find(ctx);
	beq = it->second;

	event = 2;
	error_type = event_data->event;

	const char* reason = xio_strerror (event_data->reason);
	len = strlen (reason);

	if (len +1 + sizeof(jint)){
		printf("reason too long"); //TODO: should not happen but add a falg indicating double event occupation
	}

	memcpy(beq->buf + beq->offset, &event, sizeof(event));
	beq->offset +=sizeof(event);
	memcpy(beq->buf + beq->offset, &error_type, sizeof(error_type));
	beq->offset +=sizeof(error_type);
	memcpy(beq->buf + beq->offset, &len, sizeof(len));
	beq->offset +=sizeof(len);
	memcpy(beq->buf + beq->offset, &reason, len+1);
	beq->offset += len + 1;

	beq->offset += (64 - (len +1 + sizeof(jint))); //TODO: static variable??? pass it from java

	printf("the end of on_session_event_callback\n");
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



extern "C" JNIEXPORT jlongArray JNICALL Java_com_mellanox_JXBridge_startClientSessionNative(JNIEnv *env, jclass cls, jstring jhostname, jint port, jlong ptrCtx) {

	struct xio_session	*session;
	struct xio_connection * con;
	char			url[256];
	jlongArray dataToJava;
	jlong temp[2];


	struct xio_context *ctx = (struct xio_context *)ptrCtx;

	const char *hostname = env->GetStringUTFChars(jhostname, NULL);

	sprintf(url, "rdma://%s:%d", hostname, port);

	struct xio_session_ops ses_ops;
	ses_ops.on_session_event		=  on_session_event_callback;
	ses_ops.on_session_established		=  on_session_established_callback;
	ses_ops.on_msg				=  NULL;
	ses_ops.on_msg_error			=  NULL;


	struct xio_session_attr attr;
	attr.ses_ops = &ses_ops; /* callbacks structure */
	attr.user_context = NULL;	  /* no need to pass the server private data */
	attr.user_context_len = 0;

	session = xio_session_open(XIO_SESSION_REQ,
				   &attr, url, 0, ctx);
	env->ReleaseStringUTFChars(jhostname, hostname);

	if (session == NULL){
		printf("Error in creating session\n");
		return NULL;
	}

	/* connect the session  */
	con = xio_connect(session, ctx, 0, ctx);

	if (con == NULL){
		printf("Error in creating connection\n");
		return NULL;
	}

	dataToJava = env->NewLongArray(2);
	if (dataToJava == NULL) {
		printf("Error in allocating array via jni\n");
		 return NULL;
	 }
	 // fill a temp structure to use to populate the java long array

	 temp[0] = (jlong)(intptr_t) session;
	 temp[1] = (jlong)(intptr_t) con;

	 // move from the temp structure to the java structure
	 env->SetLongArrayRegion(dataToJava,0, 2, temp);

	 printf("startClientSession done with \n");

	 /*
	 printf("for debugging \n");
	 struct xio_msg *req = (struct xio_msg *) malloc(sizeof(struct xio_msg));

	 // create "hello world" message
	 memset(req, 0, sizeof(req));
	 req->out.header.iov_base = strdup("hello world header request");
	 req->out.header.iov_len = strlen("hello world header request");
	 	// send first message
	 xio_send_request(con, req);
	 printf("done debugging \n");

	 */
	 return dataToJava;
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








