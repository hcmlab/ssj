/*
 * CameraImageData.java
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
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * Encapsulates data necessary for camera image decoding.
 * Each image data object consists of two bitmaps, one for original
 * RGB values and another for cropped image.
 *
 * @author Vitaly
 */
public class CameraImageResizer
{
	private static final int ROTATION = 90;
	private Bitmap rgbBitmap;
	private Bitmap croppedBitmap;

	private Canvas canvas;

	private Matrix cropToFrameTransform;
	private Matrix frameToCropTransform;

	private int width;
	private int height;

	/**
	 * Initializes bitmaps, rgb array, canvas, and transform matrices.
	 *
	 * @param width Image width in pixels.
	 * @param height Image height in pixels.
	 * @param inputSize Acceptable image size for Inception model.
	 * @param maintainAspectRatio Whether or not to retain aspect ratio of the image.
	 */
	public CameraImageResizer(int width, int height, int inputSize,
							  boolean maintainAspectRatio)
	{
		// Cache input image sizes and rgb matrix
		this.width = width;
		this.height = height;

		// Create bitmap for the original image
		rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		// Create bitmap for the cropped image
		croppedBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888);

		// Transform image to be of a quadratic form as Inception model only
		// accepts images with the same width and height
		cropToFrameTransform = new Matrix();
		frameToCropTransform = CameraUtil.getTransformationMatrix(
				width, height, inputSize, inputSize, ROTATION, maintainAspectRatio
		);
		frameToCropTransform.invert(cropToFrameTransform);
		canvas = new Canvas(croppedBitmap);
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

		// Crop bitmap to a quadratic form
		canvas.drawBitmap(rgbBitmap, frameToCropTransform, null);

		return croppedBitmap;
	}

	/**
	 * Crops image into a quadratic shape.
	 *
	 * @param rgb RGB pixel values.
	 * @param cropSize Final image size.
	 * @return Cropped bitmap.
	 */
	public Bitmap cropImage(int[] rgb, int cropSize)
	{
		if (cropSize >= width || cropSize >= height)
		{
			return null;
		}

		// Create bitmap for the cropped image
		Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

		// Create integer array for cropped image pixel data
		int[] croppedRgb = new int[cropSize * cropSize];

		// Calculate matrix offsets
		int heightMargin = (height - cropSize) / 2;
		int widthMargin = (width - cropSize) / 2;

		for (int y = heightMargin, cy = 0; y < height - heightMargin; y++, cy++)
		{
			for (int x = widthMargin, cx = 0; x < width - widthMargin; x++, cx++)
			{
				croppedRgb[cy * cropSize + cx] = rgb[y * width + x];
			}
		}

		croppedBitmap.setPixels(croppedRgb, 0, cropSize, 0, 0, cropSize, cropSize);
		return croppedBitmap;
	}
}
