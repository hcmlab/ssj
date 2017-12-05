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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import hcm.ssj.audio.AudioUtils;
import hcm.ssj.creator.R;

/**
 * Custom view to display audio waveform.
 */
public class WaveformView extends View
{
	private TextPaint textPaint;
	private Paint strokePaint;
	private Paint fillPaint;
	private Paint markerPaint;
	private Rect drawRect;

	private int width;
	private int height;
	private int mode;
	private int audioLength;
	private int markerPosition;
	private int sampleRate;
	private int channelNum;

	private float xStep;
	private float centerY;

	private short[] samples;
	private Bitmap cachedWaveformBitmap;
	private boolean showTextAxis = true;

	public WaveformView(Context context)
	{
		super(context);
		init(context, null, 0);
	}

	public WaveformView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, attrs, 0);
	}

	public WaveformView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	public void setSamples(short[] s)
	{
		samples = s;
		calculateAudioLength();
		onSamplesChanged();
	}

	public void setSampleRate(int rate)
	{
		sampleRate = rate;
		calculateAudioLength();
	}

	public void setChannelNum(int channels)
	{
		channelNum = channels;
		calculateAudioLength();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (cachedWaveformBitmap != null)
		{
			canvas.drawBitmap(cachedWaveformBitmap, null, drawRect, null);
		}
		if (markerPosition > -1 && markerPosition < audioLength)
		{
			canvas.drawLine(xStep * markerPosition, 0, xStep * markerPosition,
							height, markerPaint);
		}
	}

	/**
	 * Read XML attribute values of a waveform view and initialize colors.
	 * @param context Context which uses WaveformView.
	 * @param attrs Set of XML attributes for customization options.
	 * @param defStyle Integer which represents the default style.
	 */
	private void init(Context context, AttributeSet attrs, int defStyle)
	{
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.WaveformView, defStyle, 0);

		float strokeThickness = a.getFloat(R.styleable.WaveformView_waveformStrokeThickness, 1f);
		int strokeColor = a.getColor(R.styleable.WaveformView_waveformColor,
									 ContextCompat.getColor(context, R.color.colorPrimary));
		int fillColor = a.getColor(R.styleable.WaveformView_waveformFillColor,
								   ContextCompat.getColor(context, R.color.colorPrimary));
		int markerColor = a.getColor(R.styleable.WaveformView_playbackIndicatorColor,
						 			    ContextCompat.getColor(context, R.color.colorPrimary));
		int textColor = a.getColor(R.styleable.WaveformView_timecodeColor,
								   ContextCompat.getColor(context, R.color.colorPrimary));
		a.recycle();

		textPaint = new TextPaint();
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setColor(textColor);
		textPaint.setTextSize(12f);

		strokePaint = new Paint();
		strokePaint.setColor(strokeColor);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeWidth(strokeThickness);
		strokePaint.setAntiAlias(true);

		fillPaint = new Paint();
		fillPaint.setStyle(Paint.Style.FILL);
		fillPaint.setAntiAlias(true);
		fillPaint.setColor(fillColor);

		markerPaint = new Paint();
		markerPaint.setStyle(Paint.Style.STROKE);
		markerPaint.setStrokeWidth(0);
		markerPaint.setAntiAlias(true);
		markerPaint.setColor(markerColor);

		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		width = displayMetrics.widthPixels;
		height = displayMetrics.heightPixels;
		xStep = width / (audioLength * 1.0f);
		centerY = height / 2f;
		drawRect = new Rect(0, 0, width, height);
	}

	/**
	 * Cache audio length if samples, sample rate and number of channelNum is given.
	 */
	private void calculateAudioLength()
	{
		if (samples == null || sampleRate == 0 || channelNum == 0)
		{
			return;
		}
		audioLength = AudioUtils.calculateAudioLength(samples.length, sampleRate, channelNum);
	}

	/**
	 * Reset marker position, recalculate step along x-axis and re-create waveform.
	 */
	private void onSamplesChanged()
	{
		markerPosition = -1;
		xStep = width / (audioLength * 1.0f);
		createWaveform();
		invalidate();
	}

	/**
	 * Show audio waveform on screen.
	 */
	private void createWaveform()
	{
		if (width <= 0 || height <= 0 || samples == null)
		{
			return;
		}
		Canvas canvas;
		cachedWaveformBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(cachedWaveformBitmap);

		Path waveformPath = drawWaveform(width, height, samples);
		canvas.drawPath(waveformPath, fillPaint);
		canvas.drawPath(waveformPath, strokePaint);
	}

	/**
	 * Draw waveform as a line path from given audio samples.
	 * @param width Width of view.
	 * @param height Height of view.
	 * @param buffer Audio samples.
	 * @return Waveform path.
	 */
	private Path drawWaveform(int width, int height, short[] buffer)
	{
		Path waveformPath = new Path();
		float centerY = height / 2f;
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
