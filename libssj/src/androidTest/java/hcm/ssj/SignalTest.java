/*
 * SignalTest.java
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

import java.io.File;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.biosig.HRVSpectral;
import hcm.ssj.core.Pipeline;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.signal.Derivative;
import hcm.ssj.signal.FFTfeat;
import hcm.ssj.signal.Functionals;
import hcm.ssj.signal.PSD;
import hcm.ssj.signal.Spectrogram;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SignalTest
{
	@Test
	public void testFFT() throws Exception
	{
		String[] files = null;
		files = getInstrumentation().getContext().getResources().getAssets().list("");

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

		// Sensor
		FileReader file = new FileReader();
		file.options.filePath.set(dir.getAbsolutePath());
		file.options.fileName.set(fileName);
		FileReaderChannel channel = new FileReaderChannel();
		channel.setWatchInterval(0);
		channel.setSyncInterval(0);
		frame.addSensor(file, channel);

		// Transformer
		FFTfeat fft = new FFTfeat();
		frame.addTransformer(fft, channel, 512.0 / channel.getSampleRate(), 0);

		Logger log = new Logger();
		frame.addConsumer(log, fft, 1, 0);

		// start framework
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

		// stop framework
		frame.stop();
		frame.clear();

		header.delete();
		data.delete();
	}

	@Test
	public void testDerivative() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		frame.options.countdown.set(0);

		// Sensor
		AndroidSensor sensor = new AndroidSensor();
		sensor.options.sensorType.set(SensorType.ACCELEROMETER);
		AndroidSensorChannel channel = new AndroidSensorChannel();
		channel.options.sampleRate.set(40);
		frame.addSensor(sensor, channel);

		// Transformer
		Derivative deriv = new Derivative();
		frame.addTransformer(deriv, channel, 1, 0);

		Logger log = new Logger();
		frame.addConsumer(log, deriv, 1, 0);

		// start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_SHORT;
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

	@Test
	public void testFunctionals() throws Exception
	{
		//setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		//sensor
		AndroidSensor sensor = new AndroidSensor();
		sensor.options.sensorType.set(SensorType.ACCELEROMETER);

		//channel
		AndroidSensorChannel sensorChannel = new AndroidSensorChannel();
		frame.addSensor(sensor,sensorChannel);
		//transformer
		Functionals transformer = new Functionals();
		frame.addTransformer(transformer, sensorChannel, 1, 0);
		//logger
		Logger log = new Logger();
		frame.addConsumer(log, transformer, 1, 0);
		//start framework
		frame.start();
		//run test
		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_SHORT;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		frame.stop();
		frame.release();
	}

	@Test
	public void testSpectrogram() throws Exception
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
		file.options.filePath.set(dir.getAbsolutePath());
		file.options.fileName.set(fileName);

		FileReaderChannel channel = new FileReaderChannel();
		channel.options.chunk.set(0.032);
		channel.setWatchInterval(0);
		channel.setSyncInterval(0);
		frame.addSensor(file, channel);

		// Transformer
		Spectrogram spectrogram = new Spectrogram();
		spectrogram.options.banks.set("0.003 0.040, 0.040 0.150, 0.150 0.400");
		spectrogram.options.nbanks.set(3);
		spectrogram.options.nfft.set(1024);
		spectrogram.options.dopower.set(true);
		spectrogram.options.dolog.set(false);
		frame.addTransformer(spectrogram, channel, 0.1, 0);

		HRVSpectral feat = new HRVSpectral();
		frame.addTransformer(feat, spectrogram, 0.1, 0);

		Logger log = new Logger();
		frame.addConsumer(log, feat, 0.1, 0);

		// start framework
		frame.start();

		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		// stop framework
		frame.stop();
		frame.clear();
	}

	@Test
	public void testPSD() throws Exception
	{
		//setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		//sensor
		AndroidSensor sensor = new AndroidSensor();
		sensor.options.sensorType.set(SensorType.LIGHT);

		//channel
		AndroidSensorChannel sensorChannel = new AndroidSensorChannel();
		frame.addSensor(sensor,sensorChannel);
		//transformer
		PSD transformer = new PSD();
		transformer.options.entropy.set(false);
		transformer.options.normalize.set(false);
		frame.addTransformer(transformer, sensorChannel, 1, 0);
		//logger
		Logger log = new Logger();
		frame.addConsumer(log, transformer, 1, 0);
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
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		frame.stop();
		frame.release();
	}
}
