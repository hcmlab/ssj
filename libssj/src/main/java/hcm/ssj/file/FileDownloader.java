/*
 * FileDownloader.java
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
import java.util.LinkedList;
import java.util.Locale;

import hcm.ssj.core.Log;

/**
 * Created by Ionut Damian on 08.09.2017.
 */

public class FileDownloader extends Thread
{
	private static final int BUFFER_SIZE = 4096;
	private static final int BYTES_IN_MEGABYTE = 1000000;
	private static final int EOF = -1;
	private static final int LOG_STEP = 200;

	private boolean terminate = false;
	private LinkedList<Task> queue = new LinkedList<>();

	public class Task
	{
		String filename;
		String from;
		String to;
		Boolean result = false;
		final Object token = new Object();

		public Task(String filename, String from, String to)
		{
			this.filename = filename;
			this.from = from;
			this.to = to;
		}
	}

	@Override
	public void run()
	{
		while(!terminate)
		{
			if(!queue.isEmpty())
			{
				Task t = queue.removeFirst();

				try
				{
					t.result = downloadFile(t.filename, t.from, t.to);
				}
				catch (IOException e)
				{
					Log.e("Error while downloading file", e);
				}

				synchronized (t.token)
				{
					t.token.notify();
				}
			}
			else
			{
				//wait to see if something new comes
				try { Thread.sleep(3000); }
				catch (InterruptedException e){}

				//if not, terminate
				if(queue.isEmpty())
					terminate();
			}
		}
	}

	public Task addToQueue(String fileName, String from, String to)
	{
		if(isTerminating())
			return null;

		Task t = new Task(fileName, from, to);
		queue.addLast(t);
		return t;
	}

	public boolean wait(Task t)
	{
		try
		{
			synchronized (t.token)
			{
				t.token.wait();
			}
		}
		catch (InterruptedException e) {}

		return t.result;
	}

	/**
	 * Downloads file from a given URL and saves it on the SD card with a given file name.
	 *
	 * @param fileName Name of the file.
	 * @param from Folder URL where file is located.
	 * @param to Folder to where to download the file
	 * @return Instance of the downloaded file.
	 */
	private boolean downloadFile(String fileName, String from, String to) throws IOException
	{
		File destinationDir = new File(to);

		// Create folders on the SD card if not already created.
		destinationDir.mkdirs();

		File downloadedFile = new File(destinationDir.getAbsolutePath(), fileName);

		if (!downloadedFile.exists() || downloadedFile.length() == 0)
		{
			Log.i("Starting to download '" + fileName + "'...");
			URL fileURL = new URL(from + File.separator + fileName);

			InputStream input = fileURL.openStream();
			FileOutputStream output = new FileOutputStream(downloadedFile);

			byte[] buffer = new byte[BUFFER_SIZE];
			int numberOfBytesRead;
			int totalBytesDownloaded = 0;
			int counter = 0;

			while ((numberOfBytesRead = input.read(buffer)) != EOF)
			{
				if (terminate)
				{
					Log.i("Download interrupted.");
					downloadedFile.delete();
					return false;
				}

				output.write(buffer, 0, numberOfBytesRead);

				totalBytesDownloaded += numberOfBytesRead;

				if (counter % LOG_STEP == 0)
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
		return true;
	}

	public void terminate()
	{
		terminate = true;
	}

	public boolean isTerminating()
	{
		return terminate;
	}
}
