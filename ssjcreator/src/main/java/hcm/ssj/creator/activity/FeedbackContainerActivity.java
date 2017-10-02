/*
 * FeedbackContainerActivity.java
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

package hcm.ssj.creator.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.creator.view.FeedbackLevelLayout;
import hcm.ssj.feedback.AndroidTactileFeedback;
import hcm.ssj.feedback.AuditoryFeedback;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackContainer;
import hcm.ssj.feedback.MyoTactileFeedback;
import hcm.ssj.feedback.VisualFeedback;

public class FeedbackContainerActivity extends AppCompatActivity
{
	public static FeedbackContainer feedbackContainer = null;
	private FeedbackContainer innerFeedbackContainer = null;
	private LinearLayout levelLinearLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback_container);
		mock();
		init();
		createLevels();
	}

	private void mock() {
		feedbackContainer = new FeedbackContainer();
		feedbackContainer.addFeedback(new AuditoryFeedback(), 0, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new AndroidTactileFeedback(), 0, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new MyoTactileFeedback(), 0, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new VisualFeedback(), 1, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new AuditoryFeedback(), 1, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new VisualFeedback(), 2, FeedbackContainer.Valence.UNKNOWN);
		feedbackContainer.addFeedback(new AuditoryFeedback(), 3, FeedbackContainer.Valence.UNKNOWN);
	}

	private void init() {
		innerFeedbackContainer = feedbackContainer;
		feedbackContainer = null;
		if(innerFeedbackContainer == null)
		{
			finish();
			throw new RuntimeException("no feedbackcontainer given");
		}
		levelLinearLayout = (LinearLayout) findViewById(R.id.feedbackLinearLayout);
		levelLinearLayout.setOnDragListener(new View.OnDragListener() {
			@Override
			public boolean onDrag(View v, DragEvent event)
			{
				switch (event.getAction())
				{
					case DragEvent.ACTION_DRAG_ENDED:
						break;
				}
				return true;
			}
		});
		setTitle(innerFeedbackContainer.getComponentName());
	}

	private void createLevels() {
		levelLinearLayout.removeAllViews();
		List<Map<Feedback, FeedbackContainer.Valence>> feedbackLevelList = innerFeedbackContainer.getFeedbackList();
		for(int i=0 ; i<feedbackLevelList.size(); i++)
		{
			levelLinearLayout.addView(new FeedbackLevelLayout(this, i, feedbackLevelList.get(i)));
		}
		levelLinearLayout.addView(new FeedbackLevelLayout(this, feedbackLevelList.size(), null));
	}
}
