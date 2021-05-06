/*
 * FaceLandmarks.java
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

package hcm.ssj.landmark;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;

import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.landmark.utils.LandmarkSmoothingCalculator;
import hcm.ssj.ml.TFLiteWrapper;
import hcm.ssj.ssd.CalculatorOptions;
import hcm.ssj.ssd.Detection;
import hcm.ssj.ssd.Keypoint;
import hcm.ssj.ssd.Landmark;
import hcm.ssj.ssd.SingleShotMultiBoxDetector;

/**
 * Created by Michael Dietz on 08.01.2021.
 */
public class FaceLandmarks extends Transformer
{
	private static final String LEGACY_MODEL_NAME = "shape_predictor_68_face_landmarks.dat";
	private static final String LEGACY_MODEL_PATH = FileCons.MODELS_DIR + File.separator + LEGACY_MODEL_NAME;
	private static final int LEGACY_MODEL_INPUT_SIZE = 224;
	private static final int LEGACY_LANDMARK_NUM = 68;
	private static final int LEGACY_OUTPUT_DIM = LEGACY_LANDMARK_NUM * 2;  // x, y for each landmark

	private static final String DETECTION_MODEL_NAME = "face_detection_front.tflite";
	private static final String DETECTION_MODEL_PATH = FileCons.MODELS_DIR + File.separator + DETECTION_MODEL_NAME;
	private static final int DETECTION_MODEL_INPUT_SIZE = 128;
	private static final int DETECTION_MODEL_INPUT_CHANNELS = 3;

	private static final String LANDMARK_MODEL_NAME = "face_landmark.tflite";
	private static final String LANDMARK_MODEL_PATH = FileCons.MODELS_DIR + File.separator + LANDMARK_MODEL_NAME;
	private static final int LANDMARK_MODEL_INPUT_SIZE = 192;
	private static final int LANDMARK_MODEL_INPUT_CHANNELS = 3;

	private static final int LANDMARK_NUM = 468;
	private static final int LANDMARK_DIM = LANDMARK_NUM * 3; // x, y, z for each landmark
	private static final int LANDMARK_OUTPUT_DIM = LANDMARK_NUM * 2; // x, y for each landmark
	private static final double SCALE_FACTOR = 1.5;

	public class Options extends OptionList
	{
		public final Option<Integer> rotation = new Option<>("rotation", 270, Integer.class, "rotation of the input image, use 270 for front camera and 90 for back camera");
		public final Option<Float> faceConfidenceThreshold = new Option<>("faceConfidenceThreshold", 0.5f, Float.class, "threshold for the face confidence score to determine whether a face is present");
		public final Option<Boolean> useGPU = new Option<>("useGPU", true, Boolean.class, "if true tries to use GPU for better performance");
		public final Option<Boolean> useLegacyModel = new Option<>("useLegacyModel", false, Boolean.class, "if true uses old landmark detection model");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	// Helper class to postprocess classifier results
	private SingleShotMultiBoxDetector ssd;

	private TFLiteWrapper detectionWrapper;
	private TFLiteWrapper landmarkWrapper;

	// ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
	private ByteBuffer detectionImgData = null;
	private ByteBuffer landmarkImgData = null;

	private int[] originalInputArray;
	private int[] detectionModelInputArray;
	private int[] landmarkModelInputArray;

	// Create bitmap for the original image
	private Bitmap inputBitmap;

	// Rotated bitmap for original image
	private Bitmap rotatedBitmap;
	private Bitmap detectionModelInputBitmap;
	private Bitmap landmarkModelInputBitmap;
	private Bitmap faceBitmap;

	private Matrix rotationMatrix;

	private int rotatedWidth;
	private int rotatedHeight;

	private Detection currentDetection;

	private double targetAngleRad;

	private int width;
	private int height;
	private File detectionModelFile;
	private File landmarkModelFile;

	private List<Landmark> landmarkList;
	private List<Landmark> smoothedLandmarkList;
	private LandmarkSmoothingCalculator landmarkSmoother;
	private double rotationRad;
	private float faceCenterX;
	private float faceCenterY;
	private int boxSize;
	private boolean faceDetected;
	private boolean landmarksDetected;
	private Keypoint leftKeypoint;
	private Keypoint rightKeypoint;

	private FaceDet legacyLandmarkDetector;
	private List<VisionDetRet> legacyResults;

