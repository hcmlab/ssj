/*
 * BodyTest.java
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.body.AccelerationFeatures;
import hcm.ssj.core.Pipeline;
import hcm.ssj.file.FileWriter;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getContext;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BodyTest
{
	@Test
	public void testWriting() throws Exception
	{
		//resources
		File dir = getContext().getFilesDir();
		String fileName = getClass().getSimpleName() + ".test";
		File file = new File(dir, fileName);
		String fileName2 = getClass().getSimpleName() + "2.test";
		File file2 = new File(dir, fileName2);

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		AndroidSensor sensor = new AndroidSensor();

		// Channel
		AndroidSensorChannel channel = new AndroidSensorChannel();
		channel.options.sensorType.set(SensorType.ACCELEROMETER);
		channel.options.sampleRate.set(40);
		frame.addSensor(sensor, channel);

		// Transformer
		AccelerationFeatures features = new AccelerationFeatures();
		frame.addTransformer(features, channel, 2, 2);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, features, 2, 0);

		FileWriter rawWriter = new FileWriter();
		rawWriter.options.filePath.setValue(dir.getAbsolutePath());
		rawWriter.options.fileName.set(fileName);
		frame.addConsumer(rawWriter, channel, 1, 0);

		FileWriter featureWriter = new FileWriter();
		featureWriter.options.filePath.setValue(dir.getAbsolutePath());
		featureWriter.options.fileName.set(fileName2);
		frame.addConsumer(featureWriter, features, 2, 0);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
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
		frame.clear();

		//get data files
		File data = new File(dir, fileName + "~");
		File data2 = new File(dir, fileName + "~");

		//verify
		Assert.assertTrue(file.length() > 100);
		Assert.assertTrue(file2.length() > 100);
		Assert.assertTrue(data.length() > 100);
		Assert.assertTrue(data2.length() > 100);

		if(file.exists()) file.delete();
		if(file2.exists()) file2.delete();
		if(data.exists()) data.delete();
		if(data2.exists()) data2.delete();
	}
}
