/*
 * ImageLoaderChannel.java
 * Copyright (c) 2019
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

package hcm.ssj.camera;

import android.os.SystemClock;

import java.nio.ByteBuffer;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 13.11.2019.
 */
public class ImageLoaderChannel extends SensorChannel
{
	public static final int IMAGE_CHANNELS = 3;

	public class Options extends OptionList
	{
		public final Option<Double> sampleRate = new Option<>("sampleRate", 15., Double.class, "sample rate for loaded image");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	private ImageLoaderSensor imageSensor;
	private int width = -1;
	private int height = -1;
	private int sampleDimension = 0;

	private ByteBuffer byteBuffer;
	byte[] byteArray;

	public ImageLoaderChannel()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	protected void init() throws SSJException
	{
		try
		{
			imageSensor = (ImageLoaderSensor) _sensor;
			imageSensor.loadImage();

			width = imageSensor.getImageWidth();
			height = imageSensor.getImageHeight();
			sampleDimension = width * height * IMAGE_CHANNELS;
		}
		catch (SSJFatalException e)
		{
			throw new SSJException(e);
		}
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		int size = imageSensor.image.getRowBytes() * imageSensor.image.getHeight();

		// Copy image to buffer
		byteBuffer = ByteBuffer.allocate(size);
		imageSensor.image.copyPixelsToBuffer(byteBuffer);

		// Contains rgba values in signed byte form [-127, 127], needs to be converted to [0, 255]
		byteArray = byteBuffer.array();
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		byte[] out = stream_out.ptrB();

		for (int i = 0; i < width * height; i++)
		{
			out[i * 3] = byteArray[i * 4];
			out[i * 3 + 1] = byteArray[i * 4 + 1];
			out[i * 3 + 2] = byteArray[i * 4 + 2];
		}

		return true;
	}

	@Override
	protected double getSampleRate()
	{
		return options.sampleRate.get();
	}

	@Override
	protected int getSampleDimension()
	{
		return sampleDimension;
	}

	@Override
	protected Cons.Type getSampleType()
	{
		return Cons.Type.IMAGE;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[]{"image"};

		((ImageStream)_stream_out).width = width;
		((ImageStream)_stream_out).height = height;
		((ImageStream) stream_out).format = Cons.ImageFormat.FLEX_RGB_888.val;
	}
}
