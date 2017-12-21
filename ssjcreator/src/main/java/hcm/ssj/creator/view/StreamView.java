/*
 * StreamView.java
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

import hcm.ssj.core.stream.Stream;

/**
 * Plots stream data on canvas.
 */
public class StreamView extends View
{
	private static final int LAYOUT_HEIGHT = 350;
	private static final int LAYOUT_MARGIN_TOP = 20;
	private static final int LAYOUT_MARGIN_BOTTOM = 20;
	private static final int LAYOUT_MARGIN_LEFT = 25;
	private static final int LAYOUT_MARGIN_RIGHT = 0;

	private Stream stream;
	private Bitmap streamBitmap;
	private Rect drawRect;

	private int width;
	private int height;

	public StreamView(Context context, Stream s)
	{
		super(context);
		stream = s;
		init();
	}

	public StreamView(Context context)
	{
		super(context);
		init();
	}

	public StreamView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public StreamView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH)
	{
		width = getMeasuredWidth();
		height = getMeasuredHeight();
		init();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (streamBitmap != null)
		{
			canvas.drawBitmap(streamBitmap, null, drawRect, null);
		}
	}

	private void init()
	{
		drawRect = new Rect(0, 0, width, height);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LAYOUT_HEIGHT);
		layoutParams.setMargins(LAYOUT_MARGIN_LEFT, LAYOUT_MARGIN_TOP,
								LAYOUT_MARGIN_RIGHT, LAYOUT_MARGIN_BOTTOM);
		setLayoutParams(layoutParams);
		createPlot();
	}

	private void createPlot()
	{
		if (width <= 0 || height <= 0 || stream == null)
		{
			return;
		}
		Canvas canvas;
		streamBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(streamBitmap);

		ArrayList<Path> streamPaths = drawStreams();
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1.5f);

		for (Path path : streamPaths)
		{
			canvas.drawPath(path, paint);
		}
		invalidate();
	}

	private ArrayList<Path> drawStreams()
	{
		float[] data = stream.ptrF();
		float max = findMax(data);
		float centerY = height / 2.0f;
		ArrayList<Path> paths = new ArrayList<>();
		for (int i = 0; i < stream.dim; i++)
		{
			paths.add(new Path());
		}

		for (int col = 0; col < stream.dim; col++)
		{
			int x = 0;
			paths.get(col).moveTo(x, centerY - ((data[col] / max) * centerY));
			for (int i = col; i < data.length; i += stream.dim)
			{
				paths.get(col).lineTo(x++, centerY - ((data[i] / max) * centerY));
			}
		}

		return paths;
	}

	private float findMax(float[] array)
	{
		float max = Float.MIN_VALUE;
		for (float element : array)
		{
			if (element > max)
			{
				max = element;
			}
		}
		return max;
	}
}
