/*
 * BitalinoTest.java
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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.bitalino.Bitalino;
import hcm.ssj.bitalino.BitalinoChannel;
import hcm.ssj.core.Pipeline;
import hcm.ssj.signal.Merge;
import hcm.ssj.test.Logger;

/**
 * Tests all channels of the MS Band.<br>
 * Created by Ionut Damian
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BitalinoTest
{
	@Test
	public void testChannels() throws Exception
	{
		//setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		//sensor
		Bitalino sensor = new Bitalino();
		sensor.options.name.set("BITalino-17-44");
		BitalinoChannel ch[] = new BitalinoChannel[6];
		int i = 0;

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A1_EMG);
		frame.addSensor(sensor, ch[i++]);

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A2_ECG);
		frame.addSensor(sensor, ch[i++]);

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A3_EDA);
		frame.addSensor(sensor, ch[i++]);

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A4_EEG);
		frame.addSensor(sensor, ch[i++]);

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A5_ACC);
		frame.addSensor(sensor, ch[i++]);

		ch[i] = new BitalinoChannel();
		ch[i].options.channel.set(Bitalino.Channel.A6_LUX);
		frame.addSensor(sensor, ch[i++]);

		frame.addConsumer(new Logger(), frame.addTransformer(new Merge(), ch, 1, 0), 1, 0);

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
