/*
 * SendImageTest.java
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

import android.hardware.Camera;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.ioput.BluetoothChannel;
import hcm.ssj.ioput.BluetoothConnection;
import hcm.ssj.ioput.BluetoothReader;
import hcm.ssj.ioput.BluetoothWriter;
import hcm.ssj.test.Logger;

/**
 * Created by hiwi on 22.08.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SendImageTest
{
	@Test
	public void sendImageViaBluetooth() throws SSJException
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		int imageWidth = 640;
		int imageHeight = 480;

		double sampleRate = 1;

		CameraSensor cameraSensor = new CameraSensor();
		cameraSensor.options.cameraID.set(Camera.CameraInfo.CAMERA_FACING_BACK);
		cameraSensor.options.width.set(imageWidth);
		cameraSensor.options.height.set(imageHeight);
		cameraSensor.options.previewFpsRangeMin.set(15);
		cameraSensor.options.previewFpsRangeMax.set(15);

		CameraChannel cameraChannel = new CameraChannel();
		cameraChannel.options.sampleRate.set(sampleRate);
		frame.addSensor(cameraSensor, cameraChannel);

		BluetoothWriter bluetoothWriter = new BluetoothWriter();
		bluetoothWriter.options.connectionType.set(BluetoothConnection.Type.CLIENT);
		bluetoothWriter.options.serverName.set("Hcm Lab (Galaxy S5)");
		bluetoothWriter.options.connectionName.set("image-stream");
		frame.addConsumer(bluetoothWriter, cameraChannel, sampleRate, 0);

		BluetoothReader bluetoothReader = new BluetoothReader();
		bluetoothReader.options.connectionType.set(BluetoothConnection.Type.SERVER);
		bluetoothReader.options.connectionName.set("image-stream");

		BluetoothChannel bluetoothChannel = new BluetoothChannel();
		bluetoothChannel.options.channel_id.set(0);
		bluetoothChannel.options.dim.set((int)(imageWidth * imageHeight * 1.5));
		bluetoothChannel.options.bytes.set(1);
		bluetoothChannel.options.type.set(Cons.Type.IMAGE);
		bluetoothChannel.options.sr.set(sampleRate);
		bluetoothChannel.options.num.set(50);
		frame.addSensor(bluetoothReader, bluetoothChannel);

		Logger logger = new Logger();
		logger.options.reduceNum.set(true);
		frame.addConsumer(logger, bluetoothChannel, sampleRate, 0);

		frame.registerEventListener(bluetoothReader, bluetoothWriter);

		try {
			frame.start();

			long start = System.currentTimeMillis();
			while(true)
			{
				if(System.currentTimeMillis() > start + TestHelper.DUR_TEST_SHORT)
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
