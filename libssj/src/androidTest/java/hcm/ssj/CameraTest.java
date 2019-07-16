/*
 * CameraTest.java
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

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import android.view.SurfaceView;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.CameraWriter;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Pipeline;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

/**
 * Tests all camera sensor, channel and consumer.<br>
 * Created by Frank Gaibler on 28.01.2016.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraTest
{
	/**
	 * Test types
	 */
	private enum Type
	{
		WRITER, PAINTER
	}

	@Test
	public void testCameraWriter() throws Throwable
	{
		buildPipe(Type.WRITER);
	}

	@Test
	public void testCameraPainter() throws Throwable
	{
		buildPipe(Type.PAINTER);
	}

	private void buildPipe(Type type) throws Exception
	{
		// Small values because of memory usage
		int frameRate = 10;
		int width = 176;
		int height = 144;

		// Resources
		File file = null;

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraType.set(Cons.CameraType.FRONT_CAMERA);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(4 * 1000);
		cameraSensor.options.previewFpsRangeMax.set(16 * 1000);

		// Channel
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set((double) frameRate);
		frame.addSensor(cameraSensor, cameraChannel);

		// Consumer
		switch (type)
		{
			case WRITER:
			{
				//file
				File dir = getInstrumentation().getContext().getFilesDir();
				String fileName = getClass().getSimpleName() + "." + getClass().getSimpleName();
				//
				CameraWriter cameraWriter = new CameraWriter();
				cameraWriter.options.filePath.setValue(dir.getPath());
				cameraWriter.options.fileName.set(fileName);
				cameraWriter.options.width.set(width);
				cameraWriter.options.height.set(height);
				frame.addConsumer(cameraWriter, cameraChannel, 1.0 / frameRate, 0);
				break;
			}
			case PAINTER:
			{
				CameraPainter cameraPainter = new CameraPainter();
				cameraPainter.options.surfaceView.set(new SurfaceView(getInstrumentation().getContext()));
				frame.addConsumer(cameraPainter, cameraChannel, 1 / frameRate, 0);
				break;
			}
		}

		// Start framework
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

		// Stop framework
		frame.stop();
		frame.release();

		// Cleanup
		switch (type)
		{
			case WRITER:
			{
				Assert.assertTrue(file.length() > 1000);

				if (file.exists())
				{
					if (!file.delete())
					{
						throw new RuntimeException("File could not be deleted");
					}
				}
				break;
			}
		}
	}
}
