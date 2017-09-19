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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import hcm.ssj.creator.R;
import hcm.ssj.creator.main.TwoDScrollView;
import hcm.ssj.creator.view.ComponentView;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackContainer;

/**
 * Created by Antonio Grieco on 18.09.2017.
 */

public class FeedbackLevelLayout extends LinearLayout
{
	//layout
	private final static int LANDSCAPE_NUMBER_OF_BOXES = 10; //@todo adjust to different screen sizes (e.g. show all boxes on tablet)
	private final static int PORTRAIT_NUMBER_OF_BOXES = LANDSCAPE_NUMBER_OF_BOXES * 2;
	private final int iGridWidthNumberOfBoxes = 50; //chosen box number
	private final int iGridHeightNumberOfBoxes = 50; //chosen box number
	private int iOrientation = Configuration.ORIENTATION_UNDEFINED;
	private int iGridBoxSize = 0; //box size depends on screen width
	private int iGridPadWPix = 0; //left and right padding to center grid
	private int iGridPadHPix = 0; //top and bottom padding to center grid
	private int iSizeWidth = 0; //draw size width
	private int iSizeHeight = 0; //draw size height

	private final int level;
	private final Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap;
	private LinearLayout feedbackGrid;
	private TextView levelTextView;

	public FeedbackLevelLayout(Context context, int level, Map<Feedback, FeedbackContainer.Valence> feedbackValenceMap)
	{
		super(context);
		initView(context);
		this.level = level;
		this.feedbackValenceMap = feedbackValenceMap;
		initLayout();
	}

	private void initView(Context context)
	{
		LinearLayout.inflate(context, R.layout.single_level_layout, this);
		levelTextView = (TextView) this.findViewById(R.id.levelText);
		feedbackGrid = (LinearLayout) this.findViewById(R.id.levelComponents);
	}

	private void initLayout()
	{
		levelTextView.setText(String.valueOf(level));
		for (Map.Entry<Feedback, FeedbackContainer.Valence> feedbackValenceEntry : feedbackValenceMap.entrySet())
		{
			ComponentView componentView = new ComponentView(getContext(), feedbackValenceEntry.getKey());
			//componentView.layout(feedbackGrid.getLeft()+50*feedbackComponents.size(), feedbackGrid.getTop(), feedbackGrid.getLeft()+50*feedbackComponents.size()+50, feedbackGrid.getBottom());
			componentView.layout(0,0,10,10);
/*			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(50, 50, 50, 50);
			componentView.setLayoutParams(lp);*/

			feedbackGrid.addView(componentView);
		}
	}

}
