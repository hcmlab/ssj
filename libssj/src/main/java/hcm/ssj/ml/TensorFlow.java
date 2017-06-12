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

	private int classNum;
	private String[] classNames;

	static
	{
		System.loadLibrary("tensorflow_inference");
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
		int width = 640;
		int height = 480;

		int[] argb = new int[width * height];

		yuvNv21ToRgb(argb, stream[0].ptrB(), width, height);
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bmp.setPixels(argb, 0, width, 0, 0, width, height);
		storeImage(bmp);

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

	private void storeImage(Bitmap image)
	{
		File pictureFile = getOutputMediaFile();
		if (pictureFile == null) {
			Log.d("tf_ssj", "Error creating media file");
			return;
		}
		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			image.compress(Bitmap.CompressFormat.PNG, 90, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			Log.d("tf_ssj", "File not found: " + e.getMessage());
		} catch (IOException e) {
			Log.d("tf_ssj", "Error accessing file: " + e.getMessage());
		}
	}

	private File getOutputMediaFile(){
		File mediaStorageDir = new File(LoggingConstants.SSJ_EXTERNAL_STORAGE + "/CamFiles");

		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				return null;
			}
		}
		// Create a media file name
		String timeStamp = new Date().toString();
		File mediaFile;
		String mImageName="MI_"+ timeStamp +".jpg";
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
		return mediaFile;
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
