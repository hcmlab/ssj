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

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;

import static android.R.attr.type;

/**
 * Created by Antonio Grieco on 20.10.2017.
 */

public class StrategyLoader
{

	private File strategyFile;

	public StrategyLoader(File strategyFile)
	{
		this.strategyFile = strategyFile;
	}

	public void load()
	{
//		InputStream inputStream = new FileInputStream(strategyFile);
//		XmlPullParser parser = Xml.newPullParser();
//		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//
//		parser.setInput(inputStream, null);
//
//		while (parser.next() != XmlPullParser.END_DOCUMENT)
//		{
//			switch (parser.getEventType())
//			{
//				case XmlPullParser.START_TAG:
//					if (parser.getName().equalsIgnoreCase("strategy"))
//					{
//						loadStrategy(parser);
//					}
//					break;
//			}
//		}
//		inputStream.close();
	}
//
//	private void loadStrategy(XmlPullParser parser) throws IOException, XmlPullParserException
//	{
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
//
//	private class ThresholdRange
//	{
//		private final int lowerBound;
//		private final int upperBound;
//
//		public ThresholdRange(int lowerBound, int upperBound)
//		{
//			this.lowerBound = lowerBound;
//			this.upperBound = upperBound;
//		}
//
//		@Override
//		public boolean equals(Object object)
//		{
//			if (!(object instanceof ThresholdRange))
//			{
//				return false;
//			}
//
//			return ((ThresholdRange) object).getLowerBound() == lowerBound &&
//					((ThresholdRange) object).getUpperBound() == upperBound;
//		}
//
//		protected boolean intersects(ThresholdRange thresholdRange)
//		{
//			boolean isLower = thresholdRange.getLowerBound() < lowerBound && thresholdRange.getLowerBound() < upperBound;
//			boolean isUpper = thresholdRange.getUpperBound() > lowerBound && thresholdRange.getUpperBound() > upperBound;
//			return !(isLower || isUpper);
//		}
//
//		public int getLowerBound()
//		{
//			return lowerBound;
//		}
//
//		public int getUpperBound()
//		{
//			return upperBound;
//		}
//	}

}
