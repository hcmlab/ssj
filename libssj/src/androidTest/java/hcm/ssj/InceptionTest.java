/*
 * InceptionTest.java
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
import android.view.SurfaceView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.CameraWriter;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ml.ClassifierT;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by Vitaly on 04.06.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InceptionTest
{
	@Test
	public void loadInceptionModel() throws Exception
	{
		File dir = getContext().getFilesDir();

		// Neural network trainer file for classifying images
		String modelName = "inception_model.trainer";

		String outputFileName = getClass().getSimpleName() + ".test";

		// Option parameters for camera sensor
		double frameRate = 10.0;
		int width = 176;
		int height = 144;

		// Load inception model and trainer file
		TestHelper.copyAssetToFile(modelName, new File(dir, modelName));
		TestHelper.copyAssetToFile(modelName + ".model", new File(dir, modelName + ".model"));

		// Get pipeline instance
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Instantiate camera sensor and set options
		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraInfo.set(Camera.CameraInfo.CAMERA_FACING_FRONT);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(4 * 1000);
		cameraSensor.options.previewFpsRangeMax.set(16 * 1000);

		// Add sensor to the pipeline
		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(frameRate);
		frame.addSensor(cameraSensor, cameraChannel);

		// Add classifier transformer to the pipeline
		ClassifierT classifier = new ClassifierT();
		classifier.options.trainerPath.set(dir.getAbsolutePath());
		classifier.options.trainerFile.set(modelName);
		classifier.options.merge.set(false);
		frame.addTransformer(classifier, cameraChannel, 2, 0);

		// Add consumer to the pipeline
		CameraWriter cameraWriter = new CameraWriter();
		cameraWriter.options.filePath.set(dir.getPath());
		cameraWriter.options.fileName.set(outputFileName);
		cameraWriter.options.width.set(width);
		cameraWriter.options.height.set(height);
		frame.addConsumer(cameraWriter, cameraChannel, 1.0 / frameRate, 0);

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
