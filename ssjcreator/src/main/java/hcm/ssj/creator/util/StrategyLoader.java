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

import android.support.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Component;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.event.ThresholdClassEventSender;
import hcm.ssj.feedback.AndroidTactileFeedback;
import hcm.ssj.feedback.AuditoryFeedback;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.feedback.MSBandTactileFeedback;
import hcm.ssj.feedback.MyoTactileFeedback;
import hcm.ssj.feedback.VisualFeedback;

/**
 * Created by Antonio Grieco on 20.10.2017.
 */

public class StrategyLoader
{
	private File strategyFile;
	private List<ParsedStrategyFeedback> parsedStrategyFeedbackList = new ArrayList<>();
	private List<Component> addedComponents = new ArrayList<>();

	public StrategyLoader(File strategyFile)
	{
		this.strategyFile = strategyFile;
	}

	public boolean load()
	{
		InputStream inputStream = null;

		try
		{
			inputStream = new FileInputStream(strategyFile);
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(inputStream, null);

			parser.nextTag();
			parser.require(XmlPullParser.START_TAG, null, "ssj");
			parser.nextTag();
			parser.require(XmlPullParser.START_TAG, null, "strategy");
			loadStrategyComponents(parser);
			inputStream.close();
			addComponents();
			return true;
		}
		catch (IOException | XmlPullParserException | SSJFatalException e)
		{
			removeAddedComponents();
			return false;
		}
	}

	private void removeAddedComponents()
	{
		for (Component addedComponent : addedComponents)
		{
			PipelineBuilder.getInstance().remove(addedComponent);
		}
	}

	private void loadStrategyComponents(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		parser.require(XmlPullParser.START_TAG, null, "strategy");

		while (parser.next() != XmlPullParser.END_DOCUMENT)
		{
			switch (parser.getEventType())
			{
				case XmlPullParser.START_TAG:
					if (parser.getName().equalsIgnoreCase("feedback"))
					{
						parsedStrategyFeedbackList.add(getNextParsedStrategyFeedback(parser));
					}
					break;
			}
		}
	}

	private ParsedStrategyFeedback getNextParsedStrategyFeedback(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		ParsedStrategyFeedback parsedStrategyFeedback = new ParsedStrategyFeedback();

		goToNextTagWithName(parser, "feedback");
		parsedStrategyFeedback.feedbackAttributes = new FeedbackAttributes(parser);

		goToNextTagWithName(parser, "condition");
		parsedStrategyFeedback.conditionAttributes = new ConditionAttributes(parser);

		goToNextTagWithName(parser, "action");
		parsedStrategyFeedback.actionAttributes = new ActionAttributes(parser);

		return parsedStrategyFeedback;
	}

