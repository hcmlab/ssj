/*
 * FileUtils.java
 * Copyright (c) 2017
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

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
	private static final int BUFFER_SIZE = 4096;
	private static final int BYTES_IN_MEGABYTE = 1000000;
	private static final int EOF = -1;

	/**
	 * @param filePath Option
	 * @param fileName Option
	 * @return File
	 */
	public static File getFile(String filePath, String fileName) throws IOException
	{
		boolean isURL = filePath.startsWith("http://") || filePath.startsWith("https://");

		if (isURL)
		{
			return FileUtils.downloadFile(filePath, fileName);
		}

		if (filePath == null)
		{
			Log.w("file path not set, setting to default " + LoggingConstants.SSJ_EXTERNAL_STORAGE);
			filePath = LoggingConstants.SSJ_EXTERNAL_STORAGE;
		}
		File fileDirectory = new File(filePath);
		if (fileName == null)
		{
			Log.e("file name not set");
			return null;
		}
		return new File(fileDirectory, fileName);
	}

	/**
	 * Downloads file from a given URL and saves it on the SD card with a given file name.
	 *
	 * @param location Folder URL where file is located.
	 * @param fileName Name of the file.
	 * @return Instance of the downloaded file.
	 */
	public static File downloadFile(String location, String fileName) throws IOException
	{
		File destinationDir = new File(LoggingConstants.DOWNLOAD_MODELS_DIR);

		// Create folders on the SD card if not already created.
		destinationDir.mkdirs();

		File downloadedFile = new File(destinationDir.getAbsolutePath(), fileName);

		if (!downloadedFile.exists())
		{
			Log.i("Starting to download '" + fileName + "'...");
			URL fileURL = new URL(location + File.separator + fileName);

			InputStream input = fileURL.openStream();
			FileOutputStream output = new FileOutputStream(downloadedFile);

			byte[] buffer = new byte[BUFFER_SIZE];
			int numberOfBytesRead;
			int totalBytesDownloaded = 0;
			int counter = 0;

			Pipeline pipe = Pipeline.getInstance();
			while ((numberOfBytesRead = input.read(buffer)) != EOF)
			{
				output.write(buffer, 0, numberOfBytesRead);

				totalBytesDownloaded += numberOfBytesRead;

				if (counter % 200 == 0)
				{
					String progress = String.format(Locale.US, "%.2f", (float)totalBytesDownloaded / (float)BYTES_IN_MEGABYTE);
					Log.i("File '" + fileName + "' " + progress + " Mb downloaded.");
				}

				counter++;
			}

			input.close();
			output.close();

			String progress = String.format(Locale.US, "%.2f", (float)totalBytesDownloaded / BYTES_IN_MEGABYTE);
			Log.i("File '" + fileName + "' " + progress + " Mb downloaded.");
			Log.i("File '" + fileName + "' downloaded successfully.");
		}
		return downloadedFile;
	}
}
