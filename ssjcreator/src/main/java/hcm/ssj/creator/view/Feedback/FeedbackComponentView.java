/*
 * FeedbackComponentView.java
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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.Map;

import hcm.ssj.creator.view.ComponentView;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackContainer;

/**
 * Created by Antonio Grieco on 04.10.2017.
 */

public class FeedbackComponentView extends ComponentView
{
	private Map.Entry<Feedback, FeedbackContainer.Valence> feedbackValenceEntry;

	private Paint dragBoxPaint;

	private boolean currentlyDraged = false;

	public FeedbackComponentView(Context context, Map.Entry<Feedback, FeedbackContainer.Valence> feedbackValenceEntry)
	{
		super(context, feedbackValenceEntry.getKey());

		this.feedbackValenceEntry = feedbackValenceEntry;

		OnLongClickListener onTouchListener = new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				ClipData.Item item = new ClipData.Item("DragEvent");
				ClipData dragData = new ClipData("DragEvent", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
				DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
				v.startDrag(dragData, shadowBuilder, v, 0);
				((FeedbackComponentView) v).setCurrentlyDraged(true);
				return true;
			}
		};
		this.setOnLongClickListener(onTouchListener);

		initPaints();
	}

	private void initPaints()
	{
		dragBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		dragBoxPaint.setStyle(Paint.Style.FILL);
		dragBoxPaint.setColor(Color.DKGRAY);
	}

	public Map.Entry<Feedback, FeedbackContainer.Valence> getFeedbackValenceEntry()
	{
		return feedbackValenceEntry;
	}

	public void setCurrentlyDraged(boolean currentlyDraged)
	{
		this.currentlyDraged = currentlyDraged;
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		if(!currentlyDraged)
		{
			super.onDraw(canvas);
		}
		else
		{
			canvas.save();
			canvas.drawRect(0,0,getWidth(),getHeight(), dragBoxPaint);
			invalidate();
			canvas.restore();
		}
	}
}
