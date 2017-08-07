/*
 * FileDownloader.java
 * Copyright (c) 2017
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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
import java.io.InputStream;
import java.net.URL;

import hcm.ssj.core.Log;

/**
 * Allows to download files from a valid URL and saves them in a predetermined folder on
 * the SD card.
 *
 * @author Vitaly
 */
public class FileDownloader
{
	private static final int BUFFER_SIZE = 4096;
	private static final int EOF = -1;


	/**
	 * Downloads file from a given URL and saves it on the SD card with a given file name.
	 *
	 * @param location File URL.
	 * @param fileName Name of the file after downloading.
	 * @return Instance of the downloaded file.
	 */
	public static File downloadFile(String location, String fileName)
	{
		try
		{
			File destinationDir = new File(LoggingConstants.TENSORFLOW_MODELS_DIR);

			// Create folders on the SD card if not already created.
			destinationDir.mkdirs();

			File downloadedFile = new File(destinationDir.getAbsolutePath(), fileName);

			if (!downloadedFile.exists())
			{
				URL fileURL = new URL(location);

				InputStream input = fileURL.openStream();
				FileOutputStream output = new FileOutputStream(downloadedFile);

				byte[] buffer = new byte[BUFFER_SIZE];
				int numberOfBytesRead;

				while ((numberOfBytesRead = input.read(buffer)) != EOF)
				{
					output.write(buffer, 0, numberOfBytesRead);
				}

				input.close();
				output.close();

				Log.i("File '" + location + "' downloaded successfully.");
			}
			return downloadedFile;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error while downloading " + location, e);
		}
	}
}
