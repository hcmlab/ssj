/*
 * TensorFlowModel.java
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
import android.graphics.Canvas;
import android.graphics.Matrix;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Date;

import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.LoggingConstants;

/**
 * TensorFlow model.
 * Supports prediction using tensor flow's Android API.
 * Requires pre-trained frozen graph, e.g. using SSI.
 *
 * @author Vitaly Krumins
 */

public class TensorFlow extends Model
{
	private Graph modelGraph;
	private Session session;

	// Constants for inception model evaluation
	private final int INPUT_SIZE = 224;
	private final int IMAGE_MEAN = 117;
	private final float IMAGE_STD = 1;
	private final String INPUT_NAME = "input";
	private final String OUTPUT_NAME = "output";
	private final boolean MAINTAIN_ASPECT = true;

	private Bitmap rgbBitmap;
	private Bitmap croppedBitmap;

	Canvas canvas;

	Matrix cropToFrameTransform;
	Matrix frameToCropTransform;

	private int[] rgb;

	int width = 640;
	int height = 480;

	private int classNum;
	private String[] classNames;

	static
	{
		System.loadLibrary("tensorflow_inference");
	}

	public TensorFlow()
	{
		rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
		rgb = new int[width * height];
		cropToFrameTransform = new Matrix();
		frameToCropTransform = getTransformationMatrix(
				width, height, INPUT_SIZE, INPUT_SIZE, 90, MAINTAIN_ASPECT
		);
		frameToCropTransform.invert(cropToFrameTransform);
		canvas = new Canvas(croppedBitmap);
	}

	protected float[] forward(Stream[] stream)
	{
/*
		if (stream.length != 1)
		{
			Log.w("only one input stream currently supported, consider using merge");
			return null;
		}
		if (!_isTrained)
		{
			Log.w("not trained");
			return null;
		}
		if (stream[0].type != Cons.Type.FLOAT) {
			Log.w ("invalid stream type");
			return null;
		}

		float[] ptr = stream[0].ptrF();

		FloatBuffer fb = FloatBuffer.allocate(stream[0].dim);

		for (float f : ptr)
			fb.put(f);
		fb.rewind();

		Tensor inputTensor = Tensor.create(new long[] {stream[0].num, stream[0].dim}, fb);
		Tensor resultTensor = session.runner()
				.feed("input/x", inputTensor)
				.fetch("Wx_plus_b/output")
				.run().get(0);

		float[][] probabilities = new float[1][classNum];
		resultTensor.copyTo(probabilities);

		return probabilities[0];
*/

		// Decode yuv to rgb
		yuvNv21ToRgb(rgb, stream[0].ptrB(), width, height);

		// Set bitmap pixels to those saved in argb
		rgbBitmap.setPixels(rgb, 0, width, 0, 0, width, height);

		canvas.drawBitmap(rgbBitmap, frameToCropTransform, null);

		// Save image to external storage
		saveBitmap(croppedBitmap, new Date().toString() + "preview3.png");
		rgb = new int[width * height];
		return null;
	}

	private void yuvNv21ToRgb(int[] argb, byte[] yuv, int width, int height) {
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

	private void saveBitmap(final Bitmap bitmap, final String filename) {
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

	private Matrix getTransformationMatrix(
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

	@Override
	void train(Stream[] stream)
	{
		Log.e("training not supported yet");
	}

	protected void loadOption(File file)
	{
		// Empty implementation
	}

	@Override
	void save(File file)
	{
		Log.e("saving not supported yet");
	}

	@Override
	int getNumClasses()
	{
		return classNum;
	}

	public void setNumClasses(int classNum)
	{
		this.classNum = classNum;
	}

	@Override
	String[] getClassNames()
	{
		return classNames;
	}

	public void setClassNames(String[] classNames)
	{
		this.classNames = classNames;
	}

	protected void load(File file)
	{
		FileInputStream fileInputStream;
		byte[] fileBytes = new byte[(int) file.length()];

		try
		{
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(fileBytes);
			fileInputStream.close();

			modelGraph = new Graph();
			modelGraph.importGraphDef(fileBytes);
			session = new Session(modelGraph);
		}
		catch (Exception e)
		{
			Log.e("Error while importing the model: " + e.getMessage());
			return;
		}

		_isTrained = true;
	}
}
