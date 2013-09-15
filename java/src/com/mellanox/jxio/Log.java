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
package com.mellanox.jxio;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log extends Logger{
		
		private String file_name = "Log.txt"; 
//		FileHandler handler;
		ConsoleHandler handler;
	
		private Log(String name, String resourceBundleName) {
			super(name, resourceBundleName);
			try {
//				handler = new FileHandler(file_name);
				handler = new ConsoleHandler();
				SimpleFormatter formatter = new SimpleFormatter();
				handler.setFormatter(formatter);
				this.addHandler(handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	}

		public static Log getLog(String class_name){
			return new Log(class_name, null);
			}

}	



