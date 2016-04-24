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

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <map>
#include <jni.h>

#include <libxio.h>

#include "bullseye.h"
#include "Client.h"
#include "ServerPortal.h"
#include "Context.h"
#include "Utils.h"
#include "MsgPool.h"
#include "Bridge.h"

static jclass cls;
static JavaVM *cached_jvm;

static jclass cls_data;
static jweak jweakBridge; // use weak global ref for allowing GC to unload & re-load the class and handles
static jclass jclassBridge; // just casted ref to above jweakBridge. Hence, has same life time
static jfieldID fidPtr;
static jfieldID fidBuf;
static jfieldID fidError;
static jmethodID jmethodID_logToJava; // handle to java cb method
static jmethodID jmethodID_requestForBoundMsgPool;

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
extern "C" void bridge_print_error(const char* logmsg)
{
	fprintf(stderr, "in JXIO/c/Bridge: %s\n", logmsg);
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif

static const char jxio_git_describe_str[] = GIT_VERSION;
const char* get_version()
{
	return jxio_git_describe_str;
}

const char* get_version_xio()
{
	return xio_version();
}

// JNI inner functions implementations
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void* reserved)
{
	//printf("in JXIO/c/Bridge - JNI_OnLoad\n");
	xio_init();

	cached_jvm = jvm;
	JNIEnv *env;

	// direct buffer requires java 1.4
	BULLSEYE_EXCLUDE_BLOCK_START
	if (jvm->GetEnv((void **)&env, JNI_VERSION_1_4)) { 
		bridge_print_error("JNI version 1.4 and higher requiered");
		return JNI_ERR;
	}

	cls = env->FindClass("org/accelio/jxio/impl/Bridge");
	if (cls == NULL) {
		bridge_print_error("java class was NOT found");
		return JNI_ERR;
	}

	// keeps the handle valid after function exits, but still, use weak global ref
	// for allowing GC to unload & re-load the class and handles
	jweakBridge = env->NewWeakGlobalRef(cls);
	if (jweakBridge == NULL) {
		bridge_print_error("C++ weak global ref to java class was NOT found");
		return JNI_ERR;
	}
	jclassBridge = (jclass)jweakBridge;

	cls_data = env->FindClass("org/accelio/jxio/EventQueueHandler$DataFromC");
	if (cls_data == NULL) {
		bridge_print_error("java class was NOT found");
		return JNI_ERR;
	}

	if (fidPtr == NULL) {
		fidPtr = env->GetFieldID(cls_data, "ptrCtx", "J");
		if (fidPtr == NULL) {
			bridge_print_error("could not get field ptrCtx");
		}
	}

	if (fidBuf == NULL) {
		fidBuf = env->GetFieldID(cls_data, "eventQueue","Ljava/nio/ByteBuffer;");
		if (fidBuf == NULL) {
			bridge_print_error("could not get field fidBuf");
		}
	}

	// logToJava callback
	jmethodID_logToJava = env->GetStaticMethodID(jclassBridge, "logToJava", "(Ljava/lang/String;I)V");
	if (jmethodID_logToJava == NULL) {
		bridge_print_error("C++ java Bridge.logToJava() callback method was NOT found");
		return JNI_ERR;
	}

	// requestForBoundMsgPool callback
	jmethodID_requestForBoundMsgPool = env->GetStaticMethodID(jclassBridge, "requestForBoundMsgPool", "(JII)V");
	if (jmethodID_requestForBoundMsgPool == NULL) {
		bridge_print_error("C++ java Bridge.requestForBoundMsgPool() callback method was NOT found");
		return JNI_ERR;
	}

	// setup log collection from AccelIO into JXIO's logging
	logs_from_xio_set_threshold(g_log_threshold);
	logs_from_xio_callback_register();
	xio_init();

	// disable Accelio's HugeTbl memory allocation scheme
	int opt = 1;
	if (xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_DISABLE_HUGETBL, &opt, sizeof(opt))) {
		bridge_print_error("failed to disable AccelIO's HugeTbl memory allocation scheme");
	}

	opt = 0;
	int optlen = sizeof(opt);
	// check for RDMA support and if true, do some AccelIO option tuning
	if (!xio_get_opt(NULL, XIO_OPTLEVEL_RDMA, XIO_OPTNAME_RDMA_NUM_DEVICES, &opt, &optlen) && (optlen <= 4) && (opt > 0)) {
		LOG_DBG("Found %d RDMA devices", opt);

#ifdef ACCELIO_IB_FORK_SUPPORT // Disbale ibv_fork_init()
		// prepare IB resources to support fork-ing (prevent COW for the RDMA registered memroy)
		opt = 1;
		if (xio_set_opt(NULL, XIO_OPTLEVEL_RDMA, XIO_OPTNAME_ENABLE_FORK_INIT, &opt, sizeof(opt))) {
			bridge_print_error("failed to enable AccelIO's fork init option");
		}
#endif
		// disable Accelio's internal mem pool
		// JXIO requiers Java user application to allocate our memory pool
		opt = 0;
		if (xio_set_opt(NULL, XIO_OPTLEVEL_RDMA, XIO_OPTNAME_ENABLE_MEM_POOL, &opt, sizeof(opt))) {
			bridge_print_error("failed to disable AccelIO's internal memory pool buffers");
		}

		// force AccelIO to work in RDMA mode for all data traffic based on JXIO's buffer pool
		opt = 512;
		if (xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_MAX_INLINE_XIO_DATA, &opt, sizeof(opt))) {
			bridge_print_error("failed to change Accelio's RDMA_BUF_THRESHOLD");
		}

		opt = 0;
		if (xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_MAX_INLINE_XIO_HEADER, &opt, sizeof(opt))) {
			bridge_print_error("failed to change Accelio's RDMA_BUF_THRESHOLD");
		}
	}
	else {
		LOG_DBG("No RDMA devices available");
	}

	BULLSEYE_EXCLUDE_BLOCK_END

	LOG_DBG("Version: %s", get_version());
	LOG_DBG("JXIO/c/Bridge & AccelIO ready, java callback methods were found and cached");

	return JNI_VERSION_1_4;  //direct buffer requires java 1.4
}

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void* reserved)
{
	logs_from_xio_callback_unregister();
	xio_shutdown();

	// NOTE: We never reached this place
	static bool alreadyCalled = false;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (alreadyCalled) return;
	alreadyCalled = true;

	JNIEnv *env;
	if (cached_jvm->GetEnv((void **)&env, JNI_VERSION_1_4)) {
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	if (jweakBridge != NULL) {
		env->DeleteWeakGlobalRef(jweakBridge);
		jweakBridge = NULL;
		LOG_DBG("after env->DeleteWeakGlobalRef(jweaBridge)");
	}
	return;
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif

void Bridge_invoke_logToJava_callback(const int severity, const char* log_message)
{
	JNIEnv *env;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (cached_jvm->GetEnv((void **) &env, JNI_VERSION_1_4)) {
		fprintf(stderr, "-->> Warning: unable to get JNIEnv when trying to log message: '%s'\n", log_message);
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	jstring j_message = env->NewStringUTF(log_message);
	env->CallStaticVoidMethod(jclassBridge, jmethodID_logToJava, j_message, severity);
	env->DeleteLocalRef(j_message);
}

void Bridge_invoke_requestForBoundMsgPool_callback(Context* ctx, int inSize, int outSize)
{
	JNIEnv *env;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (cached_jvm->GetEnv((void **) &env, JNI_VERSION_1_4)) {
		printf("-->> Error getting JNIEnv In C++ Bridge_invoke_requestForBoundMsgPool when trying to request for more buffers\n");
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	long ptrEQH = (jlong) (intptr_t) ctx;
	env->CallStaticLongMethod(jclassBridge, jmethodID_requestForBoundMsgPool, ptrEQH, inSize, outSize);
}

extern "C" JNIEXPORT jstring JNICALL Java_org_accelio_jxio_impl_Bridge_getVersionNative(JNIEnv *env, jclass cls)
{
	return env->NewStringUTF(get_version());
}

extern "C" JNIEXPORT jstring JNICALL Java_org_accelio_jxio_impl_Bridge_getVersionAccelIONative(JNIEnv *env, jclass cls)
{
	return env->NewStringUTF(get_version_xio());
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_setLogLevelNative(JNIEnv *env, jclass cls, jint logLevel)
{
	log_set_threshold((log_severity_t)logLevel);
	logs_from_xio_set_threshold(g_log_threshold);
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_createCtxNative(JNIEnv *env, jclass cls, jint eventQueueSize, jobject dataToC)
{
	int size = eventQueueSize;
	Context *ctx = NULL;
	jobject jbuf = NULL;
	BULLSEYE_EXCLUDE_BLOCK_START
	try {
		ctx = new Context(size);
		jbuf = env->NewDirectByteBuffer(ctx->get_buffer_raw(), eventQueueSize);
	} catch (std::exception e) {
		LOG_ERR("failure on new Context()");
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	// coverity[noescape] - suppress 'ctx' false positive RESOURCE_LEAK error
	env->SetLongField(dataToC, fidPtr, (jlong)(intptr_t)ctx);
	env->SetObjectField(dataToC, fidBuf, jbuf);
	return;
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_closeCtxNative(JNIEnv *env, jclass cls, jlong ptrCtx)
{
	Context *ctx = (Context *)ptrCtx;
	delete (ctx);
	LOG_DBG("end of closeCTX");
}

extern "C" JNIEXPORT jint JNICALL Java_org_accelio_jxio_impl_Bridge_runEventLoopNative(JNIEnv *env, jclass cls, jlong ptrCtx, jlong timeOutMicroSec)
{
	Context *ctx = (Context *)ptrCtx;
	return ctx->run_event_loop((long)timeOutMicroSec);
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_breakEventLoopNative(JNIEnv *env, jclass cls, jlong ptrCtx)
{
	Context *ctx = (Context *)ptrCtx;
	ctx->break_event_loop();
}

extern "C" JNIEXPORT jlong JNICALL Java_org_accelio_jxio_impl_Bridge_startSessionClientNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptrCtx)
{
	const char *url = env->GetStringUTFChars(jurl, NULL);
	Client* client = NULL;
	BULLSEYE_EXCLUDE_BLOCK_START
	try {
		client = new Client(url, ptrCtx);
	} catch (std::exception e) {
		LOG_ERR("failure on new Client(ctx=%p, url=%s)", ptrCtx, url);
	}
	env->ReleaseStringUTFChars(jurl, url);
	BULLSEYE_EXCLUDE_BLOCK_END
	return (jlong)(intptr_t)client;
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_connectSessionClientNative(JNIEnv *env, jclass cls, jlong ptrSes)
{
	Client *ses = (Client*)ptrSes;
	return ses->create_connection();
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_closeSessionClientNative(JNIEnv *env, jclass cls, jlong ptrSes)
{
	Client *ses = (Client*)ptrSes;
	return ses->close_connection();
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_deleteClientNative(JNIEnv *env, jclass cls, jlong ptrSes)
{
	Client *ses = (Client*)ptrSes;
	LOG_DBG("deleting client");
	delete (ses);
	return true;
}

extern "C" JNIEXPORT jlongArray JNICALL Java_org_accelio_jxio_impl_Bridge_startServerPortalNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptrCtx)
{
	jlong temp[2];

	jlongArray dataToJava = env->NewLongArray(2);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (dataToJava == NULL) {
		return NULL; /* out of memory error thrown */
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	const char *url = env->GetStringUTFChars(jurl, NULL);

	ServerPortal *server = NULL;
	BULLSEYE_EXCLUDE_BLOCK_START
	try {
		server = new ServerPortal(url, ptrCtx);
	} catch (std::exception e) {
		LOG_ERR("failure on new ServerPortal(ctx=%p, url=%s)", ptrCtx, url);
		env->ReleaseStringUTFChars(jurl, url);
		return NULL;
	}
	env->ReleaseStringUTFChars(jurl, url);
	temp[0] = (jlong)(intptr_t)server;
	temp[1] = (jlong)server->port;
	env->SetLongArrayRegion(dataToJava,0, 2, temp);
	return dataToJava;
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_stopServerPortalNative(JNIEnv *env, jclass cls, jlong ptrServer)
{
	ServerPortal *server = (ServerPortal *)ptrServer;
	server->is_closing = true;
	if (server->sessions == 0) {
		LOG_DBG("there aren't any sessions on server %p. Can close", server);
		server->scheduleWriteEventAndDelete();
	} else {
		LOG_DBG("there are %d sessions on server %p", server->sessions, server);
	}
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_closeSessionServerNative(JNIEnv *env, jclass cls, jlong ptr_ses_server)
{
	ServerSession *jxio_session = (ServerSession*) ptr_ses_server;
	if (!jxio_session) {
		LOG_DBG("ERROR. closing empty session");
		return;
	}
	if (jxio_session->get_is_closing()) {
		LOG_DBG("trying to close session while already closing");
		return;
	}
	close_xio_connection(jxio_session);
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_forwardSessionNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptr_session, jlong ptr_portal)
{
	const char *url = env->GetStringUTFChars(jurl, NULL);
	ServerSession* jxio_session = (ServerSession*)ptr_session;
	struct xio_session *xio_session = jxio_session->get_xio_session();
	ServerPortal * portal = (ServerPortal *) ptr_portal;
	jxio_session->set_portal(portal);
	LOG_DBG("forwarding xio session=%p to portal=%p, whose url=%s", xio_session, portal, url);

	bool ret_val = forward_session(jxio_session, url);
	env->ReleaseStringUTFChars(jurl, url);
	if (ret_val)
		portal->sessions++;
	return ret_val;
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_acceptSessionNative(JNIEnv *env, jclass cls, jlong ptr_session, jlong ptr_portal)
{
	ServerSession* jxio_session = (ServerSession*)ptr_session;
	struct xio_session *xio_session = jxio_session->get_xio_session();
	ServerPortal * portal = (ServerPortal *) ptr_portal;

	LOG_DBG("accepting xio session=%p to portal=%p", xio_session, portal);

	bool ret_val = accept_session(jxio_session);
	if (ret_val)
		portal->sessions++;
	return ret_val;
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_rejectSessionNative(JNIEnv *env, jclass cls, jlong ptr_session, jint reason, jstring jdata, jint length)
{
	ServerSession* jxio_session = (ServerSession*)ptr_session;

	const char *data = env->GetStringUTFChars(jdata, NULL);
	char * data_copy = (char*)malloc (length + 1);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (data_copy == NULL) {
		LOG_ERR("memory allocation failed");
		delete jxio_session;
		env->ReleaseStringUTFChars(jdata, data);
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	strcpy(data_copy, data);
	bool retVal = reject_session(jxio_session, reason, data_copy, length);

	env->ReleaseStringUTFChars(jdata, data);
	free (data_copy);
	return retVal;
}

extern "C" JNIEXPORT jobject JNICALL Java_org_accelio_jxio_impl_Bridge_createMsgPoolNative(JNIEnv *env, jclass cls, jint msg_num, jint in_size, jint out_size, jlongArray ptr)
{
	jlong temp[msg_num+1];
	MsgPool* pool = NULL;
	BULLSEYE_EXCLUDE_BLOCK_START
	try {
		pool = new MsgPool(msg_num, in_size, out_size);
	} catch (std::exception e) {
		LOG_ERR("failure on new MsgPool(msg_num+%d, in_size=%d, out_size=%d)", msg_num, in_size, out_size);
		return NULL;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	jobject jbuf = env->NewDirectByteBuffer(pool->buf, pool->buf_size);

	//TODO: go over the actual data structure and not msg_ptrs
	temp [0] = (jlong)(intptr_t) pool; //the first element in array represents ptr to MsgPool
	for (int i=1; i<=msg_num; i++) {
		temp[i] = (jlong)(intptr_t) pool->msg_ptrs[i-1];
	}

	env->SetLongArrayRegion(ptr,0, msg_num+1, temp);
	return jbuf;
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_deleteMsgPoolNative(JNIEnv *env, jclass cls, jlongArray ptr_msg_pool)
{
	MsgPool * pool = (MsgPool*)ptr_msg_pool;
	delete pool;
}

extern "C" JNIEXPORT jint JNICALL Java_org_accelio_jxio_impl_Bridge_serverSendResponseNative(JNIEnv *env, jclass cls, jlong ptr_msg, jint size, jlong ptr_ses_server)
{
	ServerSession *ses = (ServerSession*) ptr_ses_server;
	Msg * msg = (Msg*) ptr_msg;
	if (ses->get_is_closing()) {
		LOG_DBG("trying to send message while session %p is closing", ses->get_xio_session());
		return XIO_E_SESSION_DISCONNECTED;
	}
	int ret = msg->send_response(size);
	return -ret; //ret value of send_response is negative error (while '-0' == '0')
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_discardRequestNative(JNIEnv *env, jclass cls, jlong ptr_msg)
{
	Msg * msg = (Msg*) ptr_msg;
	bool status = true;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_send_response(msg->get_xio_msg())) {
		LOG_DBG("Got error from releasing xio_msg: '%s' (%d)", xio_strerror(xio_errno()), xio_errno());
		status = false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	return status;
}

extern "C" JNIEXPORT jint JNICALL Java_org_accelio_jxio_impl_Bridge_clientSendReqNative(JNIEnv *env, jclass cls, jlong ptr_session, jlong ptr_msg, jint out_size, jint in_size, jboolean is_mirror)
{
	Msg * msg = (Msg*) ptr_msg;
	Client * client = (Client*)ptr_session;
	return client->send_msg(msg, out_size, in_size, is_mirror);
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_releaseMsgServerSideNative(JNIEnv *env, jclass cls, long ptr_msg)
{
	Msg * msg = (Msg*) ptr_msg;
	msg->release_to_pool();
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_accelio_jxio_impl_Bridge_bindMsgPoolNative(JNIEnv *env, jclass cls, jlong ptr_msg_pool, jlong ptr_ctx)
{
	Context * ctx = (Context*) ptr_ctx;
	MsgPool * pool = (MsgPool*)ptr_msg_pool;
	ctx->add_msg_pool(pool);
	return true;
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_destroyConnectionSessionServerNative(JNIEnv *env, jclass cls, jlong ptr_ses_server)
{
	ServerSession * ses = (ServerSession*) ptr_ses_server;
	xio_connection* con = ses->get_xio_connection();
	LOG_DBG("destroying connection %p for session %p", con, ses->get_xio_session());
	BULLSEYE_EXCLUDE_BLOCK_START
	if (xio_connection_destroy(con)) {
		LOG_ERR("error destroying connection %p for session %p", con, ses->get_xio_session());
	}
	BULLSEYE_EXCLUDE_BLOCK_END
}

extern "C" JNIEXPORT void JNICALL Java_org_accelio_jxio_impl_Bridge_deleteSessionServerNative(JNIEnv *env, jclass cls, jlong ptr_ses_server)
{
	ServerSession * ses = (ServerSession*) ptr_ses_server;
	LOG_DBG("delete jxio session %p", ses);
	delete ses;
}
