/*
 * FeedbackCollectionTest.java
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.body.OverallActivation;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.feedback.AndroidTactileFeedback;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.signal.MvgAvgVar;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FeedbackCollectionTest
{
	private Pipeline pipeline;
	private FeedbackCollection feedbackCollection;
	private List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackList;

	@Before
	public void prepareEnvironment() throws SSJException
	{
		pipeline = Pipeline.getInstance();
		pipeline.options.countdown.set(0);

		AndroidSensor sensor = new AndroidSensor();
		AndroidSensorChannel sensorChannel = new AndroidSensorChannel();
		sensorChannel.options.sensorType.set(SensorType.LINEAR_ACCELERATION);
		pipeline.addSensor(sensor, sensorChannel);

		OverallActivation overallActivation = new OverallActivation();
		pipeline.addTransformer(overallActivation, sensorChannel);

		MvgAvgVar mvgAvgVar = new MvgAvgVar();
		mvgAvgVar.options.window.set(0.05);
		pipeline.addTransformer(mvgAvgVar, overallActivation);

		FloatsEventSender floatsEventSender = new FloatsEventSender();
		// TODO: SET thresholds
		pipeline.addConsumer(floatsEventSender, mvgAvgVar);


		feedbackCollection = new FeedbackCollection();
		feedbackCollection.options.progression.set(2.0f);
		feedbackCollection.options.regression.set(3.0f);
		pipeline.registerEventListener(feedbackCollection, floatsEventSender);
		// LEVEL 0
		AndroidTactileFeedback androidTactileFeedback1 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback1, feedbackCollection, 0, FeedbackCollection.LevelBehaviour.Regress);
		AndroidTactileFeedback androidTactileFeedback2 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback2, feedbackCollection, 0, FeedbackCollection.LevelBehaviour.Neutral);
		AndroidTactileFeedback androidTactileFeedback3 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback3, feedbackCollection, 0, FeedbackCollection.LevelBehaviour.Progress);
		// LEVEL 1
		AndroidTactileFeedback androidTactileFeedback4 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback4, feedbackCollection, 1, FeedbackCollection.LevelBehaviour.Regress);
		AndroidTactileFeedback androidTactileFeedback5 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback5, feedbackCollection, 1, FeedbackCollection.LevelBehaviour.Neutral);
		AndroidTactileFeedback androidTactileFeedback6 = new AndroidTactileFeedback();
		pipeline.registerInFeedbackCollection(androidTactileFeedback6, feedbackCollection, 1, FeedbackCollection.LevelBehaviour.Progress);

		feedbackList = feedbackCollection.getFeedbackList();

	}

	@Test
	public void testNoProgression()
	{
		try
		{
			pipeline.start();

			Thread.sleep((int) ((feedbackCollection.options.progression.get() * 1000) * 1.5));

			pipeline.stop();
			pipeline.release();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		checkLevelActive(0);
	}

	@Test
	public void testProgression()
	{

		for (Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourEntry : feedbackList.get(0).entrySet())
		{
			if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackCollection.LevelBehaviour.Regress))
			{
				((AndroidTactileFeedback) feedbackLevelBehaviourEntry.getKey()).options.lock.set((int) (feedbackCollection.options.progression.get() * 1000) * 2);
			}
		}

		try
		{
			pipeline.start();

			Thread.sleep((int) ((feedbackCollection.options.progression.get() * 1000) * 1.5));

			pipeline.stop();
			pipeline.release();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		checkLevelActive(1);
	}

	@Test
	public void testNoRegression()
	{
		for (Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourEntry : feedbackList.get(0).entrySet())
		{
			if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackCollection.LevelBehaviour.Regress))
			{
				((AndroidTactileFeedback) feedbackLevelBehaviourEntry.getKey()).options.lock.set((int) (feedbackCollection.options.progression.get() * 1000) * 2);
			}
		}
		try
		{
			pipeline.start();

			Thread.sleep((int) ((feedbackCollection.options.progression.get() * 1000) * 1.5));

			checkLevelActive(1);

			Thread.sleep((int) ((feedbackCollection.options.regression.get() * 1000) * 1.5));

			pipeline.stop();
			pipeline.release();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		checkLevelActive(1);
	}

	@Test
	public void testRegression()
	{
		for (Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourEntry : feedbackList.get(0).entrySet())
		{
			if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackCollection.LevelBehaviour.Regress))
			{
				((AndroidTactileFeedback) feedbackLevelBehaviourEntry.getKey()).options.lock.set((int) (feedbackCollection.options.progression.get() * 1000) * 2);
			}
		}
		for (Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourEntry : feedbackList.get(1).entrySet())
		{
			if (feedbackLevelBehaviourEntry.getValue().equals(FeedbackCollection.LevelBehaviour.Progress))
			{
				((AndroidTactileFeedback) feedbackLevelBehaviourEntry.getKey()).options.lock.set((int) (feedbackCollection.options.regression.get() * 1000) * 2);
			}
		}

		try
		{
			pipeline.start();

			Thread.sleep((int) (feedbackCollection.options.progression.get() * 1000) + 200);

			checkLevelActive(1);

			Thread.sleep((int) (feedbackCollection.options.regression.get() * 1000) + 200);

			pipeline.stop();
			pipeline.release();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		checkLevelActive(0);
	}

	private void checkLevelActive(int level)
	{
		for (int i = 0; i < feedbackList.size(); i++)
		{
			for (Feedback feedback : feedbackList.get(i).keySet())
			{
				if (i == level)
				{
					assertTrue(feedback.isActive());
				}
				else
				{
					assertFalse(feedback.isActive());
				}
			}
		}
	}
}
