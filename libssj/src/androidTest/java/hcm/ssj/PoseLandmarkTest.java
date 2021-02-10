/*
 * LandmarkTest.java
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

package hcm.ssj;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Pipeline;
import hcm.ssj.landmark.PoseLandmarks;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 04.02.2021.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PoseLandmarkTest
{
	@Test
	public void testPoseLandmarks() throws Exception
	{
		// Get pipeline instance
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Instantiate camera sensor and set options
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraType.set(Cons.CameraType.FRONT_CAMERA);
		cameraSensor.options.width.set(640);
		cameraSensor.options.height.set(480);
		cameraSensor.options.previewFpsRangeMin.set(15);
		cameraSensor.options.previewFpsRangeMax.set(15);

		// Add sensor to the pipeline
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(10.0);
		frame.addSensor(cameraSensor, cameraChannel);

		// Set up a NV21 decoder
		NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
		frame.addTransformer(decoder, cameraChannel);

		// Add pose landmarks
		PoseLandmarks poseLandmarks = new PoseLandmarks();
		poseLandmarks.options.rotation.set(0);
		frame.addTransformer(poseLandmarks, decoder);

		Logger log = new Logger();
		frame.addConsumer(log, poseLandmarks);

		// Start pipeline
		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_NORMAL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Stop pipeline
		frame.stop();
		frame.release();
	}
}
