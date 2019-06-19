/*
 * AndroidSensorTest.java
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

import android.content.Context;
import android.hardware.SensorManager;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class AndroidSensorTest
{
	@Test
	public void testSensors() throws Exception
	{
		// Test for every sensor type
		for (SensorType type : SensorType.values())
		{
			SensorManager mSensorManager = (SensorManager) getInstrumentation().getContext().getSystemService(Context.SENSOR_SERVICE);
			if (mSensorManager.getDefaultSensor(type.getType()) != null)
			{
				// Setup
				Pipeline frame = Pipeline.getInstance();
				frame.options.bufferSize.set(10.0f);

				// Sensor
				AndroidSensor sensor = new AndroidSensor();
				AndroidSensorChannel channel = new AndroidSensorChannel();
				channel.options.sensorType.set(type);
				frame.addSensor(sensor, channel);

				// Logger
				Logger log = new Logger();
				frame.addConsumer(log, channel, 1, 0);

				// Start framework
				frame.start();

				// Wait duration
				try
				{
					Thread.sleep(TestHelper.DUR_TEST_SHORT);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				// Stop framework
				frame.stop();
				frame.release();
			}
			else
			{
				Log.i(type.getName() + " not present on device");
			}
		}
	}
}
