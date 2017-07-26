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

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.File;
import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * TensorFlow model.
 * Supports prediction using tensor flow's Android API.
 * Requires pre-trained frozen graph, e.g. using SSI.
 *
 * @author Vitaly Krumins
 */

public class TensorFlow extends Model
{
	public class Options extends OptionList
	{
		public final Option<String> inputNode = new Option<>("inputNode", "input", String.class, "name of the input node");
		public final Option<String> outputNode = new Option<>("outputNode", "output", String.class, "name of the output node");
		public final Option<long[]> shape = new Option<>("shape", null, long[].class, "shape of the input tensor");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();
	public int classNum;
	public String[] classNames;

	private Graph graph;
	private Session session;

	static
	{
		System.loadLibrary("tensorflow_inference");
	}


	/**
	 * Returns index of element with the highest value in float array.
	 *
	 * @param probabilities Float array.
	 * @return Index of element with the highest value.
	 */
	public static int maxIndex(float[] probabilities) {
		int best = 0;
		for (int i = 1; i < probabilities.length; ++i) {
			if (probabilities[i] > probabilities[best]) {
				best = i;
			}
		}
		return best;
	}


	@Override
	public int getNumClasses()
	{
		return classNum;
	}


	@Override
	public String[] getClassNames()
	{
		return classNames;
	}


	@Override
	protected float[] forward(Stream[] stream)
	{
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

		float[] floatValues = stream[0].ptrF();
		float[] probabilities = makePrediction(floatValues);

		return probabilities;
	}


	@Override
	protected void train(Stream[] stream)
	{
		Log.e("training not supported yet");
	}


	@Override
	protected void save(File file)
	{
		Log.e("saving not supported yet");
	}


	@Override
	protected void loadOption(File file)
	{
		Log.e("option loading not supported yet");
	}


	@Override
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


	/**
	 * Makes prediction about the given image data.
	 *
	 * @param floatValues RGB float data.
	 * @return Probability array.
	 */
	private float[] makePrediction(float[] floatValues)
	{
		Tensor input = Tensor.create(options.shape.get(), FloatBuffer.wrap(floatValues));
		Tensor result = session.runner()
				.feed(options.inputNode.get(), input)
				.fetch(options.outputNode.get())
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
}
