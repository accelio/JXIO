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
package org.accelio.jxio.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class LoadLibrary {

	/**
	 * Tries to load the library, by extracting it from the current class jar. If that fails, falls back on
	 * {@link System#loadLibrary(String)}.
	 * 
	 * @param String to the library name to be extracted and loaded from the this current jar
	 * @return true if the the file loaded successfully, or false on error
	 */
	public static boolean loadLibrary(String resourceName) {
		ClassLoader loader = LoadLibrary.class.getClassLoader();
		URL url = loader.getResource(resourceName);
		File file = null;
		try {
			file = extractResource(url);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("JXIO: Native code library failed to extract URL\n" + e);
			return false;
		}

		if (file != null && file.exists()) {
			String filename = file.getAbsolutePath();
			try {
				System.load(filename);
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
				System.err.println("JXIO: Native code library failed to load\n" + e);
				return false;
			}
			if (file.delete() == false) {
				System.err.println("JXIO: Error deleting the temp native code library (filename='" + filename + "')\n");
			}
		}
		return true;
	}

	/**
	 * Extracts a resource into a temp directory.
	 * 
	 * @param resourceURL the URL of the resource to extract
	 * @return the File object representing the extracted file
	 * @throws IOException if fails to extract resource properly
	 */
	public static File extractResource(URL resourceURL) throws IOException {
		InputStream is = resourceURL.openStream();
		if (is == null) {
			return null;
		}
		File file = new File(getTempDir(), new File(resourceURL.getPath()).getName());
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(file);
			copy(is, os);
		} finally {
			closeQuietly(os);
			closeQuietly(is);
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

	/**
	 * Helper function to copy an InputStream into an OutputStream
	 */
	public static long copy(InputStream is, OutputStream os) throws IOException {
		if (is == null || os == null)
			return 0;
		long length_total = 0;
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = is.read(buffer)) != -1) {
			os.write(buffer, 0, length);
			length_total += length;
		}
		return length_total;
	}
	
	/**
	 * Helper function to close InputStream or OutputStream in a quiet way which hides the exceptions
	 */
	public static void closeQuietly(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (IOException e) {
			// No logging in this 'Quite Close' method
			// System.err.println("JXIO: Failed to close '" + c + "'\n");
		}
	}
}
