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
import java.nio.FloatBuffer;
import java.util.Arrays;

import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;

/**
 * TensorFlow model.
 * Supports prediction using tensor flow's Android API.
 * Requires pre-trained frozen graph, e.g. using SSI.
 *
 * @author Vitaly Krumins
 */

// TODO: Encapsulate int/float-value arrays into ImageData class.
public class TensorFlow extends Model
{
	private Graph graph;
	private Session session;

	// Constants for inception model evaluation
	private final int INPUT_SIZE = 224;
	private final int IMAGE_MEAN = 117;
	private final float IMAGE_STD = 1;
	private final String INPUT_NAME = "input";
	private final String OUTPUT_NAME = "output";
	private final boolean MAINTAIN_ASPECT = true;

	private int[] intValues;
	private float[] floatValues;

	private int classNum;
	private String[] classNames;

	private ImageData imageData;

	static
	{
		System.loadLibrary("tensorflow_inference");
	}

	/**
	 * Initializes image data of proper size.
	 */
	public TensorFlow()
	{
		imageData = new ImageData(INPUT_SIZE, MAINTAIN_ASPECT);
		intValues = new int[INPUT_SIZE * INPUT_SIZE];
		floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];
	}

	protected float[] forward(Stream[] stream)
	{
		// Create cropped bitmap for prediction.
		Bitmap bitmap = imageData.createRgbBitmap(stream[0].ptrB());

		// Get probability array.
		float[] probabilities = recognizeImage(bitmap);

		// Make prediction.
		int bestLabelIdx = maxIndex(probabilities);
		Log.d("tf_ssj",
			  String.format("BEST MATCH: %s (%.2f%% likely)",
							classNames[bestLabelIdx], probabilities[bestLabelIdx] * 100f));

		return null;
	}

	/**
	 * Makes prediction about the given image.
	 *
	 * @param bitmap Cropped bitmap to classify.
	 * @return Probability array.
	 */
	private float[] recognizeImage(Bitmap bitmap)
	{
		bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		for (int i = 0; i <intValues.length; ++i)
		{
			final int val = intValues[i];
			floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
			floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
			floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
		}

		Tensor input = Tensor.create(new long[] {1, INPUT_SIZE, INPUT_SIZE, 3}, FloatBuffer.wrap(floatValues));
		Tensor result = session.runner()
				.feed(INPUT_NAME, input)
				.fetch(OUTPUT_NAME)
				.run().get(0);

		long[] rshape = result.shape();
		if (result.numDimensions() != 2 || rshape[0] != 1)
		{
			throw new RuntimeException(
					String.format(
							"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
							Arrays.toString(rshape)));
		}
		int nlabels = (int) rshape[1];
		return result.copyTo(new float[1][nlabels])[0];
	}

	/**
	 * Returns index of element with the highest value in float array.
	 *
	 * @param probabilities Float array.
	 * @return Index of element with the highest value.
	 */
	private int maxIndex(float[] probabilities) {
		int best = 0;
		for (int i = 1; i < probabilities.length; ++i) {
			if (probabilities[i] > probabilities[best]) {
				best = i;
			}
		}
		return best;
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

			graph = new Graph();
			graph.importGraphDef(fileBytes);
			session = new Session(graph);
		}
		catch (Exception e)
		{
			Log.e("Error while importing the model: " + e.getMessage());
			return;
		}

		_isTrained = true;
	}
}