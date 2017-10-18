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

import android.widget.TableLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

	public FeedbackContainer.Options options = new FeedbackContainer.Options();
	private int currentLevel;
	private List<Map<Feedback, LevelBehaviour>> feedbackList;

	public FeedbackContainer()
	{
		_name = "FeedbackContainer";
		feedbackList = new ArrayList<>();
	}

	@Override
	public void enter()
	{
		if (options.layout.get() == null)
		{
			throw new RuntimeException("layout not set, cannot render visual feedback");
		}

		currentLevel = 0;
		changeLayouts();
		setLevelActive(currentLevel);
	}

	private void changeLayouts()
	{
		for (Map<Feedback, LevelBehaviour> levelMap : feedbackList)
		{
			for (Feedback feedback : levelMap.keySet())
			{
				if (feedback instanceof VisualFeedback)
				{
					((VisualFeedback) feedback).options.layout.set(this.options.layout.get());
				}
			}
		}
	}

	@Override
	public void notify(Event event)
	{
		if (feedbackList.isEmpty() || feedbackList.get(currentLevel).isEmpty())
		{
			return;
		}

		List<Long> lastProgressExecutionTimes = new ArrayList<>();
		List<Long> lastRegressExecutionTimes = new ArrayList<>();

		for (Map.Entry<Feedback, LevelBehaviour> feedbackEntry : feedbackList.get(currentLevel).entrySet())
		{
			long feedbackEntryLastExecutionTime = feedbackEntry.getKey().getLastExecutionTime();
			switch (feedbackEntry.getValue())
			{
				case Regress:
					lastRegressExecutionTimes.add(feedbackEntryLastExecutionTime);
					break;
				case Neutral:
					break;
				case Progress:
					lastProgressExecutionTimes.add(feedbackEntryLastExecutionTime);
					break;
				default:
					throw new RuntimeException("LevelBehaviour value invalid!");
			}
		}

		//TODO: NOT WORKING PROPPERLY!

		//if all progress feedback classes are active and no regress class is active, check if we should progress to next level
		if ((currentLevel + 1) < feedbackList.size() &&
				allTimeStampsInIntervalFromNow(lastProgressExecutionTimes, (long) (options.progression.get() * 1000)) &&
				noTimeStampInIntervalFromNow(lastRegressExecutionTimes, (long) (options.progression.get() * 1000)))
		{
			Log.d("progressing");
			setLevelActive(currentLevel + 1);
		}
		//if all regress feedback classes are active and no progress class is active, check if we can go back to the previous level
		else if (currentLevel > 0 &&
				allTimeStampsInIntervalFromNow(lastRegressExecutionTimes, (long) (options.regression.get() * 1000)) &&
				noTimeStampInIntervalFromNow(lastProgressExecutionTimes, (long) (options.regression.get() * 1000)))
		{
			Log.d("regressing");
			setLevelActive(currentLevel - 1);
		}
	}

	private boolean allTimeStampsInIntervalFromNow(List<Long> timeStamps, long interval)
	{
		if (timeStamps.isEmpty())
		{
			return false;
		}

		long currentTime = System.currentTimeMillis();
		for (Long timeStamp : timeStamps)
		{
			if (currentTime - interval < timeStamp)
			{
				return false;
			}
		}
		return true;
	}

	private boolean noTimeStampInIntervalFromNow(List<Long> timeStamps, long interval)
	{
		if (timeStamps.isEmpty())
		{
			return true;
		}

		long currentTime = System.currentTimeMillis();
		for (Long timeStamp : timeStamps)
		{
			if (currentTime - interval > timeStamp)
			{
				return false;
			}
		}
		return true;
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

	public List<Map<Feedback, LevelBehaviour>> getFeedbackList()
	{
		return feedbackList;
	}

	public void addFeedback(Feedback feedback, int level, LevelBehaviour levelBehaviour)
	{
		removeFeedback(feedback);
		while (feedbackList.size() <= level)
		{
			feedbackList.add(new LinkedHashMap<Feedback, LevelBehaviour>());
		}
		feedbackList.get(level).put(feedback, levelBehaviour);
	}

	public void removeFeedback(Feedback feedback)
	{
		for (Map<Feedback, LevelBehaviour> feedbackLevelBehaviourMap : feedbackList)
		{
			feedbackLevelBehaviourMap.remove(feedback);
		}
	}

	public void removeAllFeedbacks()
	{
		feedbackList.clear();
	}

	public enum LevelBehaviour
	{
		Regress,
		Neutral,
		Progress;
	}

	public class Options extends OptionList
	{
		public final Option<Float> progression = new Option<>("progression", 12f, Float.class, "timeout for progressing to the next feedback level");
		public final Option<Float> regression = new Option<>("regression", 60f, Float.class, "timeout for going back to the previous feedback level");
		public final Option<TableLayout> layout = new Option<>("layout", null, TableLayout.class, "TableLayout in which to render every visual feedback");

		private Options()
		{
			addOptions();
		}
	}
}
