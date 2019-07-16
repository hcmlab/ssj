/*
 * StrategyLoaderTest.java
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

package hcm.ssj.creator;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Component;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.StrategyLoader;
import hcm.ssj.event.ThresholdClassEventSender;
import hcm.ssj.feedback.AndroidTactileFeedback;
import hcm.ssj.feedback.AuditoryFeedback;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.feedback.VisualFeedback;

import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class StrategyLoaderTest
{
	File strategyFile;

	@Before
	public void makeStrategyFile() throws IOException, XmlPullParserException
	{
		String strategyContent = "" +
				"<ssj xmlns=\"hcm.ssj\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
				"     xsi:schemaLocation=\"hcm.ssj http://hcmlab.github.io/ssj/res/feedback.xsd\">" +
				"    <strategy>" +
				"        <feedback type=\"audio\" level=\"0\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"0.0\" to=\"4.0\"/>" +
				"            <action res=\"Creator/res/blop.mp3\" intensity=\"0.0\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"audio\" level=\"1\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"4.0\" to=\"13\"/>" +
				"            <action res=\"Creator/res/blop.mp3\" intensity=\"0.5\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"audio\" level=\"2\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"13\" to=\"999\"/>" +
				"            <action res=\"Creator/res/blop.mp3\" intensity=\"1.0\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"tactile\" device=\"Android\" level=\"0\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"0.0\" to=\"3\"/>" +
				"            <action duration=\"0,100,100,100\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"tactile\" device=\"Android\" level=\"1\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"3\" to=\"12\"/>" +
				"            <action duration=\"0,100,100,100\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"tactile\" device=\"Android\" level=\"2\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"12\" to=\"999\"/>" +
				"            <action duration=\"0,200,100,200\" lockSelf=\"1000\"/>" +
				"        </feedback>" +
				"        <feedback type=\"visual\" position=\"0\" level=\"0\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"0.0\" to=\"3.0\"/>" +
				"            <action res=\"Creator/res/orientation_low.png, Creator/res/thumb_negative.png\"/>" +
				"        </feedback>" +
				"        <feedback type=\"visual\" position=\"0\" level=\"1\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"3.0\" to=\"12.0\"/>" +
				"            <action res=\"Creator/res/orientation_med.png, Creator/res/thumb_positive.png\"/>" +
				"        </feedback>" +
				"        <feedback type=\"visual\" position=\"0\" level=\"2\">" +
				"            <condition type=\"BodyEnergy\" event=\"OverallActivation\" sender=\"SSJ\" from=\"12.0\" to=\"999.0\"/>" +
				"            <action res=\"Creator/res/orientation_high.png, Creator/res/thumb_negative.png\"/>" +
				"        </feedback>" +
				"    </strategy>" +
				"</ssj>";

		strategyFile = File.createTempFile("testStrategy", ".xml");
		PrintWriter printWriter = new PrintWriter(strategyFile);
		printWriter.append(strategyContent);
		printWriter.close();

		StrategyLoader strategyLoader = new StrategyLoader(strategyFile);
		strategyLoader.load();
	}

	@Test
	public void testComponentsPresent()
	{
		PipelineBuilder pipelineBuilder = PipelineBuilder.getInstance();

		/* ------ ThresholdClassEventSender's ------ */
		List<Component> thresholdClassEventSenders =
				pipelineBuilder.getComponentsOfClass(PipelineBuilder.Type.Consumer, ThresholdClassEventSender.class);
		assertTrue(thresholdClassEventSenders.size() == 1);
		ThresholdClassEventSender thresholdClassEventSender = (ThresholdClassEventSender)thresholdClassEventSenders.get(0);
		assertTrue(Arrays.equals(thresholdClassEventSender.options.thresholds.get(), new float[]{0,3,4,12,13,999}));

		/* ------ FeedbackCollection ------ */
		List<Component> feedbackCollections =
				pipelineBuilder.getComponentsOfClass(PipelineBuilder.Type.EventHandler, FeedbackCollection.class);
		assertTrue(feedbackCollections.size() == 1);

		/* ------ Feedbacks ------ */
		List<Component> visualFeedbacks =
				pipelineBuilder.getComponentsOfClass(PipelineBuilder.Type.EventHandler, VisualFeedback.class);
		assertTrue(visualFeedbacks.size() == 3);
		for (Component visualFeedback : visualFeedbacks)
		{
			assertTrue(pipelineBuilder.isManagedFeedback(visualFeedback));
		}

		List<Component> auditoryFeedbacks =
				pipelineBuilder.getComponentsOfClass(PipelineBuilder.Type.EventHandler, AuditoryFeedback.class);
		assertTrue(auditoryFeedbacks.size() == 3);
		for (Component auditoryFeedback : auditoryFeedbacks)
		{
			assertTrue(pipelineBuilder.isManagedFeedback(auditoryFeedback));
		}

		List<Component> androidTactileFeedbacks =
				pipelineBuilder.getComponentsOfClass(PipelineBuilder.Type.EventHandler, AndroidTactileFeedback.class);
		assertTrue(androidTactileFeedbacks.size() == 3);
		for (Component androidTactileFeedback : androidTactileFeedbacks)
		{
			assertTrue(pipelineBuilder.isManagedFeedback(androidTactileFeedback));
		}
	}

	@Test
	public void testComponentsOnRightLevel()
	{
		List<Component> feedbackCollections =
				PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.EventHandler, FeedbackCollection.class);
		assertTrue(feedbackCollections.size() == 1);
		FeedbackCollection feedbackCollection = (FeedbackCollection) feedbackCollections.get(0);
		List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackList = feedbackCollection.getFeedbackList();

		assertTrue(feedbackList.size() == 3);
		for (Map feedbackMap : feedbackList)
		{
			assertTrue(feedbackMap.size() == 3);
		}
	}

	@After
	public void deleteStrategyFile()
	{
		if (strategyFile != null)
		{
			strategyFile.delete();
		}
		PipelineBuilder.getInstance().clear();
	}
}