/*
 * NaiveBayesTest.java
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.ml.NaiveBayes;
import hcm.ssj.ml.NaiveBayesOld;

/**
 * Created by Michael Dietz on 07.11.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NaiveBayesTest
{
	private String trainerFileName = "activity.NaiveBayes.trainer";
	private String modelPath = "/sdcard/SSJ/Creator/res";

	@Test
	public void compareImplementations() throws Exception
	{
		// Init old model
		NaiveBayesOld oldImpl = new NaiveBayesOld();
		oldImpl.getOptions().file.setValue(modelPath + File.separator + trainerFileName);
		oldImpl.setup();
		oldImpl.load();

		// Init new model
		NaiveBayes newImpl = new NaiveBayes();
		newImpl.getOptions().file.setValue(modelPath + File.separator + trainerFileName);
		newImpl.setup();
		newImpl.load();

		Stream inputStream = Stream.create(1, newImpl.getInputDim().length, newImpl.getInputSr(), newImpl.getInputType());

		// Fill stream
		for (int i = 0; i < newImpl.getInputDim().length; i++)
		{
			inputStream.ptrF()[i] = 0.5f;
		}

		float[] oldProbs = oldImpl.forward(inputStream);
		float[] newProbs = newImpl.forward(inputStream);

		Log.d("Old:    " + Arrays.toString(oldProbs));
		Log.d("New:    " + Arrays.toString(newProbs));

		Assert.assertArrayEquals("Different probabilities between old and new implementation!", oldProbs, newProbs, 1E-5f);

		// Train model
		newImpl.train(inputStream, "Stand");
		newImpl.train(inputStream, "Walk");
		newImpl.train(inputStream, "Jog");

		float[] trainedProbs = newImpl.forward(inputStream);
		Log.d("Trained:" + Arrays.toString(trainedProbs));
	}

	@Test
	public void batchTrainingTest() throws Exception
	{
		// Init new model
		NaiveBayes model = new NaiveBayes();
		model.setNumClasses(2);
		model.setClassNames(new String[]{"a", "b"});

		Stream trainStream = Stream.create(100, 1, 1, Cons.Type.FLOAT);

		// Fill stream
		for (int i = 0; i < trainStream.num / 2; i++)
		{
			trainStream.ptrF()[i] = 0.0f + (float) (Math.random() / 10f);
		}
		for (int i = trainStream.num / 2; i < trainStream.num; i++)
		{
			trainStream.ptrF()[i] = 0.9f + (float) (Math.random() / 10f);
		}

		Annotation anno = new Annotation();
		anno.setClasses(model.getClassNames());
		anno.addEntry(model.getClassNames()[0], 0, trainStream.num / 2 * trainStream.sr);
		anno.addEntry(model.getClassNames()[1], trainStream.num / 2 * trainStream.sr, trainStream.num * trainStream.sr);
		anno.convertToFrames(1, null, 0, 0.5);

		model.setup(anno.getClassArray(), trainStream.bytes, trainStream.dim, trainStream.sr, trainStream.type);

		//train
		model.train(trainStream, anno);

		//eval
		Stream testStream = Stream.create(1, 1, 1, Cons.Type.FLOAT);

		testStream.ptrF()[0] = 0;
		float[] probs = model.forward(testStream);
		Log.d("test for input " + testStream.ptrF()[0] + ":    " + Arrays.toString(probs));
		Assert.assertArrayEquals("unexpected result!", new float[]{1.0f, 0.0f}, probs, 1E-5f);

		testStream.ptrF()[0] = 1;
		probs = model.forward(testStream);
		Log.d("test for input " + testStream.ptrF()[0] + ":    " + Arrays.toString(probs));
		Assert.assertArrayEquals("unexpected result!", new float[]{0.0f, 1.0f}, probs, 1E-5f);
	}
}
