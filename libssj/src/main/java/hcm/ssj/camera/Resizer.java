/*
 * ImageResizer.java
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

package hcm.ssj.camera;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.Date;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer that re-sizes image to necessary dimensions.
 * @author Vitaly
 */

public class Resizer extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Integer> cropSize = new Option<>("cropSize", 0, Integer.class, "size of the cropped image");
		public final Option<Boolean> maintainAspect = new Option<>("maintainAspect", false, Boolean.class, "maintain aspect ration");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();
	private CameraImageResizer imageResizer;

	private int width;
	private int height;
	private int counter = 0;

	private int[] intValues;

	public Resizer()
	{
		_name = "Resizer";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		// Get user options
		int cropSize = options.cropSize.get();
		boolean maintainAspect = options.maintainAspect.get();

		if (cropSize > 0)
			imageResizer = new CameraImageResizer(width, height, cropSize, maintainAspect);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		int[] rgb = decodeBytes(stream_in[0].ptrB());
		Bitmap cropped = imageResizer.cropImage(rgb);

		CameraUtil.saveBitmap(cropped, (counter++) + "-preview.png");
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return stream_in[0].dim;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return stream_in[0].type;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[] { "Cropped video" };
	}

	/**
	 * Converts RGB bytes to RGB ints.
	 *
	 * @param rgbBytes RGB color bytes.
	 * @return RGB color integers.
	 */
	private int[] decodeBytes(byte[] rgbBytes)
	{
		int[] rgb = new int[width * height];

		for (int i = 0; i < width * height; i++)
		{
			int r = rgbBytes[i * 3];
			int g = rgbBytes[i * 3 + 1];
			int b = rgbBytes[i * 3 + 2];

			if (r < 0)
				r += 256;
			if (g < 0)
				g += 256;
			if (b < 0)
				b += 256;

			rgb[i] = 0xff000000 | (r << 16) | (g << 8) | b;
		}

		return rgb;
	}
}
