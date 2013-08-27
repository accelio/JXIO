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
package com.mellanox;


public class JXMsg {
	
	private int offset;
	private int msg_size;

	
	protected JXMsg(int _offset ,int _msg_size){
		offset = _offset;
		msg_size = _msg_size;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public int getSize(){
		return msg_size;
	}

}
