/*
 * LandmarkPainter.java
 * Copyright (c) 2021
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

package hcm.ssj.landmark;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import hcm.ssj.camera.CameraUtil;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 30.01.2019.
 */
public class LandmarkPainter extends Consumer
{
	/**
	 * All options for the camera painter
	 */
	public class Options extends OptionList
	{
		//values should be the same as in camera
		public final Option<Cons.ImageRotation> imageRotation = new Option<>("imageRotation", Cons.ImageRotation.MINUS_90, Cons.ImageRotation.class, "rotation of input picture");
		public final Option<Boolean> scale = new Option<>("scale", false, Boolean.class, "scale image to match surface size");
		public final Option<Boolean> useVisibility = new Option<>("useVisibility", false, Boolean.class, "use third dimension as landmark visibility");
		public final Option<Float> visibilityThreshold = new Option<>("visibilityThreshold", 0.5f, Float.class, "threshold if a landmark should be shown based on visibility value");
		public final Option<SurfaceView> surfaceView = new Option<>("surfaceView", null, SurfaceView.class, "the view on which the painter is drawn");
		public final Option<Float> landmarkRadius = new Option<>("landmarkRadius", 3.0f, Float.class, "radius of landmark circle");
		public final Option<Cons.DrawColor> drawColor = new Option<>("drawColor", Cons.DrawColor.WHITE, Cons.DrawColor.class, "landmark color");

		private Options()
		{
			addOptions();
		}
	}

	public LandmarkPainter()
	{
		_name = this.getClass().getSimpleName();
	}

	public final Options options = new Options();

	private int imageStreamIndex = -1;

	// Buffers
	private int[] argbData;
	private Bitmap inputBitmap;

	private SurfaceView surfaceViewInner;
	private SurfaceHolder surfaceHolder;

	private Paint landmarkPaint;

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
		if (stream_in.length != 2)
		{
			Log.e("Stream count not supported! Requires 1 image and 1 landmark stream");
			return;
		}

		if (stream_in[0].type == Cons.Type.IMAGE && stream_in[1].type == Cons.Type.FLOAT)
		{
			imageStreamIndex = 0;
		}
		else if (stream_in[0].type == Cons.Type.FLOAT && stream_in[1].type == Cons.Type.IMAGE)
		{
			imageStreamIndex = 1;
		}
		else
		{
			Log.e("Stream types not supported, must be IMAGE and FLOAT");
			return;
		}

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

		ImageStream in = (ImageStream) stream_in[imageStreamIndex];

		surfaceHolder = surfaceViewInner.getHolder();

		// Create buffers
		argbData = new int[in.width * in.height];
		inputBitmap = Bitmap.createBitmap(in.width, in.height, Bitmap.Config.ARGB_8888);

		landmarkPaint = new Paint();
		landmarkPaint.setColor(options.drawColor.get().color);
		landmarkPaint.setStyle(Paint.Style.FILL);

		// Fix visibility usage if configured wrong
		if (options.useVisibility.get() && stream_in[1 - imageStreamIndex].dim % 3 != 0 && stream_in[1 - imageStreamIndex].dim % 2 == 0)
		{
			options.useVisibility.set(false);
		}

