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

#include "ServerManager.h"

ServerManager::ServerManager(const char *url, long ptrCtx) {
	log(lsDEBUG, "inside startServerNative method\n");

	error_creating = false;

	struct xio_session_ops server_ops;
	memset(&server_ops, 0, sizeof(server_ops));
	server_ops.on_new_session = on_new_session_callback;

	struct xio_context *ctx;
	Context *ctxClass = (Context *) ptrCtx;
	set_ctx_class(ctxClass);

	this->server = xio_bind(ctxClass->ctx, &server_ops, url, &this->port, 0,
			this);

	if (this->server == NULL) {
		log(lsERROR, "Error in binding server\n");
		error_creating = true;
	}

	log(lsDEBUG, "****** port number is %d\n", this->port);
}

ServerManager::~ServerManager() {
	if (error_creating) {
		return;
	}

	if (xio_unbind(this->server)) {
		log(lsERROR, "Error xio_unbind failed\n");
	}
}

bool ServerManager::onSessionEvent(xio_session_event eventType)
{
	//dummy implementation: will not be called
	log(lsWARN, "UNHANDLED event: got '%s' event (%d). \n", xio_session_event_str(eventType), eventType);
	return true;
}
