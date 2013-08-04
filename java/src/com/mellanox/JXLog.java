package com.mellanox;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class JXLog extends Logger{
		
		private String file_name = "JXLog.txt"; 
		FileHandler fileHandler;
	
		private JXLog(String name, String resourceBundleName) {
			super(name, resourceBundleName);
			try {
				fileHandler = new FileHandler(file_name);
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.addHandler(fileHandler);
	}

		public static JXLog getLog(String class_name){
			return new JXLog(class_name, null);
			}

}	



