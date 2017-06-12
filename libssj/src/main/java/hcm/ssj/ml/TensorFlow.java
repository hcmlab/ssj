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

	private int classNum;
	private String[] classNames;

	private ImageData imageData;

	static
	{
		System.loadLibrary("tensorflow_inference");
	}

	public TensorFlow()
	{
		imageData = new ImageData(INPUT_SIZE, MAINTAIN_ASPECT);
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

		// Decode yuv to rgb
		ImageUtils.YUVNV21ToRgb(rgb, stream[0].ptrB(), width, height);

		// Set bitmap pixels to those saved in argb
		rgbBitmap.setPixels(rgb, 0, width, 0, 0, width, height);

		canvas.drawBitmap(rgbBitmap, frameToCropTransform, null);

		// Save image to external storage
		ImageUtils.saveBitmap(croppedBitmap, new Date().toString() + "preview3.png");
		rgb = new int[width * height];
		*/
		long startTime = System.nanoTime();
		imageData.createRgbBitmap(stream[0].ptrB());
		long duration = System.nanoTime() - startTime;


		Log.d("tf_ssj", "Execution time: " + duration / 1000000 + " ms");

		return null;
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
