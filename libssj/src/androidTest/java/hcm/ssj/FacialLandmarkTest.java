/*
 * FacialLandmarkTest.java
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.ImageResizer;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Pipeline;
import hcm.ssj.face.FaceLandmarks;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 29.01.2019.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FacialLandmarkTest
{
	@Test
	public void testFacialLandmarks() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Option parameters for camera sensor
		double sampleRate = 1;
		int width = 640;
		int height = 480;
		int resize = 224;

		// Instantiate camera sensor and set options
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraType.set(Cons.CameraType.FRONT_CAMERA);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(15);
		cameraSensor.options.previewFpsRangeMax.set(15);

		// Add sensor to the pipeline
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(sampleRate);
		frame.addSensor(cameraSensor, cameraChannel);

		// Set up a NV21 decoder
		NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
		frame.addTransformer(decoder, cameraChannel, 1, 0);

		// Add image resizer to the pipeline
		ImageResizer resizer = new ImageResizer();
		resizer.options.rotation.set(-90);
		resizer.options.size.set(resize);
		frame.addTransformer(resizer, decoder, 1, 0);

		// Add landmark detector
		FaceLandmarks landmarkTransformer = new FaceLandmarks();
		frame.addTransformer(landmarkTransformer, resizer, 1, 0);

		// Add logger
		Logger logger = new Logger();
		frame.addConsumer(logger, landmarkTransformer, 1, 0);

		// Start pipeline
		frame.start();

		try
		{
			Thread.sleep(TestHelper.DUR_TEST_NORMAL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		frame.release();
	}
}
