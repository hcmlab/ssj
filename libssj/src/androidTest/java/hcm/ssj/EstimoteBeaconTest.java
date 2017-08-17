/*
 * EstimoteBeaconTest.java
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
import hcm.ssj.core.Provider;
import hcm.ssj.estimote.BeaconChannel;
import hcm.ssj.estimote.EstimoteBeacon;
import hcm.ssj.signal.Butfilt;
import hcm.ssj.signal.Merge;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 08.03.2017.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EstimoteBeaconTest
{
	@Test
	public void testBeacons() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		frame.options.logtimeout.set(0.2);

		// Sensor
		EstimoteBeacon sensor = new EstimoteBeacon();
		sensor.options.uuid.set("B9407F30-F5F8-466E-AFF9-25556B57FE6D");
		sensor.options.major.set(1337);

		// Channel
		BeaconChannel channel = new BeaconChannel();
		channel.options.identifier.set("B9407F30-F5F8-466E-AFF9-25556B57FE6D:1337:1000");
		frame.addSensor(sensor, channel);

		// Transformer
		Butfilt filter = new Butfilt();
		filter.options.zero.set(true);
		filter.options.norm.set(false);
		filter.options.low.set(0.3);
		filter.options.order.set(1);
		filter.options.type.set(Butfilt.Type.LOW);
		frame.addTransformer(filter, channel, 0.2, 0);

		Merge merge = new Merge();
		frame.addTransformer(merge, new Provider[] {channel, filter}, 0.2, 0);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, merge, 0.2, 0);

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
	}
}
