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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
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
	private GridLayout feedbackGrid;
	private TextView levelTextView;

	public FeedbackLevelLayout(Context context, int level, Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap)
	{
		super(context);

		this.level = level;
		this.feedbackValenceMap = feedbackValenceMap;

		LinearLayout.inflate(context, R.layout.single_level_layout, this);
		levelTextView = (TextView) this.findViewById(R.id.levelText);
		feedbackGrid = (GridLayout) this.findViewById(R.id.levelComponents);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		initLayout();
	}


	private void initLayout()
	{
		levelTextView.setText(String.valueOf(level));

		if (feedbackValenceMap == null)
		{
			return;
		}
		for (Map.Entry<Feedback, FeedbackContainer.Valence> feedbackValenceEntry : feedbackValenceMap.entrySet())
		{
			final ComponentView componentView = new ComponentView(getContext(), feedbackValenceEntry.getKey());

			int boxSize = feedbackGrid.getHeight() - feedbackGrid.getPaddingBottom() - feedbackGrid.getPaddingTop();
			int left = feedbackGrid.getChildCount()*boxSize + feedbackGrid.getPaddingBottom()*(feedbackGrid.getChildCount()) + feedbackGrid.getPaddingLeft();
			//componentView.layout(left,feedbackGrid.getPaddingTop(),left+boxSize,feedbackGrid.getPaddingTop()+boxSize);
			GridLayout.LayoutParams lp = new GridLayout.LayoutParams(new ViewGroup.LayoutParams(200, 200));
			lp.columnSpec = new GridLayout.Spec.DEFAULT_WEIGHT;
			componentView.setLayoutParams(lp);
			feedbackGrid.addView(componentView);
		}
		this.invalidate();
	}
}
