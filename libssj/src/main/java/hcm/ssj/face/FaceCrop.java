/*
 * FaceCrop.java
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

package hcm.ssj.face;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;

import hcm.ssj.camera.CameraUtil;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.ml.TFLiteWrapper;
import hcm.ssj.ssd.Detection;
import hcm.ssj.ssd.SingleShotMultiBoxDetector;
import hcm.ssj.file.FileCons;

/**
 * Created by Michael Dietz on 30.10.2019.
 */
public class FaceCrop extends Transformer
{
	private static final String MODEL_NAME = "face_detection_front.tflite";
	private static final String MODEL_PATH = FileCons.MODELS_DIR + File.separator + MODEL_NAME;
	private static final int MODEL_INPUT_SIZE = 128;
	private static final int MODEL_INPUT_CHANNELS = 3;

	public class Options extends OptionList
	{
		public final Option<Integer> outputWidth = new Option<>("outputWidth", 224, Integer.class, "width of the output image");
		public final Option<Integer> outputHeight = new Option<>("outputHeight", 224, Integer.class, "height of the output image");
		public final Option<Integer> rotation = new Option<>("rotation", 270, Integer.class, "rotation of the resulting image, use 270 for front camera and 90 for back camera");
		public final Option<Integer> paddingHorizontal = new Option<>("paddingHorizontal", 0, Integer.class, "increase horizontal face crop by custom number of pixels on each side");
		public final Option<Integer> paddingVertical = new Option<>("paddingVertical", 0, Integer.class, "increase vertical face crop by custom number of pixels on each side");
		public final Option<Boolean> outputPositionEvents = new Option<>("outputPositionEvents", false, Boolean.class, "if true outputs face position as events");
		public final Option<Boolean> squeeze = new Option<>("squeeze", true, Boolean.class, "if true squeezes face area to output size");
		public final Option<Boolean> useGPU = new Option<>("useGPU", true, Boolean.class, "if true tries to use GPU for better performance");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	// Helper class to postprocess classifier results
	private SingleShotMultiBoxDetector ssd;

	private TFLiteWrapper tfLiteWrapper;

	// ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
	private ByteBuffer imgData = null;

	private int[] originalInputArray;
	private int[] modelInputArray;
	private int[] outputArray;

	// Create bitmap for the original image
	private Bitmap inputBitmap;

	// Rotated bitmap for original image
	private Bitmap rotatedBitmap;
	private Bitmap modelInputBitmap;
	private Bitmap faceBitmap;
	private Bitmap outputBitmap;

	private Matrix rotationMatrix;

	private int rotatedWidth = -1;
	private int rotatedHeight = -1;

	private List<Detection> detectionList;
	private Detection currentDetection;

	private int width;
	private int height;
	private File modelFile;

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public FaceCrop()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public void init(double frame, double delta) throws SSJException
	{
		// Download blazeface model
		modelFile = new File(MODEL_PATH);

		if (!modelFile.exists())
		{
			try
			{
				Pipeline.getInstance().download(MODEL_NAME, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
			}
			catch (IOException e)
			{
				throw new SSJException("Error while downloading face detection model!", e);
			}
		}
	}

	@Override
	public synchronized void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		tfLiteWrapper = new TFLiteWrapper(options.useGPU.get());
		tfLiteWrapper.loadModel(modelFile);

		// Initialize model input buffer: size = width * height * channels * bytes per pixel (e.g., 4 for float)
		imgData = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * MODEL_INPUT_CHANNELS * Util.sizeOf(Cons.Type.FLOAT));
		imgData.order(ByteOrder.nativeOrder());

		// Initialize model input and output integer arrays
		modelInputArray = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
		outputArray = new int[options.outputWidth.get() * options.outputHeight.get()];

		// Create bitmap for the original image
		inputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		rotationMatrix = new Matrix();
		rotationMatrix.postRotate(options.rotation.get());

		// Create SSD helper class
		ssd = new SingleShotMultiBoxDetector();
	}

	@Override
	public synchronized void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Fixed output of blazeface model
		float[][][] boxesResult = new float[1][896][16];
		float[][][] scoresResult = new float[1][896][1];

		HashMap<Integer, Object> outputs = new HashMap<>();
		outputs.put(0, boxesResult);
		outputs.put(1, scoresResult);

		// Convert byte array to integer array
		originalInputArray = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Create bitmap from byte array
		inputBitmap.setPixels(originalInputArray, 0, width, 0, 0, width, height);