	private void goToNextTagWithName(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException
	{
		while (parser.getName() == null || !parser.getName().equalsIgnoreCase(tagName))
		{
			parser.next();
		}
	}

	private void addComponents() throws SSJFatalException
	{
		PipelineBuilder pipelineBuilder = PipelineBuilder.getInstance();

		FeedbackCollection feedbackCollection = new FeedbackCollection();
		pipelineBuilder.add(feedbackCollection);
		addedComponents.add(feedbackCollection);

		Map<String, List<Float>> thresholdEventMap = getThresholdEventMap(parsedStrategyFeedbackList);
		List<ThresholdClassEventSender> thresholdClassEventSenders = getThresholdClassEventSenders(thresholdEventMap);
		for (ThresholdClassEventSender thresholdClassEventSender : thresholdClassEventSenders)
		{
			pipelineBuilder.add(thresholdClassEventSender);
			pipelineBuilder.addEventInput(feedbackCollection, thresholdClassEventSender);
			addedComponents.add(thresholdClassEventSender);
		}

		addFeedbacks(feedbackCollection, thresholdEventMap);
	}

	private void addFeedbacks(FeedbackCollection feedbackCollection,
							  Map<String, List<Float>> thresholdEventMap) throws SSJFatalException
	{
		for (ParsedStrategyFeedback parsedStrategyFeedback : parsedStrategyFeedbackList)
		{
			Feedback feedback = getFeedbackForParsedFeedback(parsedStrategyFeedback);

			if (feedback == null)
			{
				throw new SSJFatalException();
			}

			PipelineBuilder.getInstance().add(feedback);
			PipelineBuilder.getInstance().addFeedbackToCollectionContainer(
					feedbackCollection,
					feedback,
					getLevel(parsedStrategyFeedback),
					getLevelBehaviour(parsedStrategyFeedback)

			);
			addedComponents.add(feedback);

			// Set lock
			if (parsedStrategyFeedback.actionAttributes.lockSelf != null)
			{
				feedback.getOptions().lock.set(Integer.parseInt(parsedStrategyFeedback.actionAttributes.lockSelf));
			}

			String strategyEventName = parsedStrategyFeedback.conditionAttributes.event;
			String[] eventNames = getEventNames(strategyEventName,
												thresholdEventMap.get(strategyEventName),
												parsedStrategyFeedback.conditionAttributes.from,
												parsedStrategyFeedback.conditionAttributes.to);
			feedback.getOptions().eventNames.set(eventNames);
		}
	}

	private List<ThresholdClassEventSender> getThresholdClassEventSenders(Map<String, List<Float>> thresholdEventMap)
	{
		List<ThresholdClassEventSender> thresholdClassEventSenders = new ArrayList<>();
		for (Map.Entry<String, List<Float>> thresholdEntry : thresholdEventMap.entrySet())
		{
			ThresholdClassEventSender thresholdClassEventSender = new ThresholdClassEventSender();
			float[] thresholdArray = new float[thresholdEntry.getValue().size()];
			int thresholdArrayCounter = 0;
			for (Float threshold : thresholdEntry.getValue())
			{
				thresholdArray[thresholdArrayCounter++] = (threshold != null ? threshold : Float.NaN);
			}
			thresholdClassEventSender.options.thresholds.set(thresholdArray);

			List<String> thresholdNames = new ArrayList<>();
			for (int thresholdNameCounter = 0; thresholdNameCounter < thresholdArray.length; thresholdNameCounter++)
			{
				thresholdNames.add(thresholdEntry.getKey() + thresholdNameCounter);
			}
			thresholdClassEventSender.options.classes.set(thresholdNames.toArray(new String[thresholdNames.size()]));
			thresholdClassEventSenders.add(thresholdClassEventSender);
		}
		return thresholdClassEventSenders;
	}

	private Map<String, List<Float>> getThresholdEventMap(List<ParsedStrategyFeedback> parsedStrategyFeedbackList)
	{
		Map<String, List<Float>> thresholdEventMap = new HashMap<>();

		// Iterate first time to build up Thresholds
		for (ParsedStrategyFeedback parsedStrategyFeedback : parsedStrategyFeedbackList)
		{
			String event = parsedStrategyFeedback.conditionAttributes.event;
			if (!thresholdEventMap.containsKey(event))
			{
				thresholdEventMap.put(event, new ArrayList<Float>());
			}

			if (parsedStrategyFeedback.conditionAttributes.from != null)
			{
				float from = Float.parseFloat(parsedStrategyFeedback.conditionAttributes.from);
				if (!thresholdEventMap.get(event).contains(from))
				{
					thresholdEventMap.get(event).add(from);
				}
			}
			if (parsedStrategyFeedback.conditionAttributes.to != null)
			{
				float to = Float.parseFloat(parsedStrategyFeedback.conditionAttributes.to);
				if (!thresholdEventMap.get(event).contains(to))
				{
					thresholdEventMap.get(event).add(to);
				}
			}
		}

		for (List<Float> thresholdList : thresholdEventMap.values())
		{
			Collections.sort(thresholdList);
		}

		return thresholdEventMap;
	}

	private String[] getEventNames(String strategyEventName, List<Float> thresholds, String from, String to)
	{
		if (from == null || to == null)
		{
			return null;
		}

		Float fromFloat = Float.parseFloat(from);
		Float toFloat = Float.parseFloat(to);

		List<String> thresholdNames = new ArrayList<>();
		for (int i = thresholds.indexOf(fromFloat);
			 i < thresholds.indexOf(toFloat);
			 i++)
		{
			thresholdNames.add(strategyEventName + i);
		}
		return thresholdNames.toArray(new String[thresholdNames.size()]);
	}

	private FeedbackCollection.LevelBehaviour getLevelBehaviour(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		FeedbackCollection.LevelBehaviour levelBehaviour = FeedbackCollection.LevelBehaviour.Neutral;
		if (parsedStrategyFeedback.feedbackAttributes.valence != null)
		{
			if (parsedStrategyFeedback.feedbackAttributes.valence.equalsIgnoreCase("desirable"))
			{
				levelBehaviour = FeedbackCollection.LevelBehaviour.Regress;
			}
			else if (parsedStrategyFeedback.feedbackAttributes.valence.equalsIgnoreCase("undesirable"))
			{
				levelBehaviour = FeedbackCollection.LevelBehaviour.Progress;
			}
		}
		return levelBehaviour;
	}

	private int getLevel(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		if (parsedStrategyFeedback.feedbackAttributes.level != null)
		{
			return Integer.parseInt(parsedStrategyFeedback.feedbackAttributes.level);
		}
		else
		{
			return 0;
		}
	}

	@Nullable
	private Feedback getFeedbackForParsedFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		if (parsedStrategyFeedback.feedbackAttributes.type.equalsIgnoreCase("audio"))
		{
			return getAuditoryFeedback(parsedStrategyFeedback);
		}
		else if (parsedStrategyFeedback.feedbackAttributes.type.equalsIgnoreCase("visual"))
		{
			return getVisualFeedback(parsedStrategyFeedback);
		}
		else if (parsedStrategyFeedback.feedbackAttributes.type.equalsIgnoreCase("tactile"))
		{
			return getTactileFeedback(parsedStrategyFeedback);
		}

		return null;
	}

