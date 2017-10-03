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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import android.widget.GridLayout;
import android.support.v7.widget.GridLayoutManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.feedback.AuditoryFeedback;
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
		this.feedbackValenceMap = new HashMap<>();
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
		this.feedbackValenceMap.put(new AuditoryFeedback(), FeedbackContainer.Valence.DESIRABLE);
//
		LinearLayout.inflate(context, R.layout.single_level_layout, this);
		levelTextView = (TextView) this.findViewById(R.id.levelText);
		levelTextView.setText(String.valueOf(level));
		feedbackComponentGrid = (android.widget.GridLayout) this.findViewById(R.id.feedbackComponentGrid);


		feedbackComponentGrid.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				try
				{
					addComponents();
					return true;
				}
				finally
				{
					feedbackComponentGrid.getViewTreeObserver().removeOnPreDrawListener(this);
				}
			}
		});
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
	}

	private void addComponents() {
		if (feedbackValenceMap == null || feedbackValenceMap.isEmpty())
		{
			return;
		}
		feedbackComponentGrid.removeAllViews();
		feedbackComponentGrid.post(new Runnable() {
			@Override
			public void run()
			{
				for(Map.Entry<Feedback,FeedbackContainer.Valence> entry : feedbackValenceMap.entrySet())
				{
					Button b = new Button(getContext());
					b.setText("TEST");
					feedbackComponentGrid.addView(b);
				}
				final View v = feedbackComponentGrid.getChildAt(0);
				v.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw()
					{
						try
						{
							android.widget.GridLayout.LayoutParams lp1 = (android.widget.GridLayout.LayoutParams) v.getLayoutParams();
							int columns = (feedbackComponentGrid.getMeasuredWidth()/(v.getMeasuredWidth()+lp1.leftMargin+lp1.rightMargin));
							for(int i = 0;i<feedbackComponentGrid.getChildCount(); i++)
							{
								android.widget.GridLayout.LayoutParams lp = (android.widget.GridLayout.LayoutParams) feedbackComponentGrid.getChildAt(i).getLayoutParams();
								lp.columnSpec = GridLayout.spec(i%columns, GridLayout.CENTER);
								lp.rowSpec = GridLayout.spec(i-(i%columns), GridLayout.CENTER);
								lp.setGravity(Gravity.FILL_VERTICAL);
								feedbackComponentGrid.getChildAt(i).setLayoutParams(lp);
							}
							feedbackComponentGrid.setColumnCount(columns);
							return true;
						}
						finally
						{
							feedbackComponentGrid.getChildAt(0).getViewTreeObserver().removeOnPreDrawListener(this);
						}
					}
				});
			}
		});
	}


	/*
	private void initLayout()
	{
		levelTextView.setText(String.valueOf(level));
		feedbackComponentGrid.removeAllViews();

		if (feedbackValenceMap == null)
		{
			return;
		}
		//this.addView(levelTextView);
		for (Map.Entry<Feedback, FeedbackContainer.Valence> feedbackValenceEntry : feedbackValenceMap.entrySet())
		{
			final ComponentView componentView = new ComponentView(getContext(), feedbackValenceEntry.getKey());
			final LinearLayout.LayoutParams lp = new LayoutParams(200,200);
			this.post(new Runnable() {
				@Override
				public void run()
				{
					componentView.setLayoutParams(lp);
					//componentView.invalidate();
					feedbackComponentGrid.addView(componentView);
				}
			});
		}
	}*/

	private class FeedbackGridAdapter extends BaseAdapter
	{
		private final List<Map.Entry<Feedback, FeedbackContainer.Valence>> feedbackValenceList;

		public FeedbackGridAdapter(Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap)
		{
			feedbackValenceList = Collections.list(Collections.enumeration(feedbackValenceMap.entrySet()));
		}

		@Override
		public int getCount()
		{
			return feedbackValenceList.size();
		}

		@Override
		public Object getItem(int i)
		{
			return feedbackValenceList.get(i);
		}

		@Override
		public long getItemId(int i)
		{
			return feedbackValenceList.get(i).hashCode();
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup)
		{
			CheckBox tb = new CheckBox(viewGroup.getContext());
			tb.setLayoutParams(
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			);
			return tb;
		}
	}
}
