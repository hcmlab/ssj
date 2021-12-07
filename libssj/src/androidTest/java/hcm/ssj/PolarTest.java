/*
 * PolarTest.java
 * Copyright (c) 2021
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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import hcm.ssj.core.Pipeline;
import hcm.ssj.file.FileWriter;
import hcm.ssj.polar.PolarACCChannel;
import hcm.ssj.polar.PolarECGChannel;
import hcm.ssj.polar.Polar;
import hcm.ssj.polar.PolarHRChannel;
import hcm.ssj.polar.PolarPPGChannel;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 08.04.2021.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PolarTest
{
	@Test
	public void testConnection() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		Polar sensor = new Polar();
		/*
		 * H10: D3:84:E5:34:76:3C
		 * OH1: A0:9E:1A:5E:8A:A0, A0:9E:1A:93:78:3D
		 */
		sensor.options.deviceIdentifier.set("A0:9E:1A:93:78:3D");

		PolarPPGChannel channel = new PolarPPGChannel();
		// PolarACCChannel channel = new PolarACCChannel();
		// PolarHRChannel channel = new PolarHRChannel();
		//PolarECGChannel channel = new PolarECGChannel();
		frame.addSensor(sensor, channel);

		FileWriter fw = new FileWriter();
		fw.options.fileName.set("ppg");
		frame.addConsumer(fw, channel);

		Logger polarLogger = new Logger();
		frame.addConsumer(polarLogger, channel);

		// Start framework
		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_NORMAL * 5);
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