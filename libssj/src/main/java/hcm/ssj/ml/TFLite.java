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
import org.tensorflow.lite.gpu.GpuDelegate;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;

/**
 * Created by Michael Dietz on 28.10.2019.
 */
public class TFLite extends Model
{
	public class Options extends Model.Options
	{
		public final Option<String> inputNode = new Option<>("input", "input", String.class, "name of the input node");
		public final Option<String> outputNode = new Option<>("output", "output", String.class, "name of the output node");
		public final Option<long[]> shape = new Option<>("shape", new long[] {1, 224, 224, 3}, long[].class, "shape of the input tensor");

		private Options()
		{
			super();
			addOptions();
		}
	}

	public TFLite.Options options = new TFLite.Options();

	// An instance of the driver class to run model inference with Tensorflow Lite.
	private Interpreter modelInterpreter;

	// Optional GPU delegate for accleration.
	private GpuDelegate gpuDelegate;

	// ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
	private ByteBuffer imgData = null;

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
	void init(String[] classes, int n_features)
	{
		// width * height * channels * bytes per pixel (e.g., 4 for float)
		imgData = ByteBuffer.allocateDirect(input_bytes * input_dim);
		imgData.order(ByteOrder.nativeOrder());
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
		float[][] prediction = new float[1][n_classes];

		// Fill byte buffer
		imgData.rewind();
		for (int i = 0; i < floatValues.length; i++)
		{
			imgData.putFloat(floatValues[i]);
		}

		// Run inference
		try
		{
			modelInterpreter.run(imgData, prediction);
		}
		catch (Exception e)
		{
			Log.e("Error while running tflite inference", e);
		}

		return prediction[0];
	}

	@Override
	void loadModel(File file)
	{
		// Initialize interpreter with GPU delegate
		gpuDelegate = new GpuDelegate();
		Interpreter.Options options = (new Interpreter.Options()).addDelegate(gpuDelegate);
		modelInterpreter = new Interpreter(file, options);

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
		if (modelInterpreter != null)
		{
			modelInterpreter.close();
		}

		if (gpuDelegate != null)
		{
			gpuDelegate.close();
		}
	}
}
