/*
 * GPSTest.java
 * Copyright (c) 2016
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

import android.app.Application;
import android.test.ApplicationTestCase;

import hcm.ssj.core.Pipeline;
import hcm.ssj.gps.GPSChannel;
import hcm.ssj.gps.GPSSensor;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 05.07.2016.
 */
public class GPSTest extends ApplicationTestCase<Application>
{
	// Test length in milliseconds
	private final static int TEST_LENGTH = 2 * 60 * 1000;

	/**
	 *
	 */
	public GPSTest()
	{
		super(Application.class);
	}

	/**
	 * @throws Exception
	 */
	public void testSensors() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		GPSSensor sensor = new GPSSensor();


		// Channel
		GPSChannel sensorChannel = new GPSChannel();
		frame.addSensor(sensor,sensorChannel);

		// Logger
		Logger log = new Logger();
		frame.addConsumer(log, sensorChannel, 1, 0);

		// start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TEST_LENGTH;
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

		// stop framework
		frame.stop();
		frame.clear();
	}


}
