/*
 * GATTTest.java
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

import java.util.UUID;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.ioput.GATTChannel;
import hcm.ssj.ioput.GATTConnection;
import hcm.ssj.ioput.GATTReader;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 27.07.2021.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class GATTTest
{
	@Test
	public void testComponents() throws SSJException
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		GATTReader sensor = new GATTReader();
		sensor.options.macAddress.set("C4:DD:57:9E:8E:EE");

		GATTChannel channel = new GATTChannel();
		channel.options.supportedUUID.set(GATTChannel.SupportedUUIDs.TEMPERATURE);

		frame.addSensor(sensor, channel);

		Logger logger = new Logger();
		frame.addConsumer(logger, channel);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(60 * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		frame.clear();
	}

	/*
	@Test
	public void testConnection()
	{
		GATTConnection connection = new GATTConnection();
		connection.registerCharacteristic(UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb"));
		connection.registerCharacteristic(UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb"));
		connection.registerCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));

		connection.connect("C4:DD:57:9E:8E:EE");

		// Wait duration
		try
		{
			Thread.sleep(60 * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		connection.disconnect();
		connection.close();
	}
	*/
}
