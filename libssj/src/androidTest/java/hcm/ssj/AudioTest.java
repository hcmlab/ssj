/*
 * AudioTest.java
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.AudioWriter;
import hcm.ssj.audio.Intensity;
import hcm.ssj.audio.Microphone;
import hcm.ssj.audio.Pitch;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.event.ThresholdEventSender;
import hcm.ssj.signal.Avg;
import hcm.ssj.test.EventLogger;

import static android.support.test.InstrumentationRegistry.getContext;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AudioTest
{
	@Test
	public void testWriter() throws Exception
	{
		// Resources
		File dir = getContext().getFilesDir();
		String fileName = getClass().getSimpleName() + ".test";
		File file = new File(dir, fileName);

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		Microphone microphone = new Microphone();
		AudioChannel audio = new AudioChannel();
		audio.options.sampleRate.set(8000);
		audio.options.scale.set(true);
		frame.addSensor(microphone, audio);

		// Consumer
		AudioWriter audioWriter = new AudioWriter();
		audioWriter.options.filePath.setValue(dir.getPath());
		audioWriter.options.fileName.set(fileName);
		frame.addConsumer(audioWriter, audio, 1, 0);

		// Start framework
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

		// Stop framework
		frame.stop();
		frame.release();

		// Verify test
		Assert.assertTrue(file.exists());
		Assert.assertTrue(file.length() > 1000); //1Kb

		// Cleanup
		if (file.exists())
		{
			if (!file.delete())
			{
				throw new RuntimeException("File could not be deleted");
			}
		}

	}

	@Test
	public void testSpeechrate() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);
		frame.options.log.set(true);

		Microphone mic = new Microphone();
		AudioChannel audio = new AudioChannel();
		audio.options.sampleRate.set(8000);
		audio.options.scale.set(true);
		audio.options.chunk.set(0.1);
		frame.addSensor(mic, audio);

		Pitch pitch = new Pitch();
		pitch.options.detector.set(Pitch.YIN);
		pitch.options.computeVoicedProb.set(true);
		frame.addTransformer(pitch, audio, 0.1, 0);

		Avg pitch_env = new Avg();
		frame.addTransformer(pitch_env, pitch, 0.1, 0);

		Intensity energy = new Intensity();
		frame.addTransformer(energy, audio, 1.0, 0);

		//VAD
		ThresholdEventSender vad = new ThresholdEventSender();
		vad.options.thresin.set(new float[]{50.0f}); //SPL
		vad.options.mindur.set(1.0);
		vad.options.maxdur.set(9.0);
		vad.options.hangin.set(3);
		vad.options.hangout.set(5);
		Provider[] vad_in = {energy};
		frame.addConsumer(vad, vad_in, 1.0, 0);
		EventChannel vad_channel = vad.getEventChannelOut();

		hcm.ssj.audio.SpeechRate sr = new hcm.ssj.audio.SpeechRate();
		sr.options.thresholdVoicedProb.set(0.3f);
		Provider[] sr_in = {energy, pitch_env};
		frame.addConsumer(sr, sr_in, vad_channel);
		EventChannel sr_channel = frame.registerEventProvider(sr);

		EventLogger log = new EventLogger();
		frame.registerEventListener(log, sr_channel);

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
	}
}