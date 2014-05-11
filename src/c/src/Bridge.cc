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

#include <infiniband/verbs.h>
#include <libxio.h>

#include "bullseye.h"
#include "CallbackFunctions.h"
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

// JNI inner functions implementations
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void* reserved)
{
	//printf("in JXIO/c/Bridge - JNI_OnLoad\n");

	cached_jvm = jvm;
	JNIEnv *env;

	// direct buffer requires java 1.4
	BULLSEYE_EXCLUDE_BLOCK_START
	if (jvm->GetEnv((void **)&env, JNI_VERSION_1_4)) { 
		bridge_print_error("JNI version 1.4 and higher requiered");
		return JNI_ERR;
	}

	cls = env->FindClass("com/mellanox/jxio/impl/Bridge");
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

	cls_data = env->FindClass("com/mellanox/jxio/EventQueueHandler$DataFromC");
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

	// prepare IB resources to support fork-ing (prevent COW for the RDMA registered memroy)
	if (ibv_fork_init()) {
		bridge_print_error("failed in ibv_fork_init()");
	}

	// setup log collection from AccelIO into JXIO's logging
	logs_from_xio_set_threshold(g_log_threshold);
	logs_from_xio_callback_register();

	// disable Accelio's HugeTbl memory allocation scheme
	int opt = 1;
	if (xio_set_opt(NULL, XIO_OPTLEVEL_ACCELIO, XIO_OPTNAME_DISABLE_HUGETBL, &opt, sizeof(opt))) {
		bridge_print_error("failed to disable AccelIO's HugeTbl memory allocation scheme");
	}

	// disable Accelio's internal mem pool
	// JXIO requiers Java user application to allocate our memory pool
	opt = 0;
	if (xio_set_opt(NULL, XIO_OPTLEVEL_RDMA, XIO_OPTNAME_ENABLE_MEM_POOL, &opt, sizeof(opt))) {
		bridge_print_error("failed to disable AccelIO's internal memory pool buffers");
	}

	opt = 512;
	if (xio_set_opt(NULL, XIO_OPTLEVEL_RDMA, XIO_OPTNAME_RDMA_BUF_THRESHOLD, &opt, sizeof(opt))) {
		bridge_print_error("failed to change Accelio's RDMA_BUF_THRESHOLD");
	}

	BULLSEYE_EXCLUDE_BLOCK_END

	LOG_DBG("Version: %s", GIT_VERSION);
	LOG_DBG("JXIO/c/Bridge & AccelIO ready, java callback methods were found and cached");

	return JNI_VERSION_1_4;  //direct buffer requires java 1.4
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void* reserved)
{
	logs_from_xio_callback_unregister();

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

void Bridge_invoke_logToJava_callback(const int severity, const char* log_message)
{
	JNIEnv *env;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (cached_jvm->GetEnv((void **) &env, JNI_VERSION_1_4)) {
		fprintf(stderr, "-->> Error getting JNIEnv when trying to log message: '%s'\n", log_message);
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	jstring j_message = env->NewStringUTF(log_message);
	env->CallStaticVoidMethod(jclassBridge, jmethodID_logToJava, j_message, severity);
	env->DeleteLocalRef(j_message);
}

void Bridge_invoke_requestForBoundMsgPool_callback (Context* ctx, int inSize, int outSize)
{
	JNIEnv *env;
	BULLSEYE_EXCLUDE_BLOCK_START
	if (cached_jvm->GetEnv((void **) &env, JNI_VERSION_1_4)) {
		printf("-->> Error getting JNIEnv In C++ Bridge_invoke_requestForBoundMsgPool when trying to request for more buffers\n");
		return;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	long ptrEQH = (jlong)(intptr_t)ctx;
	env->CallStaticLongMethod(jclassBridge, jmethodID_requestForBoundMsgPool, ptrEQH, inSize, outSize);
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_setLogLevelNative(JNIEnv *env, jclass cls, jint logLevel)
{
	log_set_threshold((log_severity_t)logLevel);
	logs_from_xio_set_threshold(g_log_threshold);
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_createCtxNative(JNIEnv *env, jclass cls, jint eventQueueSize, jobject dataToC)
{
	int size = eventQueueSize;
	Context *ctx = new Context (size);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (ctx == NULL) {
		LOG_ERR("memory allocation failed");
		return true;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	if (ctx->error_creating) {
		delete (ctx);
		return true;
	}
	jobject jbuf = env->NewDirectByteBuffer(ctx->event_queue->get_buffer(), eventQueueSize );

	jlong ptr = (jlong)(intptr_t) ctx;

	env->SetLongField(dataToC, fidPtr, ptr);
	env->SetObjectField(dataToC, fidBuf, jbuf);

	return ctx->error_creating;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_closeCtxNative(JNIEnv *env, jclass cls, jlong ptrCtx)
{
	Context *ctx = (Context *)ptrCtx;
	delete (ctx);
	LOG_DBG("end of closeCTX");
}

extern "C" JNIEXPORT jintArray JNICALL Java_com_mellanox_jxio_impl_Bridge_runEventLoopNative(JNIEnv *env, jclass cls, jlong ptrCtx, jlong timeOutMicroSec)
{
	jint temp [2];
	Context *ctx = (Context *)ptrCtx;
	jintArray dataToJava = env->NewIntArray(2);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (dataToJava == NULL) {
		return NULL; /* out of memory error thrown */
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	temp[0] = ctx->run_event_loop((long)timeOutMicroSec);
	temp[1] = ctx->offset_read_for_java;
	ctx->reset_counters();
	env->SetIntArrayRegion(dataToJava,0, 2, temp);
	return dataToJava;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_breakEventLoopNative(JNIEnv *env, jclass cls, jlong ptrCtx)
{
	Context *ctx = (Context *)ptrCtx;
	ctx->break_event_loop(0); // not sure about thread context so play it safe and 'self_thread = false'
}

extern "C" JNIEXPORT jlong JNICALL Java_com_mellanox_jxio_impl_Bridge_startSessionClientNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptrCtx)
{
	if (ptrCtx == 0) {
		LOG_ERR("eqh does not exist");
		return 0;
	}
	const char *url = env->GetStringUTFChars(jurl, NULL);
	Client * ses = new Client(url, ptrCtx);
	env->ReleaseStringUTFChars(jurl, url);

	BULLSEYE_EXCLUDE_BLOCK_START
	if (ses == NULL) {
		LOG_ERR("memory allocation failed");
		return 0;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	if (ses->error_creating) {
		delete (ses);
		return 0;
	}
	return (jlong)(intptr_t) ses;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_closeSessionClientNative(JNIEnv *env, jclass cls, jlong ptrSes)
{
	Client *ses = (Client*)ptrSes;
	return ses->close_connection();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_deleteClientNative(JNIEnv *env, jclass cls, jlong ptrSes)
{
	Client *ses = (Client*)ptrSes;
	delete (ses);
	return true;
}

extern "C" JNIEXPORT jlongArray JNICALL Java_com_mellanox_jxio_impl_Bridge_startServerPortalNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptrCtx)
{
	jlong temp[2];

	jlongArray dataToJava = env->NewLongArray(2);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (dataToJava == NULL) {
		return NULL; /* out of memory error thrown */
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	const char *url = env->GetStringUTFChars(jurl, NULL);

	ServerPortal *server = new ServerPortal(url, ptrCtx);
	env->ReleaseStringUTFChars(jurl, url);

	BULLSEYE_EXCLUDE_BLOCK_START
	if (server == NULL) {
		LOG_ERR("memory allocation failed");
		return NULL;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	if (server->error_creating) {
		temp[0] = 0;
		temp[1] = 0;
		delete(server);
	} else {
		temp[0] = (jlong)(intptr_t) server;
		temp[1] = (jlong)server->port;
	}

	env->SetLongArrayRegion(dataToJava,0, 2, temp);
	return dataToJava;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_stopServerPortalNative(JNIEnv *env, jclass cls, jlong ptrServer)
{
	ServerPortal *server = (ServerPortal *)ptrServer;
	server->is_closing = true;
	if (server->sessions == 0) {
		LOG_DBG("there aren't any sessions on this server. Can close");
		server->writeEventAndDelete(false);
	} else {
		LOG_DBG("there are more sessions");
	}
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_closeSessionServerNative(JNIEnv *env, jclass cls, jlong ptr_ses_server)
{
	ServerSession *jxio_session = (ServerSession*) ptr_ses_server;
	if (jxio_session->get_is_closing()) {
		LOG_DBG("trying to close session while already closing");
		return;
	}
	struct xio_session *xio_session = jxio_session->get_xio_session();
	Context * context = jxio_session->getCtx();
	close_xio_connection(xio_session, context->ctx);
}

extern "C" JNIEXPORT jlong JNICALL Java_com_mellanox_jxio_impl_Bridge_forwardSessionNative(JNIEnv *env, jclass cls, jstring jurl, jlong ptr_session, jlong ptr_portal, jlong ptr_portal_forwarder)
{
	const char *url = env->GetStringUTFChars(jurl, NULL);
	struct xio_session *xio_session = (struct xio_session *)ptr_session;
	ServerPortal * portal = (ServerPortal *) ptr_portal;
	ServerPortal * portal_forwarder = (ServerPortal *) ptr_portal_forwarder;
	Context * ctx = portal->get_ctx_class();
	ServerSession *jxio_session = new ServerSession(xio_session, ctx);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (jxio_session == NULL) {
		LOG_ERR("memory allocation failed");
		return 0;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	bool ret_val = forward_session(jxio_session, url);
	env->ReleaseStringUTFChars(jurl, url);
	if (ret_val) {
		portal_forwarder->sessions++;
		return (jlong)(intptr_t) jxio_session;
	} else {
		return 0;
	}
}

extern "C" JNIEXPORT jlong JNICALL Java_com_mellanox_jxio_impl_Bridge_acceptSessionNative(JNIEnv *env, jclass cls, jlong ptr_session, jlong ptr_portal)
{
	struct xio_session *xio_session = (struct xio_session *)ptr_session;
	ServerPortal * portal = (ServerPortal *) ptr_portal;
	Context * ctx = portal->get_ctx_class();
	ServerSession *jxio_session = new ServerSession(xio_session, ctx);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (jxio_session == NULL) {
		LOG_ERR("memory allocation failed");
		return 0;
	}
	BULLSEYE_EXCLUDE_BLOCK_END

	bool ret_val = accept_session(jxio_session);
	if (ret_val) {
		portal->sessions++;
		return (jlong)(intptr_t) jxio_session;
	} else {
		return 0;
	}
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_rejectSessionNative(JNIEnv *env, jclass cls, jlong ptr_session, jint reason, jstring jdata, jint length)
{
	struct xio_session *xio_session = (struct xio_session *)ptr_session;

	const char *data = env->GetStringUTFChars(jdata, NULL);
	ServerSession *jxio_session = new ServerSession(xio_session, NULL);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (jxio_session == NULL) {
		LOG_ERR("memory allocation failed");
		return false;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
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

extern "C" JNIEXPORT jobject JNICALL Java_com_mellanox_jxio_impl_Bridge_createMsgPoolNative(JNIEnv *env, jclass cls, jint msg_num, jint in_size, jint out_size, jlongArray ptr)
{
	jlong temp[msg_num+1];
	MsgPool *pool = new MsgPool(msg_num, in_size, out_size);
	BULLSEYE_EXCLUDE_BLOCK_START
	if (pool == NULL) {
		LOG_ERR("memory allocation failed");
		return NULL;
	}
	BULLSEYE_EXCLUDE_BLOCK_END
	if (pool->error_creating) {
		LOG_ERR("there was an error creating MsgPool");
		delete (pool);
		return NULL;
	}
	jobject jbuf = env->NewDirectByteBuffer(pool->buf, pool->buf_size);

	//TODO: go over the actual data structure and not msg_ptrs
	temp [0] = (jlong)(intptr_t) pool;//the first element in array represents ptr to MsgPool
	for (int i=1; i<=msg_num; i++) {
		temp[i] = (jlong)(intptr_t) pool->msg_ptrs[i-1];
	}

	env->SetLongArrayRegion(ptr,0, msg_num+1, temp);
	return jbuf;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_deleteMsgPoolNative(JNIEnv *env, jclass cls, jlongArray ptr_msg_pool)
{
	MsgPool * pool = (MsgPool*)ptr_msg_pool;
	delete pool;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_serverSendResponseNative(JNIEnv *env, jclass cls, jlong ptr_msg, jint size, jlong ptr_ses_server)
{
	ServerSession *ses = (ServerSession*) ptr_ses_server;
	Msg * msg = (Msg*) ptr_msg;
	if (ses->get_is_closing()) {
		LOG_DBG("trying to send message while session is closing. Releasing msg back to pool");
		msg->release_to_pool();
		return false;
	}
	return msg->send_response(size);
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_clientSendReqNative(JNIEnv *env, jclass cls, jlong ptr_session, jlong ptr_msg, jint size, jboolean is_mirror)
{
	Msg * msg = (Msg*) ptr_msg;
	Client * client = (Client*)ptr_session;
	return client->send_msg(msg, size, is_mirror);
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_releaseMsgServerSideNative(JNIEnv *env, jclass cls, long ptr_msg)
{
	Msg * msg = (Msg*) ptr_msg;
	msg->release_to_pool();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_mellanox_jxio_impl_Bridge_bindMsgPoolNative(JNIEnv *env, jclass cls, jlong ptr_msg_pool, jlong ptr_ctx)
{
	Context * ctx = (Context*) ptr_ctx;
	MsgPool * pool = (MsgPool*)ptr_msg_pool;
	ctx->add_msg_pool(pool);
	return true;
}

extern "C" JNIEXPORT void JNICALL Java_com_mellanox_jxio_impl_Bridge_deleteSessionServerNative(JNIEnv *env, jclass cls, jlong ptr_ses_server)
{
	ServerSession * ses = (ServerSession*) ptr_ses_server;
	delete (ses);
}

#if _BullseyeCoverage
    #pragma BullseyeCoverage off
#endif
extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_jxio_impl_Bridge_addEventLoopFdNative(JNIEnv *env, jclass cls, jlong ptrCtx, jint fd, jint events, jlong priv_data)
{
	Context *ctx = (Context *)ptrCtx;
	return ctx->add_event_loop_fd(fd, events, (void*)priv_data);
}

extern "C" JNIEXPORT jint JNICALL Java_com_mellanox_jxio_impl_Bridge_delEventLoopFdNative(JNIEnv *env, jclass cls, jlong ptrCtx, jint fd)
{
	Context *ctx = (Context *)ptrCtx;
	return ctx->del_event_loop_fd(fd);
}
#if _BullseyeCoverage
    #pragma BullseyeCoverage on
#endif
