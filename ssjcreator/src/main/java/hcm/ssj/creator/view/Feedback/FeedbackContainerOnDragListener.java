/*
 * FeedbackContainerOnDragListener.java
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

import android.content.Context;
import android.graphics.Rect;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ScrollView;

import hcm.ssj.creator.main.TwoDScrollView;

import static android.view.DragEvent.*;

/**
 * Created by Antonio Grieco on 04.10.2017.
 */

public class FeedbackContainerOnDragListener implements View.OnDragListener
{
	private final ScrollView scrollView;
	private static ImageView recycleBin;

	public FeedbackContainerOnDragListener(ScrollView scrollView)
	{
		this.scrollView = scrollView;
	}

	@Override
	public boolean onDrag(final View v, DragEvent event)
	{
		switch (event.getAction())
		{
			case ACTION_DRAG_STARTED:
				if (v instanceof FeedbackLevelLayout)
				{
					createRecycleBin(v, (View)event.getLocalState());
				}
				break;
			case ACTION_DROP:
				if (event.getLocalState() instanceof FeedbackComponentView)
				{
					if (v instanceof FeedbackLevelLayout)
					{
						((FeedbackLevelLayout) v).addGridComponent((FeedbackComponentView) event.getLocalState());
					}
				}
				break;
			case ACTION_DRAG_ENDED:
				// Set currently draged to false no matter where the drag ended, to force normal painting.
				if(event.getLocalState() instanceof FeedbackComponentView)
					((FeedbackComponentView) event.getLocalState()).setCurrentlyDraged(false);
				if(recycleBin != null)
				{
					recycleBin.invalidate();
					((ViewGroup) recycleBin.getParent()).removeView(recycleBin);
					recycleBin = null;
				}
				break;
		}
		return true;
	}

	private void createRecycleBin(View v, View localState) {
		if (recycleBin == null)
		{
			recycleBin = new ImageView(v.getContext());
			recycleBin.setImageResource(android.R.drawable.ic_menu_delete);
			//determine shown width of the view
			Rect rectSizeDisplayed = new Rect();
			v.getGlobalVisibleRect(rectSizeDisplayed);
			int width = rectSizeDisplayed.width();
			int height = rectSizeDisplayed.height();
			//determine scroll changes
			int scrollX = 0, scrollY = 0;
			scrollX = scrollView.getScrollX();
			scrollY = scrollView.getScrollY();
			width += scrollX;
			height += scrollY;
			int gridBoxSize = 20;
			//place recycle bin
			recycleBin.layout(width - (gridBoxSize * 3), height - (gridBoxSize * 3), width, height);
			scrollView.addView(recycleBin);
		}
	}
}
