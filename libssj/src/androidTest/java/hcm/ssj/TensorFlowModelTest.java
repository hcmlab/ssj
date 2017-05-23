/*
 * TensorFlowModelTest.java
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.ml.ClassifierT;
import hcm.ssj.signal.Functionals;
import hcm.ssj.signal.Selector;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getContext;
/**
 * Created by hiwi on 22.05.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TensorFlowModelTest
{
	@Test
	public void TensorFlowModelTest() throws Exception
	{
		File dir = getContext().getFilesDir();
		String modelName = "my_model.trainer";
		TestHelper.copyAssetToFile(modelName, new File(dir, modelName));
		TestHelper.copyAssetToFile(modelName + ".PythonModel.model", new File(dir, modelName + ".PythonModel.model"));
		TestHelper.copyAssetToFile(modelName + ".PythonModel.option", new File(dir, modelName + ".PythonModel.option"));
		String outputFileName = getClass().getSimpleName() + ".test";
		File outputFile = new File(dir, outputFileName);

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		FileReader reader = new FileReader();
		reader.options.fileName.set("mouse.stream");
		SensorChannel mouse = frame.addSensor(reader, new FileReaderChannel());

		Selector sel = new Selector();
		sel.options.values.set(new int[]{0});
		frame.addTransformer(sel, mouse, 0.1, 0);

		Functionals functionals = new Functionals();
		functionals.options.energy.set(false);
		functionals.options.len.set(false);
		functionals.options.maxPos.set(false);
		functionals.options.minPos.set(false);
		functionals.options.path.set(false);
		functionals.options.peaks.set(false);
		functionals.options.range.set(false);
		functionals.options.std.set(false);
		functionals.options.zeros.set(false);

		frame.addTransformer(functionals, sel, 2.0, 0);

		// TensorFlowModel
		ClassifierT classifier = new ClassifierT();
		classifier.options.trainerPath.set(dir.getAbsolutePath());
		classifier.options.trainerFile.set(modelName);
		frame.addTransformer(classifier, functionals, 2, 0);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, classifier, 2, 0);

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

		//get data file
		File data = new File(dir, outputFileName + "~");

		//verify
		//Assert.assertTrue(outputFile.length() > 100);
		//Assert.assertTrue(data.length() > 100);

		if(outputFile.exists()) outputFile.delete();
		if(data.exists()) data.delete();
	}
}
