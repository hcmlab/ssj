/*
 * TFLiteWrapper.java
 * Copyright (c) 2021
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

import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 03.02.2021.
 */
public class TFLiteWrapper
{
	// An instance of the driver class to run model inference with Tensorflow Lite.
	private Interpreter modelInterpreter;

	// Optional GPU delegate for accleration.
	private GpuDelegate gpuDelegate;

	// GPU Compatibility
	private boolean gpuSupported;

	private boolean useGPU;

	public TFLiteWrapper(boolean useGPU)
	{
		this.useGPU = useGPU;
	}

	public Interpreter.Options getInterpreterOptions()
	{
		// Check gpu compatibility
		CompatibilityList compatList = new CompatibilityList();
		gpuSupported = compatList.isDelegateSupportedOnThisDevice();

		Log.i("GPU delegate supported: " + gpuSupported);

		Interpreter.Options interpreterOptions = new Interpreter.Options();

		// Initialize interpreter with GPU delegate
		if (gpuSupported && useGPU)
		{
			// If the device has a supported GPU, add the GPU delegate
			gpuDelegate = new GpuDelegate(compatList.getBestOptionsForThisDevice());
			interpreterOptions.addDelegate(gpuDelegate);
		}
		else
		{
			// If the GPU is not supported, enable XNNPACK acceleration
			interpreterOptions.setUseXNNPACK(true);
			interpreterOptions.setNumThreads(Runtime.getRuntime().availableProcessors());
		}

		return interpreterOptions;
	}

	public void loadModel(File modelFile, Interpreter.Options interpreterOptions)
	{
		modelInterpreter = new Interpreter(modelFile, interpreterOptions);
	}

	public void loadModel(File modelFile)
	{
		loadModel(modelFile, getInterpreterOptions());
	}

	public void run(Object input, Object output)
	{
		if (modelInterpreter != null)
		{
			// Run inference
			try
			{
				modelInterpreter.run(input, output);
			}
			catch (Exception e)
			{
				Log.e("Error while running tflite inference", e);
			}
		}
	}

	public void runMultiInputOutput(Object[] inputs, Map<Integer, Object> outputs)
	{
		if (modelInterpreter != null)
		{
			// Run inference
			try
			{
				modelInterpreter.runForMultipleInputsOutputs(inputs, outputs);
			}
			catch (Exception e)
			{
				Log.e("Error while running TFLite inference", e);
			}
		}
	}

	public void convertBitmapToInputArray(Bitmap inputBitmap, int[] inputArray, ByteBuffer imgData)
	{
		convertBitmapToInputArray(inputBitmap, inputArray, imgData, 127.5f, 127.5f);
	}

	public void convertBitmapToInputArray(Bitmap inputBitmap, int[] inputArray, ByteBuffer imgData, float normShift, float normDiv)
	{
		// Get rgb pixel values as int array
		inputBitmap.getPixels(inputArray, 0, inputBitmap.getWidth(), 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight());

		// Normalize resized image
		imgData.rewind();
		for (int i = 0; i < inputArray.length; ++i)
		{
			final int val = inputArray[i];

			float r = (val >> 16) & 0xFF;
			float g = (val >> 8) & 0xFF;
			float b = (val & 0xFF);

			r = (r - normShift) / normDiv;
			g = (g - normShift) / normDiv;
			b = (b - normShift) / normDiv;

			// Fill byte buffer for model input
			imgData.putFloat(r);
			imgData.putFloat(g);
			imgData.putFloat(b);
		}
	}

	public void close()
	{
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
