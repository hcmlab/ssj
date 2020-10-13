/*
 * EventTest.java
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

import java.io.File;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.audio.Intensity;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.event.FloatSegmentEventSender;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.event.ThresholdEventSender;
import hcm.ssj.event.ValueEventSender;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.ioput.SocketEventReader;
import hcm.ssj.ioput.SocketEventWriter;
import hcm.ssj.test.EventLogger;

import static androidx.test.InstrumentationRegistry.getContext;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EventTest
{
	@Test
	public void testFloatsEventSender() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		AndroidSensor sensor = new AndroidSensor();
		AndroidSensorChannel acc = new AndroidSensorChannel();
		acc.options.sensorType.set(SensorType.ACCELEROMETER);
		acc.options.sampleRate.set(40);
		frame.addSensor(sensor, acc);

		FloatsEventSender evs = new FloatsEventSender();
		evs.options.mean.set(true);
		frame.addConsumer(evs, acc, 1.0, 0);
		EventChannel channel = evs.getEventChannelOut();

		EventLogger log = new EventLogger();
		frame.registerEventListener(log, channel);

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
		frame.release();
	}


	public void testThresholds() throws Exception
	{
		File dir = getContext().getFilesDir();
		String fileName = "audio.stream";
		File header = new File(dir, fileName);
		TestHelper.copyAssetToFile(fileName, header);
		File data = new File(dir, fileName + "~");
		TestHelper.copyAssetToFile(fileName + "data", data); //android does not support "~" in asset files

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		frame.options.countdown.set(0);
		frame.options.log.set(true);

		// Sensor
		FileReader file = new FileReader();
		file.options.file.setValue(dir.getAbsolutePath() + File.separator + fileName);
		FileReaderChannel audio = new FileReaderChannel();
		audio.options.chunk.set(0.032);
		audio.setWatchInterval(0);
		audio.setSyncInterval(0);
		frame.addSensor(file, audio);

		Intensity energy = new Intensity();
		frame.addTransformer(energy, audio, 1.0, 0);

		ThresholdEventSender vad = new ThresholdEventSender();
		vad.options.thresin.set(new float[]{50.0f}); //SPL
		vad.options.mindur.set(1.0);
		vad.options.maxdur.set(9.0);
		vad.options.hangin.set(3);
		vad.options.hangout.set(5);
		Provider[] vad_in = {energy};
		frame.addConsumer(vad, vad_in, 1.0, 0);
		EventChannel vad_channel = vad.getEventChannelOut();

		FloatSegmentEventSender evs = new FloatSegmentEventSender();
		evs.options.mean.set(true);
		frame.addConsumer(evs, energy, vad_channel);
		EventChannel channel = evs.getEventChannelOut();

		EventLogger log = new EventLogger();
		frame.registerEventListener(log, channel);

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
		frame.release();
	}

	@Test
	public void testSocketEventWriter() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		AndroidSensor sensor = new AndroidSensor();
		AndroidSensorChannel acc = new AndroidSensorChannel();
		acc.options.sensorType.set(SensorType.ACCELEROMETER);
		acc.options.sampleRate.set(1);
		frame.addSensor(sensor, acc);

		ValueEventSender evs = new ValueEventSender();
		frame.addConsumer(evs, acc, 1.0, 0);
		EventChannel channel = evs.getEventChannelOut();

		SocketEventWriter sew = new SocketEventWriter();
		//sew.options.ip.set("192.168.2.102"); // Receiver IP
		sew.options.ip.set("192.168.0.237"); // Receiver IP
		sew.options.port.set(343);
		sew.options.sendAsMap.set(true);
		sew.options.mapKeys.set("f_accX,f_accY,f_accZ");

		frame.registerEventListener(sew, channel);

		SocketEventReader ser = new SocketEventReader();
		ser.options.ip.set("192.168.0.169"); // Phone IP
		ser.options.port.set(6000);
		ser.options.parseXmlToEvent.set(false);

		frame.registerEventProvider(ser);

		EventLogger el = new EventLogger();
		frame.registerEventListener(el, ser.getEventChannelOut());


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
		frame.release();
	}
}