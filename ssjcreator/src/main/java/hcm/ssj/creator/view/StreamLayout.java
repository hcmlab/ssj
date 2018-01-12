/*
 * StreamLayout.java
 * Copyright (c) 2018
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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Locale;

import hcm.ssj.creator.R;

/**
 * Layout group that contains custom views representing different stream files.
 * This layout group draws time axis with corresponding time-steps as well as playback marker
 * that is moved forward as audio files are being played.
 */
public class StreamLayout extends LinearLayout
{
	private static final int TIME_AXIS_OFFSET = 80;
	private static final int TIME_CODE_OFFSET = 25;
	private static final int PADDING = 25;
	private static final int TIME_STEP_PEG_LENGTH = 20;
	private static final int TEXT_SIZE = 36;
	private static final int AXIS_STROKE_WIDTH = 4;
	private static final int MARKER_ORIGIN = PADDING;
	private static final int MARKER_WIDTH = 4;
	private static final boolean ENABLE_ANTI_ALIAS = true;

	private int markerProgress = MARKER_ORIGIN;
	private int width;
	private int height;
	private int maxAudioLength;
	float xStep;

	private TextPaint textPaint;
	private Paint axisPaint;
	private Paint markerPaint;

	public StreamLayout(Context context)
	{
		super(context);
		init(context, null, 0);
	}

	public StreamLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, attrs, 0);
	}

	public StreamLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	/**
	 * Updates the position of the playback marker.
	 * @param progress The playback progress of the audio file.
	 */
	public void setMarkerProgress(int progress)
	{
		markerProgress = progress;
		postInvalidate();
	}

	/**
	 * Puts the playback marker at the start position.
	 */
	public void resetMarker()
	{
		setMarkerProgress(MARKER_ORIGIN);
	}

	/**
	 * Keeps track of maximal audio length and recalculates movement step of playback marker along
	 * time axis.
	 * @param length The new length of the longest audio file in StreamLayout.
	 */
	public void setMaxAudioLength(int length)
	{
		maxAudioLength = length;
		xStep = width / (maxAudioLength * 1.0f);
	}

	@Override
	public boolean performClick()
	{
		super.performClick();
		return true;
	}

	@Override
	public void onViewAdded(View view)
	{
		int currentLength;
		try
		{
			currentLength = ((WaveformView) view).getAudioLength();
		}
		catch (ClassCastException e)
		{
			return;
		}
		// Keep track of the length of the longest audio file.
		if (currentLength > maxAudioLength)
		{
			setMaxAudioLength(currentLength);
		}
		// Add horizontal line to separate multiple waveforms.
		if (getChildCount() > 1)
		{
			addSeparator();
		}
		// Rescale width of all child views according to the length of the longest audio file.
		for (int i = 0; i < getChildCount(); i++)
		{
			try
			{
				WaveformView waveformView = (WaveformView) getChildAt(i);
				float factor  = (float) waveformView.getAudioLength() / maxAudioLength;
				waveformView.setWidthScale(factor);
			}
			catch (ClassCastException e)
			{
				// Empty on purpose, as we only rescale waveform views and not horizontal lines.
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		super.dispatchDraw(canvas);

		if (maxAudioLength == 0)
		{
			return;
		}
		drawTimeAxis(canvas);

		if (markerProgress > MARKER_ORIGIN && markerProgress < maxAudioLength)
		{
			float markerPosition = xStep * markerProgress + MARKER_ORIGIN;
			// Move marker forward as audio is being played.
			canvas.drawLine(markerPosition, 0, markerPosition,
							height - TIME_AXIS_OFFSET, markerPaint);
		}
		else
		{
			// Put marker at the origin.
			canvas.drawLine(MARKER_ORIGIN, 0, MARKER_ORIGIN,
							height - TIME_AXIS_OFFSET, markerPaint);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight)
	{
		width = getMeasuredWidth();
		height = getMeasuredHeight();
	}

	/**
	 * Retrieves custom XML attribute values of StreamLayout and initializes
	 * colors and text size.
	 * @param context Context of activity that uses the StreamLayout.
	 * @param attrs Set of attributes.
	 * @param defStyle Default style.
	 */
	private void init(Context context, AttributeSet attrs, int defStyle)
	{
		final TypedArray a = getContext().obtainStyledAttributes(attrs,
																 R.styleable.StreamLayout,
																 defStyle, 0);

		int timeCodeColor = a.getColor(R.styleable.StreamLayout_timeCodeColor,
									   ContextCompat.getColor(context, R.color.colorBlack));
		int axisColor = a.getColor(R.styleable.StreamLayout_axisColor,
								   ContextCompat.getColor(context, R.color.colorBlack));
		int markerColor = a.getColor(R.styleable.StreamLayout_markerColor,
								     ContextCompat.getColor(context, R.color.colorBlack));
		a.recycle();

		textPaint = new TextPaint();
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setColor(timeCodeColor);
		textPaint.setTextSize(TEXT_SIZE);

		axisPaint = new Paint();
		axisPaint.setStyle(Paint.Style.STROKE);
		axisPaint.setStrokeWidth(AXIS_STROKE_WIDTH);
		axisPaint.setAntiAlias(ENABLE_ANTI_ALIAS);
		axisPaint.setColor(axisColor);

		markerPaint = new Paint();
		markerPaint.setStrokeWidth(MARKER_WIDTH);
		markerPaint.setStyle(Paint.Style.STROKE);
		markerPaint.setPathEffect(new DashPathEffect(new float[] {5, 5}, 0));
		markerPaint.setColor(markerColor);
	}

	/**
	 * Draws time axis with appropriate time steps as labels.
	 * @param canvas Canvas to draw the axis on.
	 */
	private void drawTimeAxis(Canvas canvas)
	{
		int seconds = maxAudioLength / 1000;
		float xStep = width / (maxAudioLength / 1000f);
		float textWidth = textPaint.measureText("10.00");
		float secondStep = (textWidth * seconds * 2) / width;
		secondStep = Math.max(secondStep, 1) - 0.5f;

		for (float i = 0; i <= seconds; i += secondStep)
		{
			// Draw timestamp labels.
			canvas.drawText(String.format(Locale.ENGLISH, "%.1f", i),
							PADDING + i * xStep, height - TIME_CODE_OFFSET, textPaint);

			// Make every second time step peg half the length.
			int cutOff = i % 1 != 0 ? TIME_STEP_PEG_LENGTH / 2 : 0;
			canvas.drawLine(PADDING + i * xStep, height - TIME_AXIS_OFFSET,
							PADDING + i * xStep,
							height - TIME_AXIS_OFFSET + TIME_STEP_PEG_LENGTH - cutOff,
							axisPaint);
		}
		canvas.drawLine(0, height - TIME_AXIS_OFFSET, width,
						height - TIME_AXIS_OFFSET, axisPaint);
	}

	/**
	 * Adds vertical line to separate multiple stream file visualizations.
	 */
	private void addSeparator()
	{
		View separator = new View(getContext());
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 4);
		separator.setLayoutParams(params);
		separator.setBackgroundColor(getResources().getColor(R.color.colorSeparator));
		addView(separator, 1);
	}
}