		if (!options.useVisibility.get() && stream_in[1 - imageStreamIndex].dim % 2 != 0 && stream_in[1 - imageStreamIndex].dim % 3 == 0)
		{
			options.useVisibility.set(true);
		}
	}

	@Override
	protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
	{
		draw(stream_in[imageStreamIndex].ptrB(), stream_in[1 - imageStreamIndex].ptrF(), ((ImageStream) stream_in[imageStreamIndex]).format);
	}

	@Override
	public void flush(Stream[] stream_in) throws SSJFatalException
	{
		surfaceViewInner = null;
		argbData = null;
		surfaceHolder = null;
		inputBitmap.recycle();
		inputBitmap = null;
	}

	private void draw(final byte[] imageData, final float[] landmarkData, int imageFormat)
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
					canvas.drawColor(Color.BLACK);

					int canvasWidth = canvas.getWidth();
					int canvasHeight = canvas.getHeight();

					int bitmapWidth = inputBitmap.getWidth();
					int bitmapHeight = inputBitmap.getHeight();

					int finalWidth = bitmapWidth;
					int finalHeight = bitmapHeight;

					//decode color format
					decodeColor(imageData, bitmapWidth, bitmapHeight, imageFormat);

					// Fill bitmap with picture
					inputBitmap.setPixels(argbData, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

					// Adjust width/height for rotation
					switch (options.imageRotation.get())
					{
						case PLUS_90:
						case MINUS_90:
							int tmpWidth = bitmapWidth;
							bitmapWidth = bitmapHeight;
							bitmapHeight = tmpWidth;
							break;
					}

					// Scale bitmap
					if (options.scale.get())
					{
						float horizontalScale = canvasWidth / (float) bitmapWidth;
						float verticalScale = canvasHeight / (float) bitmapHeight;

						float scale = Math.min(horizontalScale, verticalScale);

						bitmapWidth = (int) (bitmapWidth * scale);
						bitmapHeight = (int) (bitmapHeight * scale);
					}

					finalWidth = bitmapWidth;
					finalHeight = bitmapHeight;

					// Restore width/height after scaling for rect placement
					switch (options.imageRotation.get())
					{
						case PLUS_90:
						case MINUS_90:
							int tmpWidth = bitmapWidth;
							bitmapWidth = bitmapHeight;
							bitmapHeight = tmpWidth;
							break;
					}

					int bitmapLeft = (canvasWidth - bitmapWidth) / 2; // could also do >> 1
					int bitmapTop = (canvasHeight - bitmapHeight) / 2;

					Rect dest = new Rect(bitmapLeft, bitmapTop, bitmapLeft + bitmapWidth, bitmapTop + bitmapHeight);

					// Rotate canvas coordinate system around the center of the image.
					canvas.save(); // canvas.save(Canvas.MATRIX_SAVE_FLAG);
					canvas.rotate(options.imageRotation.get().rotation, canvasWidth >> 1, canvasHeight >> 1);

					// Draw bitmap into rect
					canvas.drawBitmap(inputBitmap, null, dest, null);
					canvas.restore();

					// Draw landmarks
					if (landmarkData.length > 0)
					{
						float landmarkLeft = (canvas.getWidth() - finalWidth) / 2.0f;
						float landmarkTop = (canvas.getHeight() - finalHeight) / 2.0f;

						int landmarkDim = 2;

						if (options.useVisibility.get())
						{
							landmarkDim = 3;
						}

						for (int i = 0; i < landmarkData.length; i += landmarkDim)
						{
							boolean drawLandmark = true;

							if (options.useVisibility.get())
							{
								drawLandmark = landmarkData[i + 2] >= options.visibilityThreshold.get();
							}

							if (drawLandmark)
							{
								canvas.drawCircle(landmarkLeft + landmarkData[i] * finalWidth, landmarkTop + landmarkData[i + 1] * finalHeight, options.landmarkRadius.get(), landmarkPaint);
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

	private void decodeColor(final byte[] data, int width, int height, int format)
	{
		// TODO: implement missing conversions
		switch (format)
		{
			case ImageFormat.YV12:
			{
				throw new UnsupportedOperationException("Not implemented, yet");
			}
			case ImageFormat.YUV_420_888: //YV12_PACKED_SEMI
			{
				CameraUtil.decodeYV12PackedSemi(argbData, data, width, height);
				break;
			}
			case ImageFormat.NV21:
			{
				CameraUtil.convertNV21ToARGBInt(argbData, data, width, height);
				break;
			}
			case ImageFormat.FLEX_RGB_888:
			{
				CameraUtil.convertRGBToARGBInt(argbData, data, width, height);
				break;
			}
			default:
			{
				Log.e("Wrong color format");
				throw new RuntimeException();
			}
		}
	}
}
