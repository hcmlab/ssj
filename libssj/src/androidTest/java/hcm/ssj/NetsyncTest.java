/*
 * NetsyncTest.java
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

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.test.Logger;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class NetsyncTest
{

	@Test
	public void testListen() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		frame.options.sync.set(Pipeline.SyncType.CONTINUOUS);
		frame.options.syncPort.set(55100);
		frame.options.syncHost.set("192.168.0.180");

		Microphone mic = new Microphone();
		AudioChannel audio = new AudioChannel();
		audio.options.sampleRate.set(16000);
		audio.options.scale.set(true);
		frame.addSensor(mic, audio);

		Logger dummy = new Logger();
		dummy.options.reduceNum.set(true);
		frame.addConsumer(dummy, audio, 0.1, 0);

		/*
		BluetoothWriter blw = new BluetoothWriter();
        blw.options.connectionType = BluetoothConnection.Type.CLIENT;
        blw.options.serverAddr = "60:8F:5C:F2:D0:9D";
        blw.options.connectionName = "stream";
        frame.addConsumer(blw, audio, 0.1, 0);

        FloatsEventSender fes = new FloatsEventSender();
        frame.addConsumer(fes, audio, 1.0, 0);
        EventChannel ch = frame.registerEventChannel(fes);

        BluetoothEventWriter blew = new BluetoothEventWriter();
        blew.options.connectionType = BluetoothConnection.Type.CLIENT;
        blew.options.serverAddr = "60:8F:5C:F2:D0:9D";
        blew.options.connectionName = "event";
        frame.registerEventListener(blew);
        frame.registerEventListener(blew, ch);
        */

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_LONG);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();

		Log.i("test finished");
	}

	@Test
	public void testMaster() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		frame.options.sync.set(Pipeline.SyncType.CONTINUOUS);
		frame.options.syncPort.set(55100);
		frame.options.syncHost.set(null); //this is the syncHost

		/*
		BluetoothReader blr = new BluetoothReader();
        blr.options.connectionType = BluetoothConnection.Type.SERVER;
        blr.options.connectionName = "stream";
        BluetoothChannel data = new BluetoothChannel();
        data.options.dim = 1;
        data.options.type = Cons.Type.FLOAT;
        data.options.sr = 16000;

        frame.addSensor(blr,data);
        */

		Microphone mic = new Microphone();
		AudioChannel audio = new AudioChannel();
		audio.options.sampleRate.set(16000);
		audio.options.scale.set(true);
		frame.addSensor(mic, audio);

		Logger dummy = new Logger();
		dummy.options.reduceNum.set(true);
		frame.addConsumer(dummy, audio, 0.1, 0);

		/*
		BluetoothEventReader bler = new BluetoothEventReader();
        bler.options.connectionType = BluetoothConnection.Type.SERVER;
        bler.options.connectionName = "event";
        frame.registerEventListener(bler);
        EventChannel ch = frame.registerEventChannel(bler);

        EventLogger evlog = new EventLogger();
        frame.registerEventListener(evlog);
        frame.registerEventListener(evlog, ch);
        */

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
		Log.i("test finished");
	}
}