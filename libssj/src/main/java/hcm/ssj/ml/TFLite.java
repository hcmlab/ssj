/*
 * TFLite.java
 * Copyright (c) 2019
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

package hcm.ssj.ml;

import android.util.Xml;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 28.10.2019.
 */
public class TFLite extends Model
{
	public class Options extends Model.Options
	{
		//public final Option<long[]> shape = new Option<>("shape", new long[] {1, 224, 224, 3}, long[].class, "shape of the input tensor");
		public final Option<Boolean> useGPU = new Option<>("useGPU", true, Boolean.class, "if true tries to use GPU for better performance");

		private Options()
		{
			super();
			addOptions();
		}
	}

	public TFLite.Options options = new TFLite.Options();

	private TFLiteWrapper tfLiteWrapper;

	// ByteBuffer to hold input data (e.g., images), to be feed into Tensorflow Lite as inputs.
	private ByteBuffer inputData = null;

	// Map to hold output data
	private Map<Integer, Object> outputs = null;

	public TFLite()
	{
		_name = "TFLite";
	}

	@Override
	public Options getOptions()
	{
		return options;
	}

	@Override
	void init(int input_dim, int output_dim, String[] outputNames)
	{
		// For images width * height * channels * bytes per pixel (e.g., 4 for float)
		inputData = ByteBuffer.allocateDirect(input_bytes * input_dim * input_num);
		inputData.order(ByteOrder.nativeOrder());

		outputs = new HashMap<>();

		for (int i = 0; i < output_heads; i++)
		{
			outputs.put(i, FloatBuffer.allocate(output_dims[i]));
		}
	}

	@Override
	float[] forward(Stream stream)
	{
		if (!isTrained)
		{
			Log.w("not trained");
			return null;
		}

		float[] floatValues = stream.ptrF();

		return makePrediction(floatValues);
	}

	/**
	 * Makes prediction about the given image data.
	 *
	 * @param floatValues RGB float data.
	 * @return Probability array.
	 */
	private float[] makePrediction(float[] floatValues)
	{
		float[] prediction = new float[output_dim];

		inputData.rewind();

		for (Integer key : outputs.keySet())
		{
			((FloatBuffer) outputs.get(key)).rewind();
		}

		// Fill byte buffer
		for (int i = 0; i < floatValues.length; i++)
		{
			inputData.putFloat(floatValues[i]);
		}

		// Run inference
		tfLiteWrapper.runMultiInputOutput(new Object[] {inputData}, outputs);

		int startPos = 0;
		for (Integer key : outputs.keySet())
		{
			float[] outputArray = ((FloatBuffer) outputs.get(key)).array();

			System.arraycopy(outputArray, 0, prediction, startPos, output_dims[key]);

			startPos += output_dims[key];
		}

		return prediction;
	}

	@Override
	void loadModel(File file)
	{
		tfLiteWrapper = new TFLiteWrapper(options.useGPU.get());
		tfLiteWrapper.loadModel(file);

		isTrained = true;
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

						Object currentValue = options.getOptionValue(optionName);
						if(currentValue == null)
							options.setOptionValue(optionName, optionValue);
					}
				}
				eventType = parser.next();
			}
		}
		catch (Exception e)
		{
			Log.e("Error while loading model option file", e);
		}
	}

	@Override
	public void close()
	{
		super.close();

		// Clean up
		if (tfLiteWrapper != null)
		{
			tfLiteWrapper.close();
		}
	}
}
