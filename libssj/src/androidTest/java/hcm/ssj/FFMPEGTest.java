/*
 * FFMPEGTest.java
 * Copyright (c) 2017
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
import hcm.ssj.core.Cons;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ffmpeg.FFMPEGWriter;

/**
 * Created by Michael Dietz on 04.09.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FFMPEGTest
{
	int width = 640;
	int height = 480;
	int frameRate = 15;

	@Test
	public void testFFMPEGWriter() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraType.set(Cons.CameraType.BACK_CAMERA);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(frameRate * 1000);
		cameraSensor.options.previewFpsRangeMax.set(frameRate * 1000);

		// Sensor channel
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set((double) frameRate);
		frame.addSensor(cameraSensor, cameraChannel);

		// Consumer
		FFMPEGWriter ffmpegWriter = new FFMPEGWriter();
		ffmpegWriter.options.fileName.set("test.mp4");
		frame.addConsumer(ffmpegWriter, cameraChannel, 1.0 / frameRate);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_SHORT;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Stop framework
		frame.stop();
		frame.release();
	}
}
