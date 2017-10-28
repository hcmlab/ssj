/*
 * FeedbackCollectionOnDragListener.java
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

package hcm.ssj.creator.view.Feedback;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import hcm.ssj.creator.activity.FeedbackCollectionActivity;
import hcm.ssj.creator.core.PipelineBuilder;

import static android.view.DragEvent.ACTION_DRAG_ENDED;
import static android.view.DragEvent.ACTION_DRAG_STARTED;
import static android.view.DragEvent.ACTION_DROP;

/**
 * Created by Antonio Grieco on 04.10.2017.
 */

public class FeedbackCollectionOnDragListener implements View.OnDragListener
{
	private final FeedbackCollectionActivity feedbackCollectionActivity;

	public FeedbackCollectionOnDragListener(FeedbackCollectionActivity feedbackCollectionActivity)
	{
		this.feedbackCollectionActivity = feedbackCollectionActivity;
	}

	@Override
	public boolean onDrag(final View v, DragEvent event)
	{
		if (event.getLocalState() instanceof FeedbackComponentView)
		{
			final FeedbackComponentView feedbackComponentView = ((FeedbackComponentView) event.getLocalState());

			switch (event.getAction())
			{
				case ACTION_DRAG_STARTED:
					feedbackCollectionActivity.showDragIcons(feedbackComponentView.getWidth(), feedbackComponentView.getHeight());
					break;
				case ACTION_DROP:
					if (v instanceof FeedbackLevelLayout)
					{
						((FeedbackLevelLayout) v).addGridComponent(feedbackComponentView);
					}
					else if (v.equals(feedbackCollectionActivity.getRecycleBin()))
					{
						feedbackComponentView.post(new Runnable()
						{
							@Override
							public void run()
							{
								((ViewGroup) feedbackComponentView.getParent()).removeView(feedbackComponentView);
								feedbackCollectionActivity.requestReorder();
							}
						});
						PipelineBuilder.getInstance().remove(
								feedbackComponentView.getFeedbackLevelBehaviourEntry().getKey()
						);
					}
					break;
				case ACTION_DRAG_ENDED:
					// Set currently draged to false no matter where the drag ended, to force normal painting.
					feedbackComponentView.setCurrentlyDraged(false);
					feedbackCollectionActivity.hideDragIcons();
					break;
			}
			return true;
		}
		return false;
	}
}
