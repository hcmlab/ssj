/*
 * FileUtils.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.file;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;

/**
 * Allows to download files from a valid URL and saves them in a predetermined folder on
 * the SD card.
 *
 * @author Vitaly
 */
public class FileUtils
{
	/**
	 * @param dirPath Directory
	 * @param fileName Filename
	 * @return File
	 * @throws IOException IO Exception
	 */
	public static File getFile(String dirPath, String fileName) throws IOException
	{
		boolean isURL = dirPath.startsWith("http://") || dirPath.startsWith("https://");

		if (isURL)
		{
			Pipeline.getInstance().download(fileName, dirPath, FileCons.DOWNLOAD_DIR, true);
			dirPath = FileCons.DOWNLOAD_DIR;
		}

		if (dirPath == null)
		{
			Log.w("file path not set, setting to default " + FileCons.SSJ_EXTERNAL_STORAGE);
			dirPath = FileCons.SSJ_EXTERNAL_STORAGE;
		}
		File fileDirectory = new File(dirPath);
		if (fileName == null)
		{
			Log.e("file name not set");
			return null;
		}
		return new File(fileDirectory, fileName);
	}

	/**
	 * Copy file from source to target
	 *
	 * @param filename File name
	 * @param from Source directory
	 * @param to Target directory
	 * @throws IOException IO Exception
	 */
	public static void copyFile(String filename, String from, String to) throws IOException
	{
		InputStream in = new FileInputStream(new File(from, filename));

		File dir = new File(to);
		if (!dir.exists())
			dir.mkdirs();
		OutputStream out = new FileOutputStream(new File(dir, filename));

		copyFile(in, out);
	}

	/**
	 * Copy input stream to output stream
	 *
	 * @param in Input stream
	 * @param out Output stream
	 * @throws IOException IO Exception
	 */
	public static void copyFile(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1)
		{
			out.write(buffer, 0, read);
		}
	}

	/**
	 * Copies an asset to a target location
	 *
	 * @param context Context
	 * @param srcPath Source path
	 * @param targetPath Target path
	 */
	public static void copyAsset(Context context, String srcPath, String targetPath)
	{
		try
		{
			InputStream in = context.getAssets().open(srcPath);

			File targetFile = new File(targetPath);

			if(!targetFile.exists())
			{
				new File(targetFile.getParent()).mkdirs();
			}

			OutputStream out = new FileOutputStream(targetFile);

			// Copy files
			copyFile(in, out);

			// Close streams
			in.close();
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			Log.e("Failed to copy asset " + srcPath, e);
		}
	}


	/**
	 * Returns the extension of a given file.
	 * @param file File to identify.
	 * @return Extension of the file without the preceding dot.
	 * Example: example-file.stream: "stream"
	 */
	public static String getFileType(File file)
	{
		String filename = file.getName();
		int dotPosition = filename.lastIndexOf('.');
		return filename.substring(dotPosition + 1);
	}
}
