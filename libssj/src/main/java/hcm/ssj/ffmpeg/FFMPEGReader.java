/*
 * FFMPEGReader.java
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

package hcm.ssj.ffmpeg;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.nio.ByteBuffer;

import hcm.ssj.core.Log;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 04.09.2017.
 */

public class FFMPEGReader extends Sensor
{
	/**
	 * All options for the FFMPEGReader
	 */
	public class Options extends OptionList
	{
		public final Option<String> url = new Option<>("url", "udp://127.0.0.1:5000", String.class, "Url (file path or streaming address, e.g. udp://<ip:port>)");
		public final Option<Integer> width = new Option<>("width", 640, Integer.class, "width in pixel");
		public final Option<Integer> height = new Option<>("height", 480, Integer.class, "height in pixel");
		public final Option<Double> fps = new Option<>("fps", 15., Double.class, "fps");

		/**
		 *
		 */
		private Options()
		{
			addOptions();
		}
	}

	// Options
	public final Options options = new Options();
	private FFmpegFrameGrabber reader;
	private Frame imageFrame;

	private volatile boolean reading;
	private Thread readingThread;
	private byte[] frameBuffer;

	public FFMPEGReader()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	protected boolean connect()
	{
		reading = true;

		frameBuffer = new byte[getBufferSize()];

		readingThread = new Thread(new ReaderThread());
		readingThread.start();

		return true;
	}

	@Override
	protected void disconnect()
	{
		reading = false;

		try
		{
			readingThread.join();
		}
		catch (InterruptedException e)
		{
			Log.e("Error while waiting for reading thread", e);
		}
	}

	public int getBufferSize()
	{
		int bufferSize = options.width.get() * options.height.get();

		// 3 bytes per pixel
		bufferSize *= 3;

		return bufferSize;
	}

	public void swapBuffer(byte[] bytes)
	{
		synchronized (frameBuffer)
		{
			// Get data from buffer
			if (bytes.length < frameBuffer.length)
			{
				Log.e("Buffer read changed from " + bytes.length + " to " + frameBuffer.length);
				bytes = new byte[frameBuffer.length];
			}
			System.arraycopy(frameBuffer, 0, bytes, 0, frameBuffer.length);
		}
	}

	private class ReaderThread implements Runnable
	{
		@Override
		public void run()
		{
			ByteBuffer buffer;

			try
			{
				reader = new FFmpegFrameGrabber(options.url.get());
				reader.setImageWidth(options.width.get());
				reader.setImageHeight(options.height.get());
				reader.setFrameRate(options.fps.get());
				reader.setPixelFormat(avutil.AV_PIX_FMT_RGB24);
				reader.start();

				while ((imageFrame = reader.grab()) != null && reading)
				{
					buffer = ((ByteBuffer) imageFrame.image[0].position(0));

					if (buffer.remaining() == getBufferSize())
					{
						synchronized (frameBuffer)
						{
							buffer.get(frameBuffer);
						}
					}
				}
			}
			catch (FrameGrabber.Exception e)
			{
				Log.e("Error while grabbing frames", e);
			}
			finally
			{
				try
				{
					if (reader != null)
					{
						reader.stop();
						reader.release();
					}
				}
				catch (FrameGrabber.Exception e)
				{
					Log.e("Error while closing reader", e);
				}
			}
		}
	}
}
