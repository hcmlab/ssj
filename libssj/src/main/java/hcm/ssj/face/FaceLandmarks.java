/*
 * FaceLandmarks.java
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
import android.graphics.Point;

import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import hcm.ssj.camera.CameraUtil;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;

/**
 * Created by Michael Dietz on 29.01.2019.
 */
public class FaceLandmarks extends Transformer
{
	private static final String MODEL_NAME = "shape_predictor_68_face_landmarks.dat";
	private static final String MODEL_PATH = FileCons.MODELS_DIR + File.separator + MODEL_NAME;

	/**
	 * X and Y coordinate for each of the 68 landmarks
	 */
	public static final int LANDMARK_DIMENSIONS = 68 * 2;

	public class Options extends OptionList
	{
		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private int width;
	private int height;
	private Bitmap inputBitmap;
	private int[] rgbValues;

	private FaceDet landmarkDetector;
	private List<VisionDetRet> results;


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
		if (!new File(MODEL_PATH).exists())
		{
			try
			{
				Pipeline.getInstance().download(MODEL_NAME, FileCons.REMOTE_MODEL_PATH, FileCons.MODELS_DIR, true);
			}
			catch (IOException e)
			{
				throw new SSJException("Error while downloading landmark model!", e);
			}
		}
	}

	@Override
	public synchronized void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		inputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		landmarkDetector = new FaceDet(MODEL_PATH);
	}

	@Override
	public synchronized void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Convert byte array to integer array
		rgbValues = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Convert int array to bitmap
		inputBitmap.setPixels(rgbValues, 0, width, 0, 0, width, height);

		// Call landmark detection with jni
		results = landmarkDetector.detect(inputBitmap);

		int[] out = stream_out.ptrI();

		if (results != null && results.size() > 0)
		{
			List<Point> landmarks = results.get(0).getFaceLandmarks();

			int outputIndex = 0;
			for (Point landmark : landmarks)
			{
				out[outputIndex++] = landmark.x;
				out[outputIndex++] = landmark.y;
			}
		}
		else
		{
			// Send zeroes if no face has been recognized
			Util.fillZeroes(out, 0, LANDMARK_DIMENSIONS);
		}
	}

	@Override
	public synchronized void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (landmarkDetector != null)
		{
			landmarkDetector.release();
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return LANDMARK_DIMENSIONS;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		if (stream_in[0].type != Cons.Type.IMAGE)
		{
			Log.e("unsupported input type");
		}

		return Util.sizeOf(Cons.Type.INT);
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.INT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_in[0].dim];
		System.arraycopy(stream_in[0].desc, 0, stream_out.desc, 0, stream_in[0].desc.length);
	}
}