	private int outputDim;

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public FaceLandmarks()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public void init(double frame, double delta) throws SSJException
	{
		// Download face detection model
		detectionModelFile = new File(DETECTION_MODEL_PATH);

		if (!detectionModelFile.exists())
		{
			try
			{
				Pipeline.getInstance().download(DETECTION_MODEL_NAME, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
			}
			catch (IOException e)
			{
				throw new SSJException("Error while downloading face detection model!", e);
			}
		}

		// Download face landmark model
		landmarkModelFile = new File(LANDMARK_MODEL_PATH);

		if (!landmarkModelFile.exists())
		{
			try
			{
				Pipeline.getInstance().download(LANDMARK_MODEL_NAME, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
			}
			catch (IOException e)
			{
				throw new SSJException("Error while downloading landmark detection model (" + LANDMARK_MODEL_NAME + ")!", e);
			}
		}

		if (options.useLegacyModel.get())
		{
			outputDim = LEGACY_OUTPUT_DIM;

			// Download legacy detection model
			if (!new File(LEGACY_MODEL_PATH).exists())
			{
				try
				{
					Pipeline.getInstance().download(LEGACY_MODEL_NAME, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
				}
				catch (IOException e)
				{
					throw new SSJException("Error while downloading legacy face landmark model!", e);
				}
			}
		}
		else
		{
			outputDim = LANDMARK_OUTPUT_DIM;
		}
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		// Create bitmap for the original image
		inputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		if (options.useLegacyModel.get())
		{
			legacyLandmarkDetector = new FaceDet(LEGACY_MODEL_PATH);
		}

		// Create TFLite Wrappers
		detectionWrapper = new TFLiteWrapper(options.useGPU.get());
		landmarkWrapper = new TFLiteWrapper(options.useGPU.get());

		// Get TFLite interpreter options
		Interpreter.Options interpreterOptions = detectionWrapper.getInterpreterOptions();

		// Load TFLite models
		detectionWrapper.loadModel(detectionModelFile, interpreterOptions);
		landmarkWrapper.loadModel(landmarkModelFile, interpreterOptions);

		// Initialize model input buffer: size = width * height * channels * bytes per pixel (e.g., 4 for float)
		detectionImgData = ByteBuffer.allocateDirect(DETECTION_MODEL_INPUT_SIZE * DETECTION_MODEL_INPUT_SIZE * DETECTION_MODEL_INPUT_CHANNELS * Util.sizeOf(Cons.Type.FLOAT));
		detectionImgData.order(ByteOrder.nativeOrder());

		landmarkImgData = ByteBuffer.allocateDirect(LANDMARK_MODEL_INPUT_SIZE * LANDMARK_MODEL_INPUT_SIZE * LANDMARK_MODEL_INPUT_CHANNELS * Util.sizeOf(Cons.Type.FLOAT));
		landmarkImgData.order(ByteOrder.nativeOrder());

		// Initialize model input and output integer arrays
		detectionModelInputArray = new int[DETECTION_MODEL_INPUT_SIZE * DETECTION_MODEL_INPUT_SIZE];
		landmarkModelInputArray = new int[LANDMARK_MODEL_INPUT_SIZE * LANDMARK_MODEL_INPUT_SIZE];

		rotationMatrix = new Matrix();
		rotationMatrix.postRotate(options.rotation.get());

		// Create SSD helper class
		CalculatorOptions calculatorOptions = new CalculatorOptions();
		calculatorOptions.minScoreThresh = 0.5;

		ssd = new SingleShotMultiBoxDetector(calculatorOptions);

		targetAngleRad = degreesToRadians(0);

		landmarkList = new ArrayList<>();
		smoothedLandmarkList = new ArrayList<>();

		rotatedWidth = -1;
		rotatedHeight = -1;
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Convert byte array to integer array
		originalInputArray = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Create bitmap from byte array
		inputBitmap.setPixels(originalInputArray, 0, width, 0, 0, width, height);

		// Rotate original input
		rotatedBitmap = Bitmap.createBitmap(inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), rotationMatrix, true);

		// Cache width and height of rotated input image
		if (rotatedWidth < 0)
		{
			rotatedWidth = rotatedBitmap.getWidth();
			rotatedHeight = rotatedBitmap.getHeight();

			landmarkSmoother = new LandmarkSmoothingCalculator(5, 10.0f, LANDMARK_NUM, rotatedWidth, rotatedHeight);
		}

		// Reset variables
		faceDetected = false;
		landmarksDetected = false;

		// Resize rotated original input to model input size
		detectionModelInputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, DETECTION_MODEL_INPUT_SIZE, DETECTION_MODEL_INPUT_SIZE, true);

		// Convert and normalize bitmap to model input
		detectionWrapper.convertBitmapToInputArray(detectionModelInputBitmap, detectionModelInputArray, detectionImgData);

		// Perform face detection
		List<Detection> detectionList = detectFaceRegion(detectionImgData);

		// Recycle landmark input image
		detectionModelInputBitmap.recycle();

		// Set detection result
		faceDetected = detectionList.size() > 0;

		// Face detected
		if (faceDetected)
		{
			currentDetection = detectionList.get(0);

			// Face detection contains two key points: left eye and right eye
			leftKeypoint = currentDetection.keypoints.get(0);
			rightKeypoint = currentDetection.keypoints.get(1);

			// These keypoints will be used for rotation and scaling of the input image
			faceBitmap = rotateAndScaleImage(rotatedBitmap, currentDetection, leftKeypoint, rightKeypoint);

			if (options.useLegacyModel.get())
			{
				// Resize rotated and scaled input to model input size
				landmarkModelInputBitmap = Bitmap.createScaledBitmap(faceBitmap, LEGACY_MODEL_INPUT_SIZE, LEGACY_MODEL_INPUT_SIZE, true);

				// Perform legacy landmark detection
				landmarksDetected = detectLegacyLandmarks(landmarkModelInputBitmap, landmarkList);
			}
			else
			{
				// Resize rotated and scaled input to model input size
				landmarkModelInputBitmap = Bitmap.createScaledBitmap(faceBitmap, LANDMARK_MODEL_INPUT_SIZE, LANDMARK_MODEL_INPUT_SIZE, true);

				// Convert and normalize bitmap to model input
				landmarkWrapper.convertBitmapToInputArray(landmarkModelInputBitmap, landmarkModelInputArray, landmarkImgData, 0, 255.0f);

				// Perform landmark detection
				landmarksDetected = detectLandmarks(landmarkImgData, landmarkList);
			}

			// Recycle landmark input image
			landmarkModelInputBitmap.recycle();
			faceBitmap.recycle();
		}

		// Output stream
		float[] out = stream_out.ptrF();

		if (landmarksDetected)
		{
			// Smooth landmarks
			// landmarkSmoother.process(landmarkList, smoothedLandmarkList);

			int outputIndex = 0;

			for (Landmark landmark : landmarkList)
			{
				out[outputIndex++] = landmark.x / rotatedWidth;
				out[outputIndex++] = landmark.y / rotatedHeight;
				// out[outputIndex++] = landmark.visibility;
			}
		}
		else
		{
			// Send zeroes if no face has been recognized
			Util.fillZeroes(out, 0, outputDim);
		}

		// Free bitmap memory
		rotatedBitmap.recycle();
	}

	private List<Detection> detectFaceRegion(ByteBuffer modelInputBuffer)
	{
		// Fixed output of blazeface model
		float[][][] boxesResult = new float[1][896][16];
		float[][][] scoresResult = new float[1][896][1];

		HashMap<Integer, Object> detectionOutputs = new HashMap<>();
		detectionOutputs.put(0, boxesResult);
		detectionOutputs.put(1, scoresResult);

		// Run inference
		detectionWrapper.runMultiInputOutput(new Object[]{modelInputBuffer}, detectionOutputs);

		// Calculate detections from model results
		return ssd.process(boxesResult, scoresResult);
	}

	private Bitmap rotateAndScaleImage(Bitmap rotatedBitmap, Detection currentDetection, Keypoint leftKeypoint, Keypoint rightKeypoint)
	{
		// Calculate rotation from keypoints
		rotationRad = normalizeRadians(targetAngleRad - Math.atan2(-(rightKeypoint.y - leftKeypoint.y), rightKeypoint.x - leftKeypoint.x));

		faceCenterX = currentDetection.xMin + currentDetection.width / 2.0f;
		faceCenterY = currentDetection.yMin + currentDetection.height / 2.0f;

		double xCenter = faceCenterX * rotatedWidth;
		double yCenter = faceCenterY * rotatedHeight;

		double boxRadius = Math.max(currentDetection.width * rotatedWidth, currentDetection.height * rotatedHeight) / 2.0f;

		boxSize = (int) (2 * boxRadius);

		int boxSizeScaled = (int) (SCALE_FACTOR * boxSize);

		Bitmap faceBitmap = Bitmap.createBitmap(boxSizeScaled, boxSizeScaled, Bitmap.Config.ARGB_8888);

		// Fill background white
		Paint paintColor = new Paint();
		paintColor.setColor(Color.WHITE);
		paintColor.setStyle(Paint.Style.FILL);

		Canvas canvas = new Canvas(faceBitmap);
		canvas.drawPaint(paintColor);

		// Set center point to (0,0) then rotate and translate back
		Matrix matrix = new Matrix();
		matrix.postTranslate((float) -xCenter, (float) -yCenter);
		matrix.postRotate((float) -radiansToDegrees(rotationRad));
		matrix.postTranslate((float) (boxRadius * SCALE_FACTOR), (float) (boxRadius * SCALE_FACTOR));

		canvas.drawBitmap(rotatedBitmap, matrix, null);

		return faceBitmap;
	}

	private boolean detectLandmarks(ByteBuffer landmarkImgData, List<Landmark> landmarkList)
	{
		boolean detected = false;

		// Fixed output of landmark detection model
		float[][][][] landmarkResult = new float[1][1][1][LANDMARK_DIM];
		float[][][][] faceFlagResult = new float[1][1][1][1];

		HashMap<Integer, Object> landmarkOutputs = new HashMap<>();
		landmarkOutputs.put(0, landmarkResult);
		landmarkOutputs.put(1, faceFlagResult);

		// Run inference
		landmarkWrapper.runMultiInputOutput(new Object[] {landmarkImgData}, landmarkOutputs);

		// Set confidence that a face is present
		float faceConfidence = faceFlagResult[0][0][0][0];

		if (faceConfidence >= options.faceConfidenceThreshold.get())
		{
			detected = true;

			// Convert landmarks
			float landmarkX;
			float landmarkY;
			float landmarkZ;

			float rotationSin = (float) Math.sin(rotationRad);
			float rotationCos = (float) Math.cos(rotationRad);

			landmarkList.clear();

			// Based on: https://github.com/google/mediapipe/blob/master/mediapipe/calculators/util/landmark_projection_calculator.cc
			for (int i = 0; i < landmarkResult[0][0][0].length; i += 3)
			{
				// Create relative landmark
				landmarkX = landmarkResult[0][0][0][i] / LANDMARK_MODEL_INPUT_SIZE;
				landmarkY = landmarkResult[0][0][0][i + 1] / LANDMARK_MODEL_INPUT_SIZE;

				// Subtract pivot point
				landmarkX = landmarkX - 0.5f;
				landmarkY = landmarkY - 0.5f;

				// Rotate point
				float newX = rotationCos * landmarkX - rotationSin * landmarkY;
				float newY = rotationSin * landmarkX + rotationCos * landmarkY;

				newX *= SCALE_FACTOR;
				newY *= SCALE_FACTOR;

				landmarkX = newX * boxSize + faceCenterX * rotatedWidth;
				landmarkY = newY * boxSize + faceCenterY * rotatedHeight;

				// Ignore landmark z for now
				landmarkZ = landmarkResult[0][0][0][i + 2] / LANDMARK_MODEL_INPUT_SIZE * boxSize;

				landmarkList.add(new Landmark(landmarkX, landmarkY, landmarkZ));
			}
		}

		return detected;
	}

	private boolean detectLegacyLandmarks(Bitmap landmarkModelInputBitmap, List<Landmark> landmarkList)
	{
		boolean detected = false;

		// Call landmark detection with jni
		// Based on https://github.com/tzutalin/dlib-android-app
		legacyResults = legacyLandmarkDetector.detect(landmarkModelInputBitmap);

		if (legacyResults != null && legacyResults.size() > 0)
		{
			detected = true;

			// Convert landmarks
			float landmarkX;
			float landmarkY;

			float rotationSin = (float) Math.sin(rotationRad);
			float rotationCos = (float) Math.cos(rotationRad);

			landmarkList.clear();

			List<Point> landmarks = legacyResults.get(0).getFaceLandmarks();

			for (Point landmark : landmarks)
			{
				// Create relative landmark
				landmarkX = landmark.x / (float) LEGACY_MODEL_INPUT_SIZE;
				landmarkY = landmark.y / (float) LEGACY_MODEL_INPUT_SIZE;

				// Subtract pivot point
				landmarkX = landmarkX - 0.5f;
				landmarkY = landmarkY - 0.5f;

				// Rotate point
				float newX = rotationCos * landmarkX - rotationSin * landmarkY;
				float newY = rotationSin * landmarkX + rotationCos * landmarkY;

				newX *= SCALE_FACTOR;
				newY *= SCALE_FACTOR;

				landmarkX = newX * boxSize + faceCenterX * rotatedWidth;
				landmarkY = newY * boxSize + faceCenterY * rotatedHeight;

				landmarkList.add(new Landmark(landmarkX, landmarkY));
			}
		}

		return detected;
	}

	public double degreesToRadians(double degrees)
	{
		return degrees * Math.PI / 180.0;
	}

	public double radiansToDegrees(double radians)
	{
		return radians * 180.0 / Math.PI;
	}

	public double normalizeRadians(double angleRad)
	{
		return angleRad - 2 * Math.PI * Math.floor((angleRad - (-Math.PI)) / (2 * Math.PI));
	}

	@Override
	public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Clean up
		if (detectionWrapper != null)
		{
			detectionWrapper.close();
		}

		if (landmarkWrapper != null)
		{
			landmarkWrapper.close();
		}

		if (legacyLandmarkDetector != null)
		{
			legacyLandmarkDetector.release();
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return outputDim;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		if (stream_in[0].type != Cons.Type.IMAGE)
		{
			Log.e("unsupported input type");
		}

		return Util.sizeOf(Cons.Type.FLOAT);
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		// TODO set description
		stream_out.desc[0] = "Landmark X";
		stream_out.desc[1] = "Landmark Y";
	}
}