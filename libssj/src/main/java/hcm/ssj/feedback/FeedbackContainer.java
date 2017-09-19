/*
 * FeedbackContainer.java
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

package hcm.ssj.feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.EventChannel;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Antonio Grieco on 18.09.2017.
 */

public class FeedbackContainer extends EventHandler
{

	public enum Valence
	{
		UNKNOWN,
		DESIRABLE,
		UNDESIRABLE;
	}

	public class Options extends OptionList
	{
		public final Option<Float> progression = new Option<>("progression", 12f, Float.class, "timeout for progressing to the next feedback level");
		public final Option<Float> regression = new Option<>("regression", 60f, Float.class, "timeout for going back to the previous feedback level");

		private Options()
		{
			addOptions();
		}
	}

	public FeedbackContainer.Options options = new FeedbackContainer.Options();
	private Pipeline pipeline;
	private int currentLevel;
	private List<Map<Feedback, Valence>> feedbackList;
	private long lastDesireableState;
	private long lastUndesireableState;

	public FeedbackContainer()
	{
		_name = "FeedbackContainer";
		feedbackList = new ArrayList<>();
	}

	@Override
	public void enter()
	{
		currentLevel = 0;
		lastDesireableState = 0;
		lastUndesireableState = 0;
		pipeline = Pipeline.getInstance();
		addEventChannels();
	}

	@Override
	public void notify(Event event)
	{
		if (feedbackList.isEmpty() || feedbackList.get(currentLevel).isEmpty())
		{
			return;
		}

		for (Map.Entry<Feedback, Valence> feedbackEntry : feedbackList.get(currentLevel).entrySet())
		{
			long feedbackEntryLastExecutionTime = feedbackEntry.getKey().getLastExecutionTime();
			switch (feedbackEntry.getValue())
			{
				case DESIRABLE:
					if (feedbackEntryLastExecutionTime > lastDesireableState)
					{
						lastDesireableState = feedbackEntryLastExecutionTime;
					}
					break;
				case UNDESIRABLE:
					if (feedbackEntryLastExecutionTime > lastUndesireableState)
					{
						lastUndesireableState = feedbackEntryLastExecutionTime;
					}
					break;
				case UNKNOWN:
					break;
				default:
					throw new RuntimeException("Valence value invalid!");
			}
		}

		//if all current feedback classes are in a non desirable state, check if we should progress to next level
		if (System.currentTimeMillis() - (int) (options.progression.get() * 1000) > lastDesireableState && (currentLevel + 1) < feedbackList.size())
		{
			setLevelActive(currentLevel + 1);
			lastDesireableState = System.currentTimeMillis();
			Log.d("activating level " + currentLevel);
		}
		//if all current feedback classes are in a desirable state, check if we can go back to the previous level
		else if (System.currentTimeMillis() - (int) (options.regression.get() * 1000) > lastUndesireableState && currentLevel > 0)
		{
			setLevelActive(currentLevel - 1);
			lastUndesireableState = System.currentTimeMillis();
		}
	}

	private void setLevelActive(int level)
	{
		if (level >= feedbackList.size())
		{
			throw new RuntimeException("Setting level " + level + " active exceeds available levels.");
		}

		Log.d("activating level " + level);

		currentLevel = level;
		for (int i = 0; i < feedbackList.size(); i++)
		{
			for (Feedback feedback : feedbackList.get(i).keySet())
			{
				feedback.setActive(currentLevel == i);
			}
		}
	}

	private void addEventChannels()
	{
		for (Map<Feedback, Valence> innerList : feedbackList)
		{
			for (Feedback feedback : innerList.keySet())
			{
				feedback.removeEventChannels();
				for (EventChannel eventChannel : _evchannel_in)
				{
					pipeline.registerEventListener(feedback, eventChannel);
				}
			}
		}
	}

	public List<Map<Feedback, Valence>> getFeedbackList()
	{
		return feedbackList;
	}

	public void addFeedback(Feedback feedback, int level, Valence valence)
	{
		while (feedbackList.size() <= level)
		{
			feedbackList.add(new HashMap<Feedback, Valence>());
		}
		feedbackList.get(level).put(feedback, valence);
	}

	public void removeFeedback(Feedback feedback)
	{
		for (Map feedbackMap : feedbackList)
		{
			if (feedbackMap.containsKey(feedback))
			{
				feedbackMap.remove(feedback);
			}
		}
	}
}
