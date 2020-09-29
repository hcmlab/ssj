/*
 * DimensionalPainter.java
 * Copyright (c) 2020
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

package hcm.ssj.graphic;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 12.02.2020.
 */
public class GridPainter extends Consumer
{
	/**
	 * All options for the camera painter
	 */
	public class Options extends OptionList
	{
		public final Option<String> minLabelX = new Option<>("minLabelX", "min x value", String.class, "minimum X-axis label");
		public final Option<String> maxLabelX = new Option<>("maxLabelX", "max x value", String.class, "maximum X-axis label");
		public final Option<String> minLabelY = new Option<>("minLabelY", "min y value", String.class, "minimum Y-axis label");
		public final Option<String> maxLabelY = new Option<>("maxLabelY", "max y value", String.class, "maximum Y-axis label");
		public final Option<Double> minValueX = new Option<>("minValueX", -1., Double.class, "minimum X-axis value");
		public final Option<Double> maxValueX = new Option<>("maxValueX", 1., Double.class, "maximum X-axis value");
		public final Option<Double> minValueY = new Option<>("minValueY", -1., Double.class, "minimum Y-axis value");
		public final Option<Double> maxValueY = new Option<>("maxValueY", 1., Double.class, "maximum Y-axis value");
		public final Option<Boolean> limitValues = new Option<>("limitValues", true, Boolean.class, "Limit values to defined x and y ranges");
		public final Option<SurfaceView> surfaceView = new Option<>("surfaceView", null, SurfaceView.class, "the view on which the painter is drawn");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private class GridPoint
	{
		private double x;
		private double y;

		public void set(double x, double y)
		{
			setX(x);
			setY(y);
		}

		public float getRelativeX()
		{
			return (float) ((x - options.minValueX.get()) / valueRangeX);
		}

		public void setX(double x)
		{
			this.x = x;
		}

		public float getRelativeY()
		{
			return (float) ((y - options.minValueY.get()) / valueRangeY);
		}

		public void setY(double y)
		{
			this.y = y;
		}

		public void clip()
		{
			x = Math.min(Math.max(x, options.minValueX.get()), options.maxValueX.get());
			y = Math.min(Math.max(y, options.minValueY.get()), options.maxValueY.get());
		}

		public boolean inRange()
		{
			return x >= options.minValueX.get() && x <= options.maxValueX.get() &&
					y >= options.minValueY.get() && y <= options.maxValueY.get();
		}
	}

	final private int[] colors = new int[]{0xff0F9D58, 0xffed1c24, 0xffffd54f, 0xff29b6f6, 0xff0077cc, 0xffff9900, 0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};

	private static final int TEXT_SIZE = 35;
	private static final int INDICATOR_SIZE = 10;

	private SurfaceView surfaceViewInner;
	private SurfaceHolder surfaceHolder;

	private Paint indicatorPaint;
	private Paint linePaint;
	private int textHeight;

	private String[] streamDescription;

	private Paint textPaint;

	private List<List<GridPoint>> pointList;

	private double valueRangeX;
	private double valueRangeY;

	public GridPainter()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	protected void init(Stream[] stream_in) throws SSJException
	{
		if (options.surfaceView.get() == null)
		{
			Log.w("surfaceView isn't set");
		}
		else
		{
			surfaceViewInner = options.surfaceView.get();
		}
	}

	@Override
	public void enter(Stream[] stream_in) throws SSJFatalException
	{
		synchronized (this)
		{
			if (surfaceViewInner == null)
			{
				// Wait for surfaceView creation
				try
				{
					this.wait();
				}
				catch (InterruptedException e)
				{
					Log.e("Error while waiting for surfaceView creation", e);
				}
			}
		}

		surfaceViewInner.setZOrderOnTop(true);
		surfaceHolder = surfaceViewInner.getHolder();
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

		indicatorPaint = new Paint();
		indicatorPaint.setStyle(Paint.Style.FILL);
		indicatorPaint.setAntiAlias(true);

		linePaint = new Paint();
		linePaint.setColor(Color.parseColor("#000000"));
		linePaint.setStrokeWidth(3);
		linePaint.setStyle(Paint.Style.FILL);

		textPaint = new Paint();
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(TEXT_SIZE);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.LEFT);

		textHeight = (int) (textPaint.descent() - textPaint.ascent());

		pointList = new ArrayList<>();

		streamDescription = new String[stream_in.length];

		valueRangeX = options.maxValueX.get() - options.minValueX.get();
		valueRangeY = options.maxValueY.get() - options.minValueY.get();

