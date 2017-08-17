/*
 * ImageResizer.java
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

package hcm.ssj.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import java.util.Date;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer that re-sizes image to necessary dimensions.
 * @author Vitaly
 */

public class ImageResizer extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Integer> cropSize = new Option<>("cropSize", 0, Integer.class, "size of the cropped image");
		public final Option<Integer> rotation = new Option<>("rotation", 90, Integer.class, "rotation of the resulting image");
		public final Option<Boolean> maintainAspect = new Option<>("maintainAspect", true, Boolean.class, "maintain aspect ration");
		public final Option<Boolean> savePreview = new Option<>("savePreview", false, Boolean.class, "save preview image");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private int width;
	private int height;
	private int cropSize;
	private int[] intValues;

	private Bitmap rgbBitmap;
	private Bitmap croppedBitmap;
	private Canvas canvas;
	private Matrix frameToCropTransform;

	public ImageResizer()
	{
		_name = "ImageResizer";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		// Create bitmap for the original image
		rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		// Get user options
		cropSize = options.cropSize.get();

		if (cropSize <= 0 || cropSize >= width || cropSize >= height)
		{
			Log.e("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		intValues = new int[cropSize * cropSize];

		// Create bitmap for the cropped image
		croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

		// Transform image to be of a quadratic form as Inception model only
		// accepts images with the same width and height
		frameToCropTransform = CameraUtil.getTransformationMatrix(
				width, height, cropSize, cropSize,
				options.rotation.get(), options.maintainAspect.get());

		Matrix cropToFrameTransform = new Matrix();
		frameToCropTransform.invert(cropToFrameTransform);
		canvas = new Canvas(croppedBitmap);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		if (cropSize <= 0 || cropSize >= width || cropSize >= height)
		{
			Log.e("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		// Convert byte array to integer array
		int[] rgb = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Resize image and write byte array to output buffer
		Bitmap bitmap = resizeImage(rgb);

		if (options.savePreview.get())
			CameraUtil.saveBitmap(bitmap, new Date().toString() + ".png");

		bitmapToByteArray(bitmap, stream_out.ptrB());
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
		stream_out.desc = new String[] { "Cropped video" };
		int cropSize = options.cropSize.get();

		((ImageStream) stream_out).width = cropSize;
		((ImageStream) stream_out).height = cropSize;
		((ImageStream) stream_out).format = 0x29; //ImageFormat.FLEX_RGB_888;
	}

	/**
	 * Converts bitmap to corresponding byte array and writes it
	 * to the output buffer.
	 *
	 * @param bitmap Bitmap to convert to byte array.
	 * @param out Output buffer.
	 */
	private void bitmapToByteArray(Bitmap bitmap, byte[] out)
	{
		bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		for (int i = 0; i < bitmap.getWidth() * bitmap.getHeight(); ++i)
		{
			final int pixel = intValues[i];
			out[i * 3] = (byte)((pixel >> 16) & 0xFF);
			out[i * 3 + 1] = (byte)((pixel >> 8) & 0xFF);
			out[i * 3 + 2] = (byte)(pixel & 0xFF);
		}
	}

	/**
	 * Forces an image to be of the same width and height.
	 *
	 * @param rgb RGB integer values.
	 * @return Cropped image.
	 */
	public Bitmap resizeImage(int[] rgb)
	{
		rgbBitmap.setPixels(rgb, 0, width, 0, 0, width, height);

		// Resize bitmap to a quadratic form
		canvas.drawBitmap(rgbBitmap, frameToCropTransform, null);

		return croppedBitmap;
	}
}
