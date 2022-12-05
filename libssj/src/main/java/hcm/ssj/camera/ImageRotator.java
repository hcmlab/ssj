/*
 * ImageRotator.java
 * Copyright (c) 2022
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

import android.graphics.Bitmap;
import android.graphics.Matrix;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 05.12.2022.
 */
public class ImageRotator extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Integer> rotation = new Option<>("rotation", 270, Integer.class, "rotation of the resulting image, use 270 for front camera and 90 for back camera");
		public final Option<Integer> outputWidth = new Option<>("outputWidth", 480, Integer.class, "output width of the rotated image");
		public final Option<Integer> outputHeight = new Option<>("outputHeight", 640, Integer.class, "output height of the rotated image");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	// Original sizes
	int inputWidth;
	int inputHeight;

	Matrix rotationMatrix;
	int[] originalInputArray;
	int[] outputArray;

	// Create bitmap for the original image
	Bitmap inputBitmap;

	// Rotated bitmap for original image
	Bitmap rotatedBitmap;

	// Cropped bitmap to output sizes
	Bitmap outputBitmap;

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public ImageRotator()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		inputWidth = ((ImageStream) stream_in[0]).width;
		inputHeight = ((ImageStream) stream_in[0]).height;

		outputArray = new int[options.outputWidth.get() * options.outputHeight.get()];

		// Create bitmap for the original image
		inputBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
		rotationMatrix = new Matrix();
		rotationMatrix.postRotate(options.rotation.get());
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Convert byte array to integer array
		originalInputArray = CameraUtil.decodeBytes(stream_in[0].ptrB(), inputWidth, inputHeight);

		// Create bitmap from byte array
		inputBitmap.setPixels(originalInputArray, 0, inputWidth, 0, 0, inputWidth, inputHeight);

		// Rotate original input
		rotatedBitmap = Bitmap.createBitmap(inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), rotationMatrix, true);

		// Resize rotated original input to model input size
		outputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, options.outputWidth.get(), options.outputHeight.get(), true);

		// Convert bitmap to byte stream
		CameraUtil.convertBitmapToByteArray(outputBitmap, outputArray, stream_out.ptrB());

		// Free bitmap memory
		rotatedBitmap.recycle();
		outputBitmap.recycle();
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		// width * height * channels
		return options.outputWidth.get() * options.outputHeight.get() * 3;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.IMAGE;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = stream_in[0].desc;

		((ImageStream) stream_out).width = options.outputWidth.get();
		((ImageStream) stream_out).height = options.outputHeight.get();
		((ImageStream) stream_out).format = Cons.ImageFormat.FLEX_RGB_888.val; // Android ImageFormat.FLEX_RGB_888;
	}
}
