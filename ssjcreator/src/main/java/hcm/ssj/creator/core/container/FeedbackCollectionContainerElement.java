/*
 * FeedbackCollectionContainerElement.java
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

package hcm.ssj.creator.core.container;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;

/**
 * Created by hiwi on 18.10.2017.
 */

public class FeedbackCollectionContainerElement extends ContainerElement
{

	private List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackList;

	public FeedbackCollectionContainerElement(FeedbackCollection element)
	{
		super(element);
		feedbackList = element.getFeedbackList();
	}

	public List<Map<Feedback, FeedbackCollection.LevelBehaviour>> getFeedbackList()
	{
		return feedbackList;
	}

	public void addFeedback(Feedback feedback, int level, FeedbackCollection.LevelBehaviour levelBehaviour)
	{
		while (feedbackList.size() <= level)
		{
			feedbackList.add(new LinkedHashMap<Feedback, FeedbackCollection.LevelBehaviour>());
		}
		feedbackList.get(level).put(feedback, levelBehaviour);
	}

	public void removeFeedback(Feedback feedback)
	{
		for (Map<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourMap : feedbackList)
		{
			feedbackLevelBehaviourMap.remove(feedback);
		}
	}

	public void removeAllFeedbacks() {
		feedbackList = new ArrayList<>();
	}
}
