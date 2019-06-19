/*
 * ImageResizer.java
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
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
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
	{
		public final Option<Integer> size = new Option<>("size", 0, Integer.class, "size of the image after resizing");
		public final Option<Integer> rotation = new Option<>("rotation", 90, Integer.class, "rotation of the resulting image");
		public final Option<Boolean> maintainAspect = new Option<>("maintainAspect", true, Boolean.class, "maintain aspect ration");
		public final Option<Boolean> savePreview = new Option<>("savePreview", false, Boolean.class, "save preview image");
		public final Option<Boolean> cropImage = new Option<>("cropImage", false, Boolean.class, "crop image instead of resizing");

		private Options()
		{
			addOptions();
		}
	}


	public final Options options = new Options();

	private int width;
	private int height;
	private int size;
	private int[] intValues;

	private Bitmap rgbBitmap;
	private Bitmap finalBitmap;
	private Canvas canvas;
	private Matrix frameToCropTransform;


	public ImageResizer()
	{
		_name = "ImageResizer";
	}


	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		// Create bitmap for the original image
		rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		size = options.size.get();

		// Size of the final image can't be larger than that of the original.
		if (size <= 0 || size >= width || size >= height)
		{
			Log.e("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		intValues = new int[size * size];

		// Create bitmap for the cropped image
		finalBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

		// Transform image to be of a quadratic form as Inception model only
		// accepts images with the same width and height
		frameToCropTransform = CameraUtil.getTransformationMatrix(
				width, height, size, size,
				options.rotation.get(), options.maintainAspect.get());

		Matrix cropToFrameTransform = new Matrix();
		frameToCropTransform.invert(cropToFrameTransform);
		canvas = new Canvas(finalBitmap);
	}


	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (size <= 0 || size >= width || size >= height)
		{
			Log.e("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		// Convert byte array to integer array
		int[] rgb = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		Bitmap bitmap;

		if (options.cropImage.get())
		{
			bitmap = cropImage(rgb);
		}
		else
		{
			bitmap = resizeImage(rgb);
		}

		if (options.savePreview.get())
		{
			CameraUtil.saveBitmap(bitmap);
		}

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
		int cropSize = options.size.get();

		((ImageStream) stream_out).width = cropSize;
		((ImageStream) stream_out).height = cropSize;
		((ImageStream) stream_out).format = Cons.ImageFormat.FLEX_RGB_888.val; // Android ImageFormat.FLEX_RGB_888;
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

		return finalBitmap;
	}


	/**
	 * Crops out the center of the given image.
	 *
	 * @param rgb RGB pixel values of an image.
	 * @return Cropped bitmap.
	 */
	public Bitmap cropImage(int[] rgb)
	{
		// Size of the final image can't be larger than that of the original.
		if (size >= width || size >= height)
		{
			Log.e("Invalid crop size. Crop size must be smaller than width and height.");
			return null;
		}

		// Calculate matrix offsets
		int heightMargin = (height - size) / 2;
		int widthMargin = (width - size) / 2;

		// Cut out the center of the original image.
		for (int y = heightMargin, cy = 0; y < height - heightMargin; y++, cy++)
		{
			for (int x = widthMargin, cx = 0; x < width - widthMargin; x++, cx++)
			{
				// Copy pixels from the original pixel matrix to the cropped one
				intValues[cy * size + cx] = rgb[y * width + x];
			}
		}

		int[] cropped = new int[size * size];

		// Rotate the final image 90 degrees.
		for (int x = size - 1, destX = 0; x > 0; x--, destX++)
		{
			for (int y = 0; y < size; y++)
			{
				cropped[size * y + x] = intValues[size * destX + y];
			}
		}

		// Set pixel values of the cropped image
		finalBitmap.setPixels(cropped, 0, size, 0, 0, size, size);

		return finalBitmap;
	}
}
