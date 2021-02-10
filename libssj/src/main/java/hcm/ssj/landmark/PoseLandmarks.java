/*
 * PoseLandmarks.java
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

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

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
import hcm.ssj.landmark.utils.LandmarkSmoothingCalculator;
import hcm.ssj.ml.TFLiteWrapper;
import hcm.ssj.ssd.CalculatorOptions;
import hcm.ssj.ssd.Detection;
import hcm.ssj.ssd.Keypoint;
import hcm.ssj.ssd.Landmark;
import hcm.ssj.ssd.SingleShotMultiBoxDetector;
import hcm.ssj.file.FileCons;

/**
 * Created by Michael Dietz on 08.01.2021.
 */
public class PoseLandmarks extends Transformer
{
	private static final String DETECTION_MODEL_NAME = "pose_detection.tflite";
	private static final String DETECTION_MODEL_PATH = FileCons.MODELS_DIR + File.separator + DETECTION_MODEL_NAME;
	private static final int DETECTION_MODEL_INPUT_SIZE = 128;
	private static final int DETECTION_MODEL_INPUT_CHANNELS = 3;

	private static final String LANDMARK_FULL_MODEL_NAME = "pose_landmark_full_body.tflite";
	private static final String LANDMARK_UPPER_MODEL_NAME = "pose_landmark_upper_body.tflite";
	private static final int LANDMARK_MODEL_INPUT_SIZE = 256;
	private static final int LANDMARK_MODEL_INPUT_CHANNELS = 3;

	public static final int LANDMARK_NUM_FULL = 33; // 35
	public static final int LANDMARK_NUM_UPPER = 25; // 27
	public static final int LANDMARK_DIM_FULL = LANDMARK_NUM_FULL * 3; // x, y, visibility for each landmark
	public static final int LANDMARK_DIM_UPPER = LANDMARK_NUM_UPPER * 3;
	public static final int AUX_LANDMARK_NUM = 2;
	public static final double SCALE_FACTOR = 1.5;

	public class Options extends OptionList
	{
		public final Option<Integer> rotation = new Option<>("rotation", 270, Integer.class, "rotation of the input image, use 270 for front camera and 90 for back camera");
		public final Option<Boolean> onlyUpperBody = new Option<>("onlyUpperBody", false, Boolean.class, "only crop to upper body, if false crops to full body");
		public final Option<Float> poseConfidenceThreshold = new Option<>("poseConfidenceThreshold", 0.5f, Float.class, "threshold for the pose confidence score to determine whether a pose is present");
		public final Option<Boolean> useGPU = new Option<>("useGPU", true, Boolean.class, "if true tries to use GPU for better performance");

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
	private Bitmap poseBitmap;

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
	private int landmarkNum;
	private double rotationRad;
	private float poseCenterX;
	private float poseCenterY;
	private int boxSize;
	private boolean doPoseDetection;
	private boolean poseDetected;
	private boolean landmarksDetected;
	private Keypoint centerKeypoint;
	private Keypoint scaleKeypoint;
	private int outputDim;

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public PoseLandmarks()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public void init(double frame, double delta) throws SSJException
	{
		// Download pose detection model
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

		// Download pose landmark model
		String landmarkModelName = LANDMARK_FULL_MODEL_NAME;
		landmarkNum = LANDMARK_NUM_FULL;
		outputDim = LANDMARK_DIM_FULL;

		if (options.onlyUpperBody.get())
		{
			landmarkModelName = LANDMARK_UPPER_MODEL_NAME;
			landmarkNum = LANDMARK_NUM_UPPER;
			outputDim = LANDMARK_DIM_UPPER;
		}

		landmarkModelFile = new File(FileCons.MODELS_DIR + File.separator + landmarkModelName);

		if (!landmarkModelFile.exists())
		{
			try
			{
				Pipeline.getInstance().download(landmarkModelName, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
			}
			catch (IOException e)
			{
				throw new SSJException("Error while downloading landmark detection model (" + landmarkModelName + ")!", e);
			}
		}
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

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

		// Create bitmap for the original image
		inputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		rotationMatrix = new Matrix();
		rotationMatrix.postRotate(options.rotation.get());

		// Create SSD helper class
		CalculatorOptions calculatorOptions = new CalculatorOptions();
		calculatorOptions.numCoords = 12;
		calculatorOptions.numKeypoints = 4;
		calculatorOptions.minScoreThresh = 0.5;

		ssd = new SingleShotMultiBoxDetector(calculatorOptions);

		targetAngleRad = degreesToRadians(90);

		landmarkList = new ArrayList<>();
		smoothedLandmarkList = new ArrayList<>();

		doPoseDetection = true;

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

			landmarkSmoother = new LandmarkSmoothingCalculator(5, 10.0f, landmarkNum + AUX_LANDMARK_NUM, rotatedWidth, rotatedHeight);
		}

		// Reset variables
		poseDetected = false;
		landmarksDetected = false;

		// Check if pose region should be detected or if previous aux keypoints should be used
		if (doPoseDetection)
		{
			// Resize rotated original input to model input size
			detectionModelInputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, DETECTION_MODEL_INPUT_SIZE, DETECTION_MODEL_INPUT_SIZE, true);

			// Convert and normalize bitmap to model input
			detectionWrapper.convertBitmapToInputArray(detectionModelInputBitmap, detectionModelInputArray, detectionImgData);

			// Perform pose detection
			List<Detection> detectionList = detectPoseRegion(detectionImgData);

			// Recycle landmark input image
			detectionModelInputBitmap.recycle();

			// Set detection result
			poseDetected = detectionList.size() > 0;

			// Pose detected
			if (poseDetected)
			{
				currentDetection = detectionList.get(0);

				// Pose detection contains four key points: first two for full-body pose and two more for upper-body pose.
				int centerIndex = 0;
				int scaleIndex = 1;

				if (options.onlyUpperBody.get())
				{
					centerIndex = 2;
					scaleIndex = 3;
				}

				// These keypoints will be used for rotation and scaling of the input image
				centerKeypoint = currentDetection.keypoints.get(centerIndex);
				scaleKeypoint = currentDetection.keypoints.get(scaleIndex);
			}
		}
		else
		{
			// Use aux keypoints from previous landmark detection result
			poseDetected = true;
		}


