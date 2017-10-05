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
import android.view.DragEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * Created by hiwi on 04.10.2017.
 */

public class FeedbackContainerOnDragListener implements View.OnDragListener
{
	private final Context context;

	public FeedbackContainerOnDragListener(Context context)
	{
		this.context = context;
	}

	@Override
	public boolean onDrag(final View v, DragEvent event)
	{
		switch (event.getAction())
		{
			case DragEvent.ACTION_DROP:
				if (event.getLocalState() instanceof FeedbackComponentView)
				{
					if (v instanceof FeedbackLevelLayout)
					{
						((FeedbackLevelLayout) v).addGridComponent((FeedbackComponentView) event.getLocalState());
					}
					else
					{
						((FeedbackComponentView) event.getLocalState()).addToLastContainingLayout();
					}
				}
				break;
		}
		return true;
	}
}
