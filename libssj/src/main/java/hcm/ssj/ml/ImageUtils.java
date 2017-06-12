/*
 * ImageUtils.java
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

package hcm.ssj.ml;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileOutputStream;

import hcm.ssj.core.Log;
import hcm.ssj.file.LoggingConstants;

/**
 * Contains necessary functions for image decoding and cropping.
 *
 * @author Vitaly
 */

public class ImageUtils
{
	public static void yuvNv21ToRgb(int[] argb, byte[] yuv, int width, int height) {
		final int frameSize = width * height;
		final int ii = 0;
		final int ij = 0;
		final int di = +1;
		final int dj = +1;

		int a = 0;
		for (int i = 0, ci = ii; i < height; ++i, ci += di) {
			for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
				int y = (0xff & ((int) yuv[ci * width + cj]));
				int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
				int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
				y = y < 16 ? 16 : y;

				int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
				int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
				int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

				r = r < 0 ? 0 : (r > 255 ? 255 : r);
				g = g < 0 ? 0 : (g > 255 ? 255 : g);
				b = b < 0 ? 0 : (b > 255 ? 255 : b);

				argb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}
	}

	public static void saveBitmap(final Bitmap bitmap, final String filename) {
		final String root =
				LoggingConstants.SSJ_EXTERNAL_STORAGE + File.separator + "tensorflow";
		final File myDir = new File(root);

		if (!myDir.mkdirs()) {
			Log.i("Make dir failed");
		}

		final String fname = filename;
		final File file = new File(myDir, fname);
		if (file.exists()) {
			file.delete();
		}
		try {
			final FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
			out.flush();
			out.close();
		} catch (final Exception e) {
			Log.e("tf_ssj", "Exception!");
		}
	}

	public static Matrix getTransformationMatrix(
			final int srcWidth,
			final int srcHeight,
			final int dstWidth,
			final int dstHeight,
			final int applyRotation,
			final boolean maintainAspectRatio) {
		final Matrix matrix = new Matrix();

		if (applyRotation != 0) {
			// Translate so center of image is at origin.
			matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

			// Rotate around origin.
			matrix.postRotate(applyRotation);
		}

		// Account for the already applied rotation, if any, and then determine how
		// much scaling is needed for each axis.
		final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

		final int inWidth = transpose ? srcHeight : srcWidth;
		final int inHeight = transpose ? srcWidth : srcHeight;

		// Apply scaling if necessary.
		if (inWidth != dstWidth || inHeight != dstHeight) {
			final float scaleFactorX = dstWidth / (float) inWidth;
			final float scaleFactorY = dstHeight / (float) inHeight;

			if (maintainAspectRatio) {
				// Scale by minimum factor so that dst is filled completely while
				// maintaining the aspect ratio. Some image may fall off the edge.
				final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
				matrix.postScale(scaleFactor, scaleFactor);
			} else {
				// Scale exactly to fill dst from src.
				matrix.postScale(scaleFactorX, scaleFactorY);
			}
		}

		if (applyRotation != 0) {
			// Translate back from origin centered reference to destination frame.
			matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
		}

		return matrix;
	}
}
