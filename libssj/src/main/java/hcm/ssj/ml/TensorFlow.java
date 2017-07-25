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

import android.util.Xml;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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

public class TensorFlow extends Model
{
	private Graph graph;
	private Session session;

	private int classNum;
	private String[] classNames;

	private String inputNode;
	private String outputNode;

	private long[] inputTensorShape;

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


	/**
	 * Set label count for the classifier.
	 *
	 * @param classNum amount of object classes to recognize.
	 */
	public void setNumClasses(int classNum)
	{
		this.classNum = classNum;
	}


	/**
	 * Set label strings for the classifier.
	 *
	 * @param classNames recognized object classes.
	 */
	public void setClassNames(String[] classNames)
	{
		this.classNames = classNames;
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
		XmlPullParser parser = Xml.newPullParser();

		try
		{
			parser.setInput(new FileReader(file));
			parser.next();

			int eventType = parser.getEventType();

			// Check if option file is of the right format.
			if (eventType != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("options"))
			{
				Log.w("unknown or malformed trainer file");
				return;
			}

			while (eventType != XmlPullParser.END_DOCUMENT)
			{
				if (eventType == XmlPullParser.START_TAG)
				{
					if (parser.getName().equalsIgnoreCase("item"))
					{
						String optionName = parser.getAttributeValue(null, "name");
						String optionValue = parser.getAttributeValue(null, "value");

						// Set input node name.
						if (optionName.equalsIgnoreCase("input"))
						{
							inputNode = optionValue;
						}

						// Set output node name.
						if (optionName.equalsIgnoreCase("output"))
						{
							outputNode = optionValue;
						}

						// Set input tensor shape.
						if (optionName.equalsIgnoreCase("shape"))
						{
							String shape = optionValue;
							inputTensorShape = parseTensorShape(shape);
						}
					}
				}
				eventType = parser.next();
			}
		}
		catch (Exception e)
		{
			Log.e(e.getMessage());
		}
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
		Tensor input = Tensor.create(inputTensorShape, FloatBuffer.wrap(floatValues));
		Tensor result = session.runner()
				.feed(inputNode, input)
				.fetch(outputNode)
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
	 * Parses string representation of input tensor shape.
	 *
	 * @param shape String that represents tensor shape.
	 * @return shape of the input tensor as a n-dimensional array.
	 */
	private long[] parseTensorShape(String shape)
	{
		// Delete square brackets and white spaces.
		String formatted = shape.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "");

		// Separate each dimension value.
		String[] shapeArray = formatted.split(",");

		long[] tensorShape = new long[shapeArray.length];

		for (int i = 0; i < shapeArray.length; i++)
		{
			tensorShape[i] = Integer.parseInt(shapeArray[i]);
		}

		return tensorShape;
	}
}
