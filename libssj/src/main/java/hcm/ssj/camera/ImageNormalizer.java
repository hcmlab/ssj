/*
 * ImageNormalizer.java
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

package hcm.ssj.camera;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Normalizes input image and prepares it for the inference with the
 * Inception model.
 *
 * @author Vitaly
 */

public class ImageNormalizer extends Transformer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
	{
		public final Option<Integer> imageMean = new Option<>("imageMean", 0, Integer.class, "image mean");
		public final Option<Float> imageStd = new Option<>("imageStd", 0f, Float.class, "image standard deviation");

		private Options()
		{
			addOptions();
		}
	}

	private int CHANNELS_PER_PIXEL = 3;

	private int width;
	private int height;

	public final Options options = new Options();

	public ImageNormalizer()
	{
		_name = "ImageNormalizer";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Convert byte array to integer array
		int[] rgb = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Normalize image values and write result to the output buffer
		normalizeImageValues(rgb, stream_out.ptrF());
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		ImageStream stream = ((ImageStream) stream_in[0]);
		return stream.width * stream.height * CHANNELS_PER_PIXEL;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[] { "Normalized image float values" };
	}

	/**
	 * Prepares image for the classification with the Inception model.
	 *
	 * @param rgb Pixel values to normalize.
	 * @param out Output stream.
	 */
	private void normalizeImageValues(int[] rgb, float[] out)
	{
		int imageMean = options.imageMean.get();
		float imageStd = options.imageStd.get();

		for (int i = 0; i < rgb.length; ++i)
		{
			final int val = rgb[i];
			out[i * 3] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
			out[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
			out[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
		}
	}
}
