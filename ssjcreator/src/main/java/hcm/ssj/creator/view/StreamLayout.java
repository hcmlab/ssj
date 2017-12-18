/*
 * StreamLayout.java
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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import hcm.ssj.creator.R;

public class StreamLayout extends LinearLayout
{
	private int markerPosition = -1;
	private int width;
	private int height;
	private int audioLength;
	float xStep;

	public StreamLayout(Context context)
	{
		super(context);
	}

	public StreamLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public StreamLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setMarkerPosition(int position)
	{
		markerPosition = position;
		postInvalidate();
	}

	public void setAudioLength(int length)
	{
		audioLength = length;
		xStep = width / (audioLength * 1.0f);
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		super.dispatchDraw(canvas);

		Paint paint = new Paint();
		paint.setStrokeWidth(4);
		paint.setColor(getResources().getColor(R.color.colorBlack));

		// Draw playback marker.
		if (markerPosition > -1 && markerPosition < audioLength)
		{
			canvas.drawLine(xStep * markerPosition, 0, xStep * markerPosition,
							height, paint);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh)
	{
		width = getMeasuredWidth();
		height = getMeasuredHeight();
	}
}
