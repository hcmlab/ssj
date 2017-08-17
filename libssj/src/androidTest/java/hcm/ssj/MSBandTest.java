/*
 * MSBandTest.java
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

import hcm.ssj.core.Pipeline;
import hcm.ssj.msband.AccelerationChannel;
import hcm.ssj.msband.AltimeterChannel;
import hcm.ssj.msband.BarometerChannel;
import hcm.ssj.msband.BrightnessChannel;
import hcm.ssj.msband.CaloriesChannel;
import hcm.ssj.msband.DistanceChannel;
import hcm.ssj.msband.GSRChannel;
import hcm.ssj.msband.GyroscopeChannel;
import hcm.ssj.msband.HeartRateChannel;
import hcm.ssj.msband.IBIChannel;
import hcm.ssj.msband.MSBand;
import hcm.ssj.msband.PedometerChannel;
import hcm.ssj.msband.SkinTempChannel;
import hcm.ssj.test.Logger;

/**
 * Tests all channels of the MS Band.<br>
 * Created by Ionut Damian
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MSBandTest
{
	@Test
	public void testChannels() throws Exception
	{
		//setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		//sensor
		MSBand sensor = new MSBand();

		frame.addConsumer(new Logger(), frame.addSensor(sensor, new DistanceChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new BarometerChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new BrightnessChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new AccelerationChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new AltimeterChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new CaloriesChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new GSRChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new GyroscopeChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new HeartRateChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new IBIChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new PedometerChannel()), 1, 0);
		frame.addConsumer(new Logger(), frame.addSensor(sensor, new SkinTempChannel()), 1, 0);

		//start framework
		frame.start();

		//run test
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

		frame.stop();
		frame.clear();
	}


}