		// Rotate original input
		rotatedBitmap = Bitmap.createBitmap(inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), rotationMatrix, true);

		// Resize rotated original input to model input size
		modelInputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

		// Convert and normalize [-1, 1] bitmap to model input
		tfLiteWrapper.convertBitmapToInputArray(modelInputBitmap, modelInputArray, imgData);

		// Run inference
		tfLiteWrapper.runMultiInputOutput(new Object[] { imgData }, outputs);

		// Calculate detections from model results
		detectionList = ssd.process(boxesResult, scoresResult);

		if (detectionList.size() > 0)
		{
			currentDetection = detectionList.get(0);

			// Cache width and height of rotated input image
			if (rotatedWidth < 0)
			{
				rotatedWidth = rotatedBitmap.getWidth();
				rotatedHeight = rotatedBitmap.getHeight();
			}

			// Scale relative detection values to input size
			int faceX = (int) Math.floor(currentDetection.xMin * rotatedWidth) - options.paddingHorizontal.get();
			int faceY = (int) Math.floor(currentDetection.yMin * rotatedHeight) - options.paddingVertical.get();
			int faceWidth = (int) Math.ceil(currentDetection.width * rotatedWidth) + options.paddingHorizontal.get() * 2;
			int faceHeight = (int) Math.ceil(currentDetection.height * rotatedHeight) + options.paddingVertical.get() * 2;

			if (!options.squeeze.get())
			{
				int centerX = faceX + faceWidth / 2;
				int centerY = faceY + faceHeight / 2;

				float widthRatio = options.outputWidth.get() / (float) faceWidth;
				float heightRatio = options.outputHeight.get() / (float) faceHeight;
				float scaleRatio = Math.min(widthRatio, heightRatio);

				float scaledWidth = faceWidth * scaleRatio;
				float scaledHeight = faceHeight * scaleRatio;

				if (scaledWidth < options.outputWidth.get())
				{
					scaledWidth = options.outputWidth.get();
				}

				if (scaledHeight < options.outputWidth.get())
				{
					scaledHeight = options.outputHeight.get();
				}

				faceWidth = (int) (scaledWidth / scaleRatio);
				faceHeight = (int) (scaledHeight / scaleRatio);
				faceX = centerX - faceWidth / 2;
				faceY = centerY - faceHeight / 2;
			}

			// Limit face coordinates to input size
			if (faceX < 0)
			{
				faceWidth += faceX;
				faceX = 0;
			}

			if (faceY < 0)
			{
				faceHeight += faceY;
				faceY = 0;
			}

			if (faceX + faceWidth > rotatedWidth)
			{
				faceX = rotatedWidth - faceWidth - 1;
				// faceWidth = rotatedWidth - faceX - 1;
			}

			if (faceY + faceHeight > rotatedHeight)
			{
				faceY = rotatedHeight - faceHeight - 1;
				// faceHeight = rotatedHeight - faceY - 1;
			}

			faceBitmap = Bitmap.createBitmap(rotatedBitmap, faceX, faceY, faceWidth, faceHeight);

			if (options.outputPositionEvents.get())
			{
				Event ev = Event.create(Cons.Type.FLOAT);
				ev.name = "position";
				ev.sender = "face";
				ev.time = (int) (1000 * stream_in[0].time + 0.5);
				ev.dur = (int) (1000 * (stream_in[0].num / stream_in[0].sr) + 0.5);
				ev.state = Event.State.COMPLETED;

				// Set center of face to (0, 0), scale to [-1, 1]
				float[] facePosData = new float[2];
				facePosData[0] = (currentDetection.xMin + 0.5f * currentDetection.width) * 2 - 1f;
				facePosData[1] = 1f - (currentDetection.yMin + 0.5f * currentDetection.height) * 2;

				// Clamp to [-1, 1]
				facePosData[0] = Math.max(-1.0f, Math.min(1.0f, facePosData[0]));
				facePosData[1] = Math.max(-1.0f, Math.min(1.0f, facePosData[1]));

				ev.setData(facePosData);
				_evchannel_out.pushEvent(ev);
			}
		}
		else
		{
			faceBitmap = rotatedBitmap;
		}

		// Resize to fixed output size
		outputBitmap = Bitmap.createScaledBitmap(faceBitmap, options.outputWidth.get(), options.outputHeight.get(), true);

		// Convert bitmap to byte stream
		CameraUtil.convertBitmapToByteArray(outputBitmap, outputArray, stream_out.ptrB());

		// Free bitmap memory
		rotatedBitmap.recycle();
		modelInputBitmap.recycle();
		faceBitmap.recycle();
		outputBitmap.recycle();
	}

	@Override
	public synchronized void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Clean up
		if (tfLiteWrapper != null)
		{
			tfLiteWrapper.close();
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return options.outputWidth.get() * options.outputHeight.get() * MODEL_INPUT_CHANNELS;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		if (stream_in[0].type != Cons.Type.IMAGE)
		{
			Log.e("unsupported input type");
		}

		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.IMAGE;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[] { "Detected face image" };

		((ImageStream) stream_out).width = options.outputWidth.get();
		((ImageStream) stream_out).height = options.outputHeight.get();
		((ImageStream) stream_out).format = Cons.ImageFormat.FLEX_RGB_888.val;
	}
}
