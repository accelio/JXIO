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
package com.mellanox.jxio.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class LoadLibrary {

	/**
	 * Tries to load the library, by extracting it from the current class jar. If that fails, falls
	 * back on {@link System#loadLibrary(String)}.
	 * 
	 * @param String to the library name to be extracted and loaded from the jar
	 * @return the String of the full path of the file loaded, or null on error
	 */
	public static String loadLibrary(String resourceName) {
		ClassLoader loader = LoadLibrary.class.getClassLoader();
		URL url = loader.getResource(resourceName);
		try {
			File file = extractResource(url);
			if (file != null && file.exists()) {
				String filename = file.getAbsolutePath();

				System.load(filename);
				file.delete();
				return filename;
			}
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		String libname = url.getFile();
		System.loadLibrary(libname);
		return libname;
	}

	/**
	 * Extracts a resource into a temp directory. 
	 * 
	 * @param resourceURL the URL of the resource to extract
	 * @return the File object representing the extracted file
	 * @throws IOException if fails to extract resource properly
	 */
	public static File extractResource(URL resourceURL)
	        throws IOException {
		InputStream is = resourceURL.openStream();
		if (is == null) {
			return null;
		}
		File file = null;
		try {
			file = new File(getTempDir(), new File(resourceURL.getPath()).getName());
			FileOutputStream os = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int length = 0;
			while ((length = is.read(buffer)) != -1) {
				os.write(buffer, 0, length);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			throw e;
		}
		return file;
	}

	/** Temporary directory set and returned by {@link #getTempDir()}. */
	static File tempDir = null;

	/**
	 * Creates a unique name for {@link #tempDir} out of {@code System.getProperty("java.io.tmpdir")} and
	 * {@code System.nanoTime()}.
	 * 
	 * @return {@link #tempDir}
	 */
	public static File getTempDir() {
		if (tempDir == null) {
			File tmpdir = new File(System.getProperty("java.io.tmpdir"));
			File f = null;
			for (int i = 0; i < 1000; i++) {
				f = new File(tmpdir, "jxio" + System.nanoTime());
				if (f.mkdir()) {
					tempDir = f;
					tempDir.deleteOnExit();
					break;
				}
			}
		}
		return tempDir;
	}

}