	@Nullable
	private Feedback getTactileFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		if (parsedStrategyFeedback.feedbackAttributes.device == null ||
				parsedStrategyFeedback.feedbackAttributes.device.equalsIgnoreCase("Android"))
		{
			return getAndroidTactileFeedback(parsedStrategyFeedback);
		}
		else if (parsedStrategyFeedback.feedbackAttributes.device.equalsIgnoreCase("Myo"))
		{
			return getMyoTactileFeedback(parsedStrategyFeedback);
		}
		else if (parsedStrategyFeedback.feedbackAttributes.device.equalsIgnoreCase("MsBand"))
		{
			return getMsBandTactileFeedback(parsedStrategyFeedback);
		}
		return null;
	}

	private Feedback getMsBandTactileFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		MSBandTactileFeedback msBandTactileFeedback = new MSBandTactileFeedback();

		// Duration
		msBandTactileFeedback.options.duration.setValue(parsedStrategyFeedback.actionAttributes.duration);

		// VibrationType
		msBandTactileFeedback.options.vibrationType.setValue(parsedStrategyFeedback.actionAttributes.type);

		// DeviceId
		msBandTactileFeedback.options.deviceId.setValue(parsedStrategyFeedback.feedbackAttributes.deviceId);

		return msBandTactileFeedback;
	}

	private Feedback getAndroidTactileFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		AndroidTactileFeedback androidTactileFeedback = new AndroidTactileFeedback();
		//Vibration Pattern
		androidTactileFeedback.options.vibrationPattern.setValue(parsedStrategyFeedback.actionAttributes.duration);

		return androidTactileFeedback;
	}

	private Feedback getMyoTactileFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		MyoTactileFeedback myoTactileFeedback = new MyoTactileFeedback();
		//DeviceId
		myoTactileFeedback.options.deviceId.set(parsedStrategyFeedback.feedbackAttributes.deviceId);
		//Intensity
		myoTactileFeedback.options.intensity.setValue(parsedStrategyFeedback.actionAttributes.intensity);
		//Duration
		myoTactileFeedback.options.duration.setValue(parsedStrategyFeedback.actionAttributes.duration);

		return myoTactileFeedback;
	}

	private Feedback getVisualFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		VisualFeedback visualFeedback = new VisualFeedback();

		//Options
		if (parsedStrategyFeedback.actionAttributes.brightness != null)
		{
			visualFeedback.options.brightness.set(Float.parseFloat(parsedStrategyFeedback.actionAttributes.brightness));
		}

		if (parsedStrategyFeedback.actionAttributes.duration != null)
		{
			visualFeedback.options.duration.set(Integer.parseInt(parsedStrategyFeedback.actionAttributes.duration));
		}

		if (parsedStrategyFeedback.feedbackAttributes.fade != null)
		{
			visualFeedback.options.fade.set(Integer.parseInt(parsedStrategyFeedback.feedbackAttributes.fade));
		}

		if (parsedStrategyFeedback.feedbackAttributes.position != null)
		{
			visualFeedback.options.position.set(Integer.parseInt(parsedStrategyFeedback.feedbackAttributes.position));
		}

		// Icons
		if (parsedStrategyFeedback.actionAttributes.res != null)
		{
			String[] iconString = parsedStrategyFeedback.actionAttributes.res.split(",");
			if (iconString.length > 0 && !iconString[0].isEmpty())
			{
				visualFeedback.options.feedbackIcon.setValue(iconString[0].trim());
			}
			if (iconString.length == 2 && !iconString[1].isEmpty())
			{
				visualFeedback.options.qualityIcon.setValue(iconString[0].trim());
			}
		}

		return visualFeedback;
	}

	private Feedback getAuditoryFeedback(ParsedStrategyFeedback parsedStrategyFeedback)
	{
		AuditoryFeedback auditoryFeedback = new AuditoryFeedback();

		//Options
		auditoryFeedback.options.audioFile.setValue(parsedStrategyFeedback.actionAttributes.res.trim());

		if (parsedStrategyFeedback.actionAttributes.intensity != null)
		{
			auditoryFeedback.options.intensity.set(Float.parseFloat(parsedStrategyFeedback.actionAttributes.intensity));
		}

		if (parsedStrategyFeedback.actionAttributes.lockSelf != null)
		{
			auditoryFeedback.options.lock.set(Integer.parseInt(parsedStrategyFeedback.actionAttributes.lockSelf));
		}

		return auditoryFeedback;
	}

	private class ParsedStrategyFeedback
	{
		FeedbackAttributes feedbackAttributes;
		ActionAttributes actionAttributes;
		ConditionAttributes conditionAttributes;
	}

	private class ActionAttributes
	{
		final String res;
		final String lock;
		final String lockSelf;
		final String intensity;
		final String duration;
		final String brightness;
		final String type;

		public ActionAttributes(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
		{
			xmlPullParser.require(XmlPullParser.START_TAG, null, "action");

			res = xmlPullParser.getAttributeValue(null, "res");
			lock = xmlPullParser.getAttributeValue(null, "lock");
			lockSelf = xmlPullParser.getAttributeValue(null, "lockSelf");
			intensity = xmlPullParser.getAttributeValue(null, "intensity");
			duration = xmlPullParser.getAttributeValue(null, "duration");
			brightness = xmlPullParser.getAttributeValue(null, "brightness");
			type = xmlPullParser.getAttributeValue(null, "type");
		}
	}

	private class ConditionAttributes
	{
		String event;
		String sender;
		String type;
		String history;
		String sum;
		String from;
		String to;

		public ConditionAttributes(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
		{
			xmlPullParser.require(XmlPullParser.START_TAG, null, "condition");

			event = xmlPullParser.getAttributeValue(null, "event");
			sender = xmlPullParser.getAttributeValue(null, "sender");
			type = xmlPullParser.getAttributeValue(null, "type");
			history = xmlPullParser.getAttributeValue(null, "history");
			sum = xmlPullParser.getAttributeValue(null, "sum");
			from = xmlPullParser.getAttributeValue(null, "from");
			to = xmlPullParser.getAttributeValue(null, "to");
		}
	}

	private class FeedbackAttributes
	{
		String type;
		String valence;
		String level;
		String layout;
		String position;
		String fade;
		String def_brightness;
		String device;
		String deviceId;

		public FeedbackAttributes(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException
		{
			xmlPullParser.require(XmlPullParser.START_TAG, null, "feedback");

			type = xmlPullParser.getAttributeValue(null, "type");
			valence = xmlPullParser.getAttributeValue(null, "valence");
			level = xmlPullParser.getAttributeValue(null, "level");
			layout = xmlPullParser.getAttributeValue(null, "layout");
			position = xmlPullParser.getAttributeValue(null, "position");
			fade = xmlPullParser.getAttributeValue(null, "fade");
			def_brightness = xmlPullParser.getAttributeValue(null, "def_brightness");
			device = xmlPullParser.getAttributeValue(null, "device");
			deviceId = xmlPullParser.getAttributeValue(null, "deviceId");
		}
	}
}
