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
import android.util.Xml;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileUtils;
import hcm.ssj.ml.NaiveBayes;
import hcm.ssj.ml.OnlineNaiveBayes;

/**
 * Created by Michael Dietz on 07.11.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NaiveBayesTest
{
	private String trainerFileName = "activity.NaiveBayes.trainer";
	private String modelPath = "/sdcard/SSJ/Creator/res";

	// Helper variables
	private String modelFileName;
	private String modelOptionFileName;

	private int[] select_dimensions;
	private String[] classNames;
	private int bytes;
	private int dim;
	private float sr;
	private Cons.Type type;

	@Test
	public void compareImplementations() throws Exception
	{
		parseTrainerFile(FileUtils.getFile(modelPath, trainerFileName));

		// Select newly trained model
		//modelFileName = "tmp_activity.NaiveBayes.model";

		// Init old model
		NaiveBayes oldImpl = new NaiveBayes();
		oldImpl.setNumClasses(classNames.length);
		oldImpl.setClassNames(classNames);

		oldImpl.load(FileUtils.getFile(modelPath, modelFileName));
		oldImpl.loadOption(FileUtils.getFile(modelPath, modelOptionFileName));

		// Init new model
		OnlineNaiveBayes newImpl = new OnlineNaiveBayes();
		newImpl.setNumClasses(classNames.length);
		newImpl.setClassNames(classNames);

		newImpl.load(FileUtils.getFile(modelPath, modelFileName));
		newImpl.loadOption(FileUtils.getFile(modelPath, modelOptionFileName));

		Stream[] inputStream = new Stream[1];
		inputStream[0] = Stream.create(1, select_dimensions.length, sr, type);

		// Fill stream
		for (int i = 0; i < select_dimensions.length; i++)
		{
			inputStream[0].ptrF()[i] = 0.5f;
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

		//newImpl.save(FileUtils.getFile(modelPath, "tmp_" + modelFileName));
	}


	private void parseTrainerFile(File file) throws XmlPullParserException, IOException
	{
		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(new FileReader(file));

		parser.next();
		if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("trainer"))
		{
			Log.w("unknown or malformed trainer file");
			return;
		}

		ArrayList<String> classNamesList = new ArrayList<>();

		while (parser.next() != XmlPullParser.END_DOCUMENT)
		{

			//STREAM
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("streams"))
			{

				parser.nextTag(); //item
				if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
				{

					bytes = Integer.valueOf(parser.getAttributeValue(null, "byte"));
					dim = Integer.valueOf(parser.getAttributeValue(null, "dim"));
					sr = Float.valueOf(parser.getAttributeValue(null, "sr"));
					type = Cons.Type.valueOf(parser.getAttributeValue(null, "type"));
				}
			}

			// CLASS
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("classes"))
			{
				parser.nextTag();

				while (parser.getName().equalsIgnoreCase("item"))
				{
					if (parser.getEventType() == XmlPullParser.START_TAG)
					{
						classNamesList.add(parser.getAttributeValue(null, "name"));
					}
					parser.nextTag();
				}
			}

			//SELECT
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("select"))
			{

				parser.nextTag(); //item
				if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
				{

					int stream_id = Integer.valueOf(parser.getAttributeValue(null, "stream"));
					if (stream_id != 0)
					{
						Log.w("multiple input streams not supported");
					}
					String[] select = parser.getAttributeValue(null, "select").split(" ");
					select_dimensions = new int[select.length];
					for (int i = 0; i < select.length; i++)
					{
						select_dimensions[i] = Integer.valueOf(select[i]);
					}
				}
			}

			//MODEL
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("model"))
			{
				modelFileName = parser.getAttributeValue(null, "path") + ".model";
				modelOptionFileName = parser.getAttributeValue(null, "option") + ".option";
			}

			if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
			{
				break;
			}
		}

		classNames = classNamesList.toArray(new String[0]);
	}
}