		for (int i = 0; i < stream_in.length; i++)
		{
			if (stream_in[i].dim != 2)
			{
				Log.e("Stream " + i + " does not have two dimensions!");
				return;
			}

			streamDescription[i] = stream_in[i].desc[0] + "/" + stream_in[i].desc[1];

			ArrayList<GridPoint> streamPointList = new ArrayList<>();

			for (int j = 0; j < stream_in[i].num; j++)
			{
				streamPointList.add(new GridPoint());
			}

			pointList.add(streamPointList);
		}
	}

	@Override
	protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
	{
		for (int i = 0; i < stream_in.length; i++)
		{
			switch (stream_in[i].type)
			{
				case CHAR:
				{
					char[] values = stream_in[i].ptrC();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
				case SHORT:
				{
					short[] values = stream_in[i].ptrS();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
				case INT:
				{
					int[] values = stream_in[i].ptrI();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
				case LONG:
				{
					long[] values = stream_in[i].ptrL();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
				case FLOAT:
				{
					float[] values = stream_in[i].ptrF();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
				case DOUBLE:
				{
					double[] values = stream_in[i].ptrD();

					for (int j = 0; j < stream_in[i].num; j++)
					{
						pointList.get(i).get(j).set(values[stream_in[i].dim * j], values[stream_in[i].dim * j + 1]);
					}

					break;
				}
			}
		}

		draw(pointList);
	}

	@Override
	public void flush(Stream[] stream_in) throws SSJFatalException
	{
		surfaceViewInner = null;
		surfaceHolder = null;
	}

	private void draw(List<List<GridPoint>> points)
	{
		Canvas canvas = null;

		if (surfaceHolder == null)
		{
			return;
		}

		try
		{
			synchronized (surfaceHolder)
			{
				canvas = surfaceHolder.lockCanvas();

				if (canvas != null)
				{
					// Clear canvas.
					canvas.drawColor(Color.WHITE);

					int canvasWidth = canvas.getWidth();
					int canvasHeight = canvas.getHeight();

					int axisLength = Math.min(canvasWidth, canvasHeight);

					float canvasCenterX = canvasWidth / 2f;
					float canvasCenterY = canvasHeight / 2f;

					float startX = (canvasWidth - axisLength) / 2f;
					float endX = startX + axisLength;

					float startY = (canvasHeight - axisLength) / 2f;
					float endY = startY + axisLength;

					float contentStartX = startX + textHeight;
					float contentEndX = endX - textHeight;
					float contentStartY = startY + textHeight;
					float contentEndY = endY - textHeight;

					float contentWidth = contentEndX - contentStartX;
					float contentHeight = contentEndY - contentStartY;

					// Draw horizontal line
					canvas.drawLine(contentStartX, canvasCenterY, contentEndX, canvasCenterY, linePaint);

					// Draw vertical line
					canvas.drawLine(canvasCenterX, contentStartY, canvasCenterX, contentEndY, linePaint);

					// Draw y-axis labels
					drawTextCentered(canvas, options.maxLabelY.get(), canvasCenterX, startY, true);
					drawTextCentered(canvas, options.minLabelY.get(), canvasCenterX, endY, false);

					// Draw x-axis labels
					canvas.save();
					canvas.rotate(-90, canvasCenterX, canvasCenterY);

					drawTextCentered(canvas, options.minLabelX.get(), canvasCenterX, startY, true);
					drawTextCentered(canvas, options.maxLabelX.get(), canvasCenterX, endY, false);

					canvas.restore();

					// Draw indicators
					for (int i = 0; i < points.size(); i++)
					{
						indicatorPaint.setColor(colors[i % colors.length]);

						for (GridPoint point : points.get(i))
						{
							if (options.limitValues.get())
							{
								point.clip();
							}

							if (point.inRange())
							{
								canvas.drawCircle(contentStartX + contentWidth * point.getRelativeX(), contentEndY - contentHeight * point.getRelativeY(), INDICATOR_SIZE, indicatorPaint);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			Log.e("Error while drawing on canvas", e);
		}
		finally
		{
			// Always try to unlock a locked canvas to keep the surface in a consistent state
			if (canvas != null)
			{
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void drawTextCentered(Canvas canvas, String text, float x, float y, boolean alignTop)
	{
		// see https://stackoverflow.com/questions/11120392/android-center-text-on-canvas

		Rect bounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), bounds);

		float posX = x - bounds.width() / 2f - bounds.left;
		float posY;

		if (alignTop)
		{
			posY = y + bounds.height() - bounds.bottom;
		}
		else
		{
			posY = y - bounds.bottom;
		}

		canvas.drawText(text, posX, posY, textPaint);
	}
}
