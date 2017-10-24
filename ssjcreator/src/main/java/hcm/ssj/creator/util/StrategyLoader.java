/*
 * StrategyLoader.java
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

package hcm.ssj.creator.util;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.event.ThresholdClassEventSender;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;

/**
 * Created by Antonio Grieco on 20.10.2017.
 */

public class StrategyLoader
{

	private File strategyFile;
	private List<StrategyFeedback> strategyFeedbackList = new ArrayList<>();

	public StrategyLoader(File strategyFile)
	{
		this.strategyFile = strategyFile;
	}

	public void load() throws IOException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(strategyFile);
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(inputStream, null);

			while (parser.next() != XmlPullParser.END_DOCUMENT)
			{
				switch (parser.getEventType())
				{
					case XmlPullParser.START_TAG:
						if (parser.getName().equalsIgnoreCase("strategy"))
						{
							loadStrategyComponents(parser);
						}
						break;
				}
			}
		}
		catch (XmlPullParserException | IOException e)
		{
			throw new RuntimeException("Could not load strategy!", e);
		}
		finally
		{
			if (inputStream != null)
			{
				inputStream.close();
			}
		}

		addComponents();
	}

	private void loadStrategyComponents(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		parser.require(XmlPullParser.START_TAG, null, "strategy");
		while (parser.next() != XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("strategy"))
		{
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("feedback"))
			{
				loadNextFeedback(parser);
			}
		}
	}

	private void loadNextFeedback(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		parser.require(XmlPullParser.START_TAG, null, "feedback");

		// LEVEL
		String level_str = parser.getAttributeValue(null, "level");
		int level = 0;
		if (level_str != null)
		{
			level = Integer.parseInt(level_str);
		}

		String valence_str = parser.getAttributeValue(null, "valence");
		FeedbackCollection.LevelBehaviour levelBehaviour = FeedbackCollection.LevelBehaviour.Neutral;
		if (valence_str != null)
		{
			if(valence_str.equalsIgnoreCase("desirable"))
				levelBehaviour = FeedbackCollection.LevelBehaviour.Regress;
			else if(valence_str.equalsIgnoreCase("undesirable"))
				levelBehaviour = FeedbackCollection.LevelBehaviour.Progress;
		}

		while (parser.next() != XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("feedback"))
		{
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("condition"))
			{
				double from = Double.parseDouble(parser.getAttributeValue(null, "from"));
				double to = Double.parseDouble(parser.getAttributeValue(null, "to"));
			}
			else if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("action"))
			{

			}
		}
	}

	private void addComponents() {
		// ThresholdClassEventSender
		List<ThresholdClassEventSender> thresholdClassEventSenderList = getThresholdClassEventSender();

		// FeedbackCollection
		FeedbackCollection feedbackCollection = new FeedbackCollection();
		PipelineBuilder.getInstance().add(feedbackCollection);
		for(ThresholdClassEventSender thresholdClassEventSender : thresholdClassEventSenderList)
		{
			PipelineBuilder.getInstance().add(thresholdClassEventSender);
			PipelineBuilder.getInstance().addEventProvider(feedbackCollection, thresholdClassEventSender);
		}

		// Feedbacks
		for(StrategyFeedback strategyFeedback : strategyFeedbackList)
		{
			PipelineBuilder.getInstance().addFeedbackToCollectionContainer(feedbackCollection,
																		   strategyFeedback.feedback,
																		   strategyFeedback.level,
																		   strategyFeedback.levelBehaviour);
		}
	}
//		List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackList = new ArrayList<>();
//
//		parser.require(XmlPullParser.START_TAG, null, "strategy");
//
//		//iterate through classes
//		while (parser.next() != XmlPullParser.END_DOCUMENT)
//		{
//			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("feedback"))
//			{
//				loadFeedback(parser, feedbackList);
//			}
//			else if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("strategy"))
//			{
//				break; //jump out once we reach end tag for classes
//			}
//		}
//		parser.require(XmlPullParser.END_TAG, null, "strategy");
//
//		addFeedbackList
//	}
//
//	private void loadFeedback(XmlPullParser parser)
//	{
//		parser.require(XmlPullParser.START_TAG, null, "feedback");
//
//		String level_str = parser.getAttributeValue(null, "level");
//		int level = 0;
//		if (level_str != null)
//		{
//			level = Integer.parseInt(level_str);
//		}
//
//		String valence_str = parser.getAttributeValue(null, "valence");
//		if (valence_str != null)
//		{
//			valence = FeedbackClass.Valence.valueOf(valence_str);
//		}
//
//		while (parser.next() != XmlPullParser.END_DOCUMENT)
//		{
//			if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("condition"))
//			{
//				condition = Condition.create(parser, context);
//			}
//			else if (xml.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("action"))
//			{
//				action = Action.create(type, parser, context);
//			}
//			else if (xml.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("feedback"))
//			{
//				break; //jump out once we reach end tag
//			}
//		}
//	}

	private class StrategyFeedback
	{
		final int level;
		final ThresholdRange thresholdRange;
		final Feedback feedback;
		final FeedbackCollection.LevelBehaviour levelBehaviour;

		private StrategyFeedback(int level,
								 ThresholdRange thresholdRange,
								 Feedback feedback,
								 FeedbackCollection.LevelBehaviour levelBehaviour) {
			this.level = level;
			this.thresholdRange = thresholdRange;
			this.feedback = feedback;
			this.levelBehaviour = levelBehaviour;
		}
	}

	private class ThresholdRange
	{
		final double lowerBound;
		final double upperBound;

		public ThresholdRange(double lowerBound, double upperBound)
		{
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		public ThresholdRangeRelation compare(ThresholdRange other)
		{
			// CONGRUENT
			if(other.lowerBound == lowerBound && other.upperBound == upperBound)
				return ThresholdRangeRelation.CONGRUENT;
			// COMPLETE_LOWER
			else if(other.lowerBound<= lowerBound && other.upperBound <= lowerBound)
				return ThresholdRangeRelation.COMPLETE_LOWER;
			// COMPLETE_UPPER
			else if(other.lowerBound >= upperBound && other.upperBound >= upperBound)
				return ThresholdRangeRelation.COMPLETE_UPPER;
			// INTERSECTING
			else
				return ThresholdRangeRelation.INTERSECTING;
		}
	}

	private enum ThresholdRangeRelation
	{
		COMPLETE_LOWER, COMPLETE_UPPER, INTERSECTING, CONGRUENT;
	}
}