		// Landmark detection on pose region
		if (poseDetected)
		{
			poseBitmap = rotateAndScaleImage(rotatedBitmap, centerKeypoint, scaleKeypoint);

			// Resize rotated and scaled input to model input size
			landmarkModelInputBitmap = Bitmap.createScaledBitmap(poseBitmap, LANDMARK_MODEL_INPUT_SIZE, LANDMARK_MODEL_INPUT_SIZE, true);

			// Convert and normalize bitmap to model input
			landmarkWrapper.convertBitmapToInputArray(landmarkModelInputBitmap, landmarkModelInputArray, landmarkImgData);

			// Perform landmark detection
			landmarksDetected = detectLandmarks(landmarkImgData, landmarkList);

			// Recycle landmark input image
			landmarkModelInputBitmap.recycle();
			poseBitmap.recycle();
		}

		// Output stream
		float[] out = stream_out.ptrF();

		if (landmarksDetected)
		{
			doPoseDetection = false;

			// Smooth landmarks
			landmarkSmoother.process(landmarkList.subList(0, landmarkNum + AUX_LANDMARK_NUM), smoothedLandmarkList);

			int outputIndex = 0;

			for (Landmark landmark : smoothedLandmarkList.subList(0, landmarkNum))
			{
				out[outputIndex++] = landmark.x / rotatedWidth;
				out[outputIndex++] = landmark.y / rotatedHeight;
				out[outputIndex++] = landmark.visibility;
			}

			// Use auxiliary landmarks as center and scale keypoints
			centerKeypoint = new Keypoint(smoothedLandmarkList.get(landmarkNum).x / rotatedWidth, smoothedLandmarkList.get(landmarkNum).y / rotatedHeight);
			scaleKeypoint = new Keypoint(smoothedLandmarkList.get(landmarkNum + 1).x / rotatedWidth, smoothedLandmarkList.get(landmarkNum + 1).y / rotatedHeight);
		}
		else
		{
			// No landmarks detected, perform pose region detection in next iteration
			doPoseDetection = true;

			// Send zeroes if no face has been recognized
			Util.fillZeroes(out, 0, outputDim);
		}

		// Free bitmap memory
		rotatedBitmap.recycle();
	}

	private List<Detection> detectPoseRegion(ByteBuffer modelInputBuffer)
	{
		// Fixed output of pose detection model
		float[][][] boxesResult = new float[1][896][12];
		float[][][] scoresResult = new float[1][896][1];

		HashMap<Integer, Object> detectionOutputs = new HashMap<>();
		detectionOutputs.put(0, boxesResult);
		detectionOutputs.put(1, scoresResult);

		// Run inference
		detectionWrapper.runMultiInputOutput(new Object[]{modelInputBuffer}, detectionOutputs);

		// Calculate detections from model results
		return ssd.process(boxesResult, scoresResult);
	}

