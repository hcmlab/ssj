/*
 * LevelLayout.java
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

package hcm.ssj.creator.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackContainer;

/**
 * Created by Antonio Grieco on 18.09.2017.
 */

public class FeedbackLevelLayout extends LinearLayout
{

	private final int level;
	private final Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap;
	private android.widget.GridLayout feedbackComponentGrid;
	private TextView levelTextView;

	public FeedbackLevelLayout(Context context, int level, final Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap)
	{
		super(context);

		this.level = level;
		this.feedbackValenceMap = feedbackValenceMap;

		LinearLayout.inflate(context, R.layout.single_level_layout, this);
		levelTextView = (TextView) this.findViewById(R.id.levelText);
		levelTextView.setText(String.valueOf(level));
		feedbackComponentGrid = (android.widget.GridLayout) this.findViewById(R.id.feedbackComponentGrid);
		buildComponentGrid(context);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);
	}

	protected void buildComponentGrid(final Context context)
	{
		if (feedbackValenceMap == null || feedbackValenceMap.isEmpty())
		{
			return;
		}
		feedbackComponentGrid.post(new Runnable()
		{
			@Override
			public void run()
			{
				feedbackComponentGrid.removeAllViews();
				int height = (int) (levelTextView.getMeasuredHeight()* 0.75);
				LinearLayout.LayoutParams componentLayoutParams = new LinearLayout.LayoutParams(height,height);
				componentLayoutParams.gravity = Gravity.CENTER_VERTICAL;
				int margin = height/10;
				componentLayoutParams.setMargins(margin,margin,margin,margin);
				for (Map.Entry<Feedback, FeedbackContainer.Valence> entry : feedbackValenceMap.entrySet())
				{
					FeedbackComponentView feedbackComponentView = new FeedbackComponentView(context, entry);
					feedbackComponentView.setLayoutParams(componentLayoutParams);
					feedbackComponentGrid.addView(feedbackComponentView);
				}
				final View v = feedbackComponentGrid.getChildAt(0);
				v.post(new Runnable()
				{
					@Override
					public void run()
					{
						android.widget.GridLayout.LayoutParams lp1 = (android.widget.GridLayout.LayoutParams) v.getLayoutParams();
						int columns = (feedbackComponentGrid.getMeasuredWidth() /
								(v.getMeasuredWidth() + lp1.leftMargin + lp1.rightMargin));
						reorder(columns);
						feedbackComponentGrid.setColumnCount(columns);
					}
				});
			}
		});
	}

	private void reorder(int columns) {
		for (int i = 0; i < feedbackComponentGrid.getChildCount(); i++)
		{
			android.widget.GridLayout.LayoutParams lp = (android.widget.GridLayout.LayoutParams) feedbackComponentGrid.getChildAt(i).getLayoutParams();
			lp.columnSpec = GridLayout.spec(i % columns, GridLayout.CENTER);
			lp.rowSpec = GridLayout.spec(i - (i % columns), GridLayout.CENTER);
			lp.setGravity(Gravity.FILL_VERTICAL);
			feedbackComponentGrid.getChildAt(i).setLayoutParams(lp);
		}
	}

	protected void reorder()
	{
		reorder(feedbackComponentGrid.getColumnCount());
	}

	protected void addGridComponent(FeedbackComponentView feedbackComponentView) {
		Map.Entry<Feedback,FeedbackContainer.Valence> feedbackValenceEntry = feedbackComponentView.getFeedbackValenceEntry();
		feedbackValenceMap.put(feedbackValenceEntry.getKey(), feedbackValenceEntry.getValue());
		buildComponentGrid(getContext());
	}

	public void removeFeedback(Feedback feedback) {
		feedbackValenceMap.remove(feedback);
		buildComponentGrid(getContext());
	}
}
