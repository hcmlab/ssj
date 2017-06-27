/*
 * NV21Decoder.java
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

import android.graphics.Bitmap;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer that is responsible for decoding of NV21 raw image nv21Data into
 * RGB color format.
 *
 * @author Vitaly
 */
public class NV21ToRGBDecoder extends Transformer
{
	/**
	 * All options for the decoder
	 */
	public class Options extends OptionList
	{
		public final Option<Boolean> prepareForInception = new Option<>("prepareForInception", false, Boolean.class, "prepare rgb int nv21Data for inference");

		private Options()
		{
			addOptions();
		}
	}

	private static final int IMAGE_MEAN = 117;
	private static final float IMAGE_STD = 1;
	private static final int CROP_SIZE = 224;
	private static final boolean MAINTAIN_ASPECT = true;
	private static final int CHANNELS_PER_PIXEL = 3;

	private int[] intValues;
	private float[] floatValues;
	private byte[] nv21Data;

	public final Options options = new Options();

	private int width;
	private int height;

	public NV21ToRGBDecoder()
	{
		_name = "NV21ToRGBDecoder";
	}

	@Override
	public void flush(Stream[] stream_in, Stream stream_out)
	{
		// Empty implementation
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		// Initialize image stream size
		width = ((ImageStream)stream_in[0]).getWidth();
		height = ((ImageStream)stream_in[0]).getHeight();

		// Initialize rgb arrays
		intValues = new int[width * height];
		floatValues = new float[width * height * CHANNELS_PER_PIXEL];
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		// Fetch raw NV21 pixel data
		nv21Data = stream_in[0].ptrB();

		// Convert NV21 to RGB and save the pixel data inside intValues
		CameraUtil.convertNV21ToRgb(intValues, nv21Data, width, height);

		if (options.prepareForInception.get())
		{
			CameraImageCropper cropper = new CameraImageCropper(intValues, width, height,
																CROP_SIZE, MAINTAIN_ASPECT);

			// Forces image to be of a quadratic shape
			Bitmap croppedBitmap = cropper.cropImage();

			// Converts RGB to float values and saves the data to floatValues array
			convertToFloatRGB(croppedBitmap);

			float out[] = stream_out.ptrF();

			// Write data prepared for inception to the output stream
			for (int i = 0; i < out.length; i++)
			{
				out[i] = floatValues[i];
			}
		}
		else
		{
			int out[] = stream_out.ptrI();

			// Write RGB pixel data to the output stream
			for (int i = 0; i < out.length; i++)
			{
				out[i] = intValues[i];
			}
		}
	}

	/**
	 * Prepares pixel nv21Data for inference with Inception model.
	 */
	private void convertToFloatRGB(Bitmap bitmap)
	{
		bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		for (int i = 0; i <intValues.length; ++i)
		{
			final int val = intValues[i];
			floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
			floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
			floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		int dimension = (int)(stream_in[0].dim / 1.5);

		if (options.prepareForInception.get())
			return dimension * CHANNELS_PER_PIXEL;
		return dimension;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if (options.prepareForInception.get())
			return Cons.Type.FLOAT;
		return Cons.Type.INT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
	{
		stream_out.dataclass = new String[1];
		stream_out.dataclass[0] = "RGB video";
	}
}