	private Bitmap rotateAndScaleImage(Bitmap rotatedBitmap, Keypoint centerKeypoint, Keypoint scaleKeypoint)
	{
		// Calculate rotation from keypoints
		rotationRad = normalizeRadians(targetAngleRad - Math.atan2(-(scaleKeypoint.y - centerKeypoint.y), scaleKeypoint.x - centerKeypoint.x));

		poseCenterX = centerKeypoint.x;
		poseCenterY = centerKeypoint.y;

		double xCenter = centerKeypoint.x * rotatedWidth;
		double yCenter = centerKeypoint.y * rotatedHeight;

		double xScale = scaleKeypoint.x * rotatedWidth;
		double yScale = scaleKeypoint.y * rotatedHeight;

		double boxRadius = Math.sqrt((xScale - xCenter) * (xScale - xCenter) + (yScale - yCenter) * (yScale - yCenter));

		boxSize = (int) (2 * boxRadius);

		int boxSizeScaled = (int) (SCALE_FACTOR * boxSize);

		Bitmap poseBitmap = Bitmap.createBitmap(boxSizeScaled, boxSizeScaled, Bitmap.Config.ARGB_8888);

		// Fill background white
		Paint paintColor = new Paint();
		paintColor.setColor(Color.WHITE);
		paintColor.setStyle(Paint.Style.FILL);

		Canvas canvas = new Canvas(poseBitmap);
		canvas.drawPaint(paintColor);

		// Set center point to (0,0) then rotate and translate back
		Matrix matrix = new Matrix();
		matrix.postTranslate((float) -xCenter, (float) -yCenter);
		matrix.postRotate((float) -radiansToDegrees(rotationRad));
		matrix.postTranslate((float) (boxRadius * SCALE_FACTOR), (float) (boxRadius * SCALE_FACTOR));

		canvas.drawBitmap(rotatedBitmap, matrix, null);

		return poseBitmap;
	}

	private boolean detectLandmarks(ByteBuffer landmarkImgData, List<Landmark> landmarkList)
	{
		boolean detected = false;

		// Fixed output of landmark detection model
		float[][] landmarkResult = new float[1][156];
		float[][] poseFlagResult = new float[1][1];
		float[][][][] segmentationResult = new float[1][128][128][1];

		if (options.onlyUpperBody.get())
		{
			landmarkResult = new float[1][124];
		}

		HashMap<Integer, Object> landmarkOutputs = new HashMap<>();
		landmarkOutputs.put(0, landmarkResult);
		landmarkOutputs.put(1, poseFlagResult);
		landmarkOutputs.put(2, segmentationResult);

		// Run inference
		landmarkWrapper.runMultiInputOutput(new Object[] {landmarkImgData}, landmarkOutputs);

		// Set confidence that a pose is present
		float poseConfidence = poseFlagResult[0][0];

		if (poseConfidence >= options.poseConfidenceThreshold.get())
		{
			detected = true;

			// Convert landmarks
			float landmarkX;
			float landmarkY;
			float landmarkZ;
			float landmarkVisibility;

			float rotationSin = (float) Math.sin(rotationRad);
			float rotationCos = (float) Math.cos(rotationRad);

			landmarkList.clear();

			// Based on: https://github.com/google/mediapipe/blob/master/mediapipe/calculators/util/landmark_projection_calculator.cc
			for (int i = 0; i < landmarkResult[0].length; i += 4)
			{
				// Create relative landmark
				landmarkX = landmarkResult[0][i] / LANDMARK_MODEL_INPUT_SIZE;
				landmarkY = landmarkResult[0][i + 1] / LANDMARK_MODEL_INPUT_SIZE;

				// Subtract pivot point
				landmarkX = landmarkX - 0.5f;
				landmarkY = landmarkY - 0.5f;

				// Rotate point
				float newX = rotationCos * landmarkX - rotationSin * landmarkY;
				float newY = rotationSin * landmarkX + rotationCos * landmarkY;

				newX *= SCALE_FACTOR;
				newY *= SCALE_FACTOR;

				landmarkX = newX * boxSize + poseCenterX * rotatedWidth;
				landmarkY = newY * boxSize + poseCenterY * rotatedHeight;

				// Ignore landmark z for now
				landmarkZ = landmarkResult[0][i + 2] / LANDMARK_MODEL_INPUT_SIZE * boxSize;
				landmarkVisibility = (float) sigmoid(landmarkResult[0][i + 3]);

				landmarkList.add(new Landmark(landmarkX, landmarkY, landmarkZ, landmarkVisibility));
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

	public double sigmoid(double x)
	{
		return 1 / (1 + Math.exp(-x));
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
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		int dim = LANDMARK_DIM_FULL;

		if (options.onlyUpperBody.get())
		{
			dim = LANDMARK_DIM_UPPER;
		}

		return dim;
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

		String[] points = new String[] {"nose", "left eye (inner)", "left eye", "left eye (outer)",
				"right eye (inner)", "right eye", "right eye (outer)", "left ear", "right ear",
				"mouth (left)", "mouth (right)", "left shoulder", "right shoulder", "left elbow",
				"right elbow", "left wrist", "right wrist", "left pinky", "right pinky",
				"left index", "right index", "left thumb", "right thumb", "left hip", "right hip",
				"left knee", "right knee", "left ankle", "right ankle", "left heel", "right heel",
				"left foot index", "right foot index"};

		for (int i = 0, j = 0; i < stream_out.dim; i += 3, j++)
		{
			stream_out.desc[i] = points[j] + " x";
			stream_out.desc[i + 1] = points[j] + " y";
			stream_out.desc[i + 2] = points[j] + " visibility";
		}
	}
}