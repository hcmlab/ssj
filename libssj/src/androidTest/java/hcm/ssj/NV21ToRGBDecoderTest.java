/*
 * NV21ToRGBDecoderTest.java
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

package hcm.ssj;

import android.hardware.Camera;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.camera.Resizer;
import hcm.ssj.core.Pipeline;
import hcm.ssj.test.Logger;

/**
 * @author Vitaly
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NV21ToRGBDecoderTest
{
	@Test
	public void decodeNV21() throws Exception
	{
		final boolean PREPARE_FOR_INCEPTION = false;
		final float BUFFER_SIZE = 10f;

		final int CROP_SIZE = 224;
		final int MIN_FPS = 15;
		final int MAX_FPS = 15;
		final int DATA_WINDOW_SIZE = 1;
		final int DATA_OVERLAP = 0;

		// Option parameters for camera sensor
		double sampleRate = 1;
		int width = 320;
		int height = 240;

		// Get pipeline instance
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(BUFFER_SIZE);

		// Instantiate camera sensor and set options
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraInfo.set(Camera.CameraInfo.CAMERA_FACING_BACK);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(MIN_FPS);
		cameraSensor.options.previewFpsRangeMax.set(MAX_FPS);

		// Add sensor to the pipeline
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(sampleRate);
		frame.addSensor(cameraSensor, cameraChannel);

		// Set up a NV21 decoder
		NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
		decoder.options.prepareForInception.set(PREPARE_FOR_INCEPTION);
		frame.addTransformer(decoder, cameraChannel, DATA_WINDOW_SIZE, DATA_OVERLAP);

		Resizer resizer = new Resizer();
		resizer.options.cropSize.set(CROP_SIZE);
		frame.addTransformer(resizer, decoder, DATA_WINDOW_SIZE, DATA_OVERLAP);

		// Add consumer to the pipeline
		Logger logger = new Logger();
		frame.addConsumer(logger, resizer, 1.0 / sampleRate, DATA_OVERLAP);

		// Start pipeline
		frame.start();

		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_SHORT;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		frame.release();
	}
}
