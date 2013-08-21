package com.mellanox;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class JXLog extends Logger{
		
		private String file_name = "JXLog.txt"; 
//		FileHandler handler;
		ConsoleHandler handler;
	
		private JXLog(String name, String resourceBundleName) {
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

		public static JXLog getLog(String class_name){
			return new JXLog(class_name, null);
			}

}	



