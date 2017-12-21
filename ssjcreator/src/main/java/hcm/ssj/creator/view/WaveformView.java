/*
 * WaveformView.java
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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import hcm.ssj.audio.AudioUtils;
import hcm.ssj.creator.R;

/**
 * View that draws audio file waveform.
 */
public class WaveformView extends View
{
	private static final boolean ENABLE_ANTI_ALIAS = true;
	private static final float STROKE_THICKNESS = 4;
	private static final int LAYOUT_HEIGHT = 350;
	private static final int LAYOUT_MARGIN_TOP = 20;
	private static final int LAYOUT_MARGIN_BOTTOM = 20;
	private static final int LAYOUT_MARGIN_LEFT = 25;
	private static final int LAYOUT_MARGIN_RIGHT = 0;

	private Paint strokePaint;
	private Paint fillPaint;
	private Rect drawRect;

	private int width;
	private int height;
	private int audioLength;

	private float scalingFactor = 1;

	private short[] samples;
	private Bitmap waveformBitmap;

	public WaveformView(Context context)
	{
		super(context);
		init(null, 0);
	}

	public WaveformView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs, 0);
	}

	public WaveformView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	/**
	 * Sets samples that are used to draw the waveform and draws the waveform based on samples given.
	 * @param s The result of {@link hcm.ssj.audio.AudioDecoder#getSamples()}
	 */
	public void setSamples(short[] s)
	{
		samples = s;
		createWaveform();
	}

	/**
	 * Sets the length of the audio file to visualize.
	 * @param length The length of the audio file that this WaveformView is visualizing.
	 */
	public void setAudioLength(int length)
	{
		audioLength = length;
	}

	/**
	 * Returns the length of the audio file that this WaveformView is visualizing.
	 * @return Length of the audio file in milliseconds.
	 */
	public int getAudioLength()
	{
		return audioLength;
	}

	/**
	 * Rescales the width of this WaveformView according to the given factor.
	 * The new width is calculated as a product of old width and a given scaling factor, thereafter
	 * the view is redrawn for the changes to take effect.
	 * @param factor Factor to scale the width of WaveformView by. Should be bigger than 0 and less
	 *               than 1.
	 */
	public void setWidthScale(float factor)
	{
		scalingFactor = factor;
		rescale();
		invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH)
	{
		width = getMeasuredWidth();
		height = getMeasuredHeight();
		rescale();
		createWaveform();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (waveformBitmap != null)
		{
			canvas.drawBitmap(waveformBitmap, null, drawRect, null);
		}
	}

	/**
	 * Shrinks the width of this WaveformView according to scaling factor.
	 */
	private void rescale()
	{
		int newWidth = (int) (width * scalingFactor);
		drawRect = new Rect(getPaddingLeft(), getPaddingTop(),
							newWidth - getPaddingLeft() - getPaddingRight(),
							height - getPaddingTop() - getPaddingBottom());
	}

	/**
	 * Reads XML attribute values of WaveformView and initializes colors.
	 * @param attrs Set of XML attributes for customization options.
	 * @param defStyle Integer which represents the default style.
	 */
	private void init(AttributeSet attrs, int defStyle)
	{
		final TypedArray a = getContext().obtainStyledAttributes(attrs,
																 R.styleable.WaveformView,
																 defStyle, 0);

		int strokeColor = getResources().getColor(R.color.colorWaveform);
		int fillColor = getResources().getColor(R.color.colorWaveformFill);
		a.recycle();

		strokePaint = new Paint();
		strokePaint.setColor(strokeColor);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeWidth(STROKE_THICKNESS);
		strokePaint.setAntiAlias(ENABLE_ANTI_ALIAS);

		fillPaint = new Paint();
		fillPaint.setStyle(Paint.Style.FILL);
		fillPaint.setAntiAlias(ENABLE_ANTI_ALIAS);
		fillPaint.setColor(fillColor);

		drawRect = new Rect(0, 0, width, height);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LAYOUT_HEIGHT);
		layoutParams.setMargins(LAYOUT_MARGIN_LEFT, LAYOUT_MARGIN_TOP,
								LAYOUT_MARGIN_RIGHT, LAYOUT_MARGIN_BOTTOM);
		setLayoutParams(layoutParams);
	}

	/**
	 * Shows audio waveform on screen.
	 */
	private void createWaveform()
	{
		if (width <= 0 || height <= 0 || samples == null)
		{
			return;
		}
		Canvas canvas;
		waveformBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(waveformBitmap);

		Path waveformPath = drawWaveform(width, height, samples);
		canvas.drawPath(waveformPath, fillPaint);
		canvas.drawPath(waveformPath, strokePaint);
		invalidate();
	}

	/**
	 * Draws waveform as a line path from given audio samples.
	 * @param width Width of view.
	 * @param height Height of view.
	 * @param buffer Audio samples.
	 * @return Waveform path.
	 */
	private Path drawWaveform(int width, int height, short[] buffer)
	{
		Path waveformPath = new Path();
		float centerY = height / 2.0f;
		float max = Short.MAX_VALUE;

		short[][] extremes = AudioUtils.getExtremes(buffer, width);

		// Start path at the origin.
		waveformPath.moveTo(0, centerY);

		// Draw maximums.
		for (int x = 0; x < width; x++)
		{
			short sample = extremes[x][0];
			float y = centerY - ((sample / max) * centerY);
			waveformPath.lineTo(x, y);
		}

		// Draw minimums.
		for (int x = width - 1; x >= 0; x--)
		{
			short sample = extremes[x][1];
			float y = centerY - ((sample / max) * centerY);
			waveformPath.lineTo(x, y);
		}

		waveformPath.close();
		return waveformPath;
	}
}
