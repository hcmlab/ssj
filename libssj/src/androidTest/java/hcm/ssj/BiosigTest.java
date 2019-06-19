/*
 * BiosigTest.java
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

import hcm.ssj.biosig.GSRArousalEstimation;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.empatica.Empatica;
import hcm.ssj.empatica.GSRChannel;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BiosigTest
{
	@Test
	public void testArousal() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		GSRChannel data = new GSRChannel();
		frame.addSensor(empatica, data);

		GSRArousalEstimation arousal = new GSRArousalEstimation();
		frame.addTransformer(arousal, data, 0.25, 0);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, arousal, 0.25, 0);

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

		frame.stop();
		Log.i("arousal test finished");
	}
}
