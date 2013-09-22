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


#ifndef Contexable__H___
#define Contexable__H___

class Context;

class Contexable {

public:
	Context* get_ctx_class(){return ctx_class;}
	void set_ctx_class(Context* c){this->ctx_class = c;}

	//returns true in case this event should be written to eventQueue and false if not
	virtual bool onSessionEvent(int eventType) = 0;

private:
	Context* ctx_class;
};




#endif // ! Contexable__H___
