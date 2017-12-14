/*
 * IOputTest.java
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

import java.io.File;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.ImageNormalizer;
import hcm.ssj.camera.ImageResizer;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ioput.BluetoothChannel;
import hcm.ssj.ioput.BluetoothConnection;
import hcm.ssj.ioput.BluetoothReader;
import hcm.ssj.ioput.BluetoothWriter;
import hcm.ssj.ml.Classifier;
import hcm.ssj.ml.TensorFlow;
import hcm.ssj.test.Logger;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SendImageTest
{
	private int width = 320 * 2;
	private int height = 240 * 2;

	private float frameSize = 1;
	private int delta = 0;


	@Test
	public void testBluetoothClient() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		int minFps = 15;
		int maxFps = 15;

		double sampleRate = 1;

		String serverName = "Hcm Lab (Galaxy S5)";

		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraType.set(Cons.CameraType.BACK_CAMERA);
		cameraSensor.options.width.set(width);
		cameraSensor.options.height.set(height);
		cameraSensor.options.previewFpsRangeMin.set(minFps);
		cameraSensor.options.previewFpsRangeMax.set(maxFps);

		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(sampleRate);
		frame.addSensor(cameraSensor, cameraChannel);

		BluetoothWriter bluetoothWriter = new BluetoothWriter();
		bluetoothWriter.options.connectionType.set(BluetoothConnection.Type.CLIENT);
		bluetoothWriter.options.serverName.set(serverName);
		bluetoothWriter.options.connectionName.set("stream");
		frame.addConsumer(bluetoothWriter, cameraChannel, frameSize, delta);

		try
		{
			frame.start();

			long start = System.currentTimeMillis();
			while(true)
			{
				if(System.currentTimeMillis() > start + TestHelper.DUR_TEST_LONG)
					break;

				Thread.sleep(1);
			}

			frame.stop();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Log.i("test finished");
	}


	@Test
	public void testBluetoothServer() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		String trainerName = "inception.trainer";
		String trainerURL = "https://raw.githubusercontent.com/hcmlab/ssj/master/models";

		BluetoothReader bluetoothReader = new BluetoothReader();
		bluetoothReader.options.connectionType.set(BluetoothConnection.Type.SERVER);
		bluetoothReader.options.connectionName.set("stream");

		BluetoothChannel bluetoothChannel = new BluetoothChannel();
		bluetoothChannel.options.channel_id.set(0);
		bluetoothChannel.options.dim.set((int)(width * height * 1.5));
		bluetoothChannel.options.bytes.set(1);
		bluetoothChannel.options.type.set(Cons.Type.IMAGE);
		bluetoothChannel.options.sr.set(1.0);
		bluetoothChannel.options.num.set(1);
		bluetoothChannel.options.imageHeight.set(height);
		bluetoothChannel.options.imageWidth.set(width);
		bluetoothChannel.options.imageFormat.set(Cons.ImageFormat.NV21);

		bluetoothChannel.setSyncInterval(20);
		bluetoothChannel.setWatchInterval(10);
		frame.addSensor(bluetoothReader, bluetoothChannel);

		NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
		frame.addTransformer(decoder, bluetoothChannel, frameSize, delta);

		ImageResizer resizer = new ImageResizer();
		resizer.options.maintainAspect.set(true);
		resizer.options.size.set(224);
		resizer.options.savePreview.set(true);
		frame.addTransformer(resizer, decoder, frameSize, delta);

		// Add image pixel value normalizer to the pipeline
		ImageNormalizer imageNormalizer = new ImageNormalizer();
		imageNormalizer.options.imageMean.set(117);
		imageNormalizer.options.imageStd.set(1f);
		frame.addTransformer(imageNormalizer, resizer, frameSize, delta);

		TensorFlow tf = new TensorFlow();
		tf.options.file.setValue(trainerURL + File.separator + trainerName);
		frame.addModel(tf);

		Classifier classifier = new Classifier();
		classifier.options.merge.set(false);
		classifier.options.log.set(true);
		classifier.setModel(tf);
		frame.addConsumer(classifier, imageNormalizer, frameSize, delta);

		Logger logger = new Logger();
		frame.addConsumer(logger, bluetoothChannel);

		try
		{
			frame.start();

			long start = System.currentTimeMillis();
			while(true)
			{
				if(System.currentTimeMillis() > start + TestHelper.DUR_TEST_LONG)
					break;

				Thread.sleep(1);
			}

			frame.stop();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Log.i("test finished");
	}
}
