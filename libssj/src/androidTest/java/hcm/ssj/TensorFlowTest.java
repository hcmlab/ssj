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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.ml.ClassifierT;
import hcm.ssj.signal.Functionals;
import hcm.ssj.signal.MvgNorm;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getContext;
/**
 * Created by hiwi on 22.05.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TensorFlowTest
{
	@Test
	public void TensorFlowModelTest() throws Exception
	{
		File dir = getContext().getFilesDir();

		String modelName = "my_model.trainer";
		String mouseStreamName = "mouse.stream";

		// Load trainer files
		TestHelper.copyAssetToFile(modelName, new File(dir, modelName));
		TestHelper.copyAssetToFile(modelName + ".PythonModel.model", new File(dir, modelName + ".PythonModel.model"));
		TestHelper.copyAssetToFile(modelName + ".PythonModel.option", new File(dir, modelName + ".PythonModel.option"));

		// Load mouse stream data
		TestHelper.copyAssetToFile(mouseStreamName, new File(dir, mouseStreamName));
		TestHelper.copyAssetToFile(mouseStreamName + "data", new File(dir, mouseStreamName + '~'));

		String outputFileName = getClass().getSimpleName() + ".test";
		File outputFile = new File(dir, outputFileName);

		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		FileReader reader = new FileReader();
		reader.options.fileName.set(mouseStreamName);
		reader.options.filePath.set(dir.getAbsolutePath());
		SensorChannel mouse = frame.addSensor(reader, new FileReaderChannel());

		Functionals functionals = new Functionals();
		frame.addTransformer(functionals, mouse, 2.0, 0);

		MvgNorm norm = new MvgNorm();
		norm.options.norm.set(MvgNorm.Norm.MIN_MAX);
		norm.options.windowSize.set(30f);
		frame.addTransformer(norm, functionals, 2.0, 0);

		// TensorFlowModel
		ClassifierT classifier = new ClassifierT();
		classifier.options.trainerPath.set(dir.getAbsolutePath());
		classifier.options.trainerFile.set(modelName);
		frame.addTransformer(classifier, norm, 2, 0);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, classifier, 2, 0);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TestHelper.DUR_TEST_LONG;
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

		if(outputFile.exists()) outputFile.delete();
		if(data.exists()) data.delete();
	}
}
