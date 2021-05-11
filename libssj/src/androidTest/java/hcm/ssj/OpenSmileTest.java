/*
 * OpenSmileTest.java
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

import android.os.Environment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.opensmile.OpenSmileFeatures;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 05.05.2021.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class OpenSmileTest
{
	@Test
	public void testOpenSMILE() throws SSJException
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		Microphone microphone = new Microphone();
		AudioChannel audio = new AudioChannel();
		audio.options.sampleRate.set(16000);
		audio.options.chunk.set(0.1);
		audio.options.scale.set(true);
		frame.addSensor(microphone, audio);

		OpenSmileFeatures openSmileFeatures = new OpenSmileFeatures();
		openSmileFeatures.options.showLog.set(true);
		frame.addTransformer(openSmileFeatures, audio);

		/*
		OpenSmileFeatures openSmileFeatures2 = new OpenSmileFeatures();
		openSmileFeatures2.options.configFile.set(new FilePath(new File(Environment.getExternalStorageDirectory(), "SSJ").getPath() + File.separator + "configs" + File.separator + "opensmile_custom.conf"));
		openSmileFeatures2.options.featureCount.set(3);
		frame.addTransformer(openSmileFeatures2, audio);
		*/

		Logger logger = new Logger();
		frame.addConsumer(logger, new Provider[] {openSmileFeatures});

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
	}
}
