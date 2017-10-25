/*
 * CameraPainter.java
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

package hcm.ssj.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.EventListener;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Camera painter for SSJ.<br>
 * Created by Frank Gaibler on 21.01.2016.
 */
public class CameraPainter extends Consumer implements EventListener
{
    /**
     * All options for the camera painter
     */
    public class Options extends OptionList
    {
        //values should be the same as in camera
        public final Option<Integer> orientation = new Option<>("orientation", 90, Integer.class, "orientation of input picture");
        public final Option<Boolean> scale = new Option<>("scale", false, Boolean.class, "scale picture to match surface size");
        public final Option<Boolean> showBestMatch = new Option<>("showBestMatch", false, Boolean.class, "show object label of the best match");
        public final Option<SurfaceView> surfaceView = new Option<>("surfaceView", null, SurfaceView.class, "the view on which the painter is drawn");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    //buffers
    private int[] iaRgbData;
    private Bitmap bitmap;
    //
    private SurfaceView surfaceViewInner = null;
    private SurfaceHolder surfaceHolder;

    private String bestMatch;
    private int textSize = 35;

    private Paint interiorPaint;
    private Paint exteriorPaint;

    /**
     *
     */
    public CameraPainter()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in Stream[]
	 */
    @Override
    protected void init(Stream[] stream_in) throws SSJException
    {
        super.init(stream_in);
        if (options.surfaceView.get() == null)
        {
            Log.w("surfaceView isn't set");
        }
        else
        {
            surfaceViewInner = options.surfaceView.get();
        }
    }

    /**
	 * @param stream_in Stream[]
	 */
    @Override
    public final void enter(Stream[] stream_in) throws SSJFatalException
    {
        if (stream_in.length != 1)
        {
            Log.e("Stream count not supported");
            return;
        }
        if (stream_in[0].type != Cons.Type.IMAGE)
        {
            Log.e("Stream type not supported");
            return;
        }
        synchronized (this)
        {
            if (surfaceViewInner == null)
            {
                //wait for surfaceView creation
                try
                {
                    this.wait();
                } catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }

        ImageStream in = (ImageStream)stream_in[0];

        surfaceHolder = surfaceViewInner.getHolder();
        iaRgbData = new int[in.width * in.height];
        //set bitmap
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        bitmap = Bitmap.createBitmap(in.width, in.height, conf);

        //register listener
		if (_evchannel_in != null && _evchannel_in.size() != 0)
		{
			for (EventChannel ch : _evchannel_in)
			{
				ch.addEventListener(this);
			}
		}

        if (options.showBestMatch.get())
        {
            interiorPaint = new Paint();
            interiorPaint.setTextSize(textSize);
            interiorPaint.setColor(Color.WHITE);
            interiorPaint.setStyle(Paint.Style.FILL);
            interiorPaint.setAntiAlias(false);
            interiorPaint.setAlpha(255);

            exteriorPaint = new Paint();
            exteriorPaint.setTextSize(textSize);
            exteriorPaint.setColor(Color.BLACK);
            exteriorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            exteriorPaint.setStrokeWidth(textSize / 8);
            exteriorPaint.setAntiAlias(false);
            exteriorPaint.setAlpha(255);
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in) throws SSJFatalException
    {
        //only draw first frame per call, since drawing multiple frames doesn't make sense without delay
        draw(stream_in[0].ptrB(), ((ImageStream)stream_in[0]).format);
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[]) throws SSJFatalException
    {
        surfaceViewInner = null;
        iaRgbData = null;
        surfaceHolder = null;
        bitmap.recycle();
        bitmap = null;
    }

    /**
     * @param data byte[]
     */
    private void draw(final byte[] data, int format)
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

                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();

					// Rotate canvas around the center of the image.
                    canvas.rotate(options.orientation.get(), canvasWidth >> 1, canvasHeight >> 1);

                    //decode color format
                    decodeColor(data, bitmapWidth, bitmapHeight, format);

                    //fill bitmap with picture
                    bitmap.setPixels(iaRgbData, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

                    if (options.scale.get())
                    {
                        int offset = (canvasHeight - canvasWidth) / 2;

                        Rect dest = new Rect(-offset, offset, canvasWidth + offset, canvasHeight - offset);

                        // scale picture to surface size
                        canvas.drawBitmap(bitmap, null, dest, null);
                    }
                    else
                    {
                        //center picture on canvas
                        canvas.drawBitmap(bitmap,
                                          canvasWidth - ((bitmapWidth + canvasWidth) >> 1),
                                          canvasHeight - ((bitmapHeight + canvasHeight) >> 1),
                                          null);
                    }

                    if (options.showBestMatch.get())
                    {
                        // Draw label of the best match.
                        canvas.rotate(-1 * options.orientation.get(), canvasWidth / 2, canvasHeight / 2);
                        canvas.drawText(bestMatch, 25, 50, exteriorPaint);
                        canvas.drawText(bestMatch, 25, 50, interiorPaint);
                    }
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            //always try to unlock a locked canvas to keep the surface in a consistent state
            if (canvas != null)
            {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * @param data byte[]
     */
    private void decodeColor(final byte[] data, int width, int height, int format)
    {
        //@todo implement missing conversions
        switch (format)
        {
            case ImageFormat.YV12:
            {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
            case ImageFormat.YUV_420_888: //YV12_PACKED_SEMI
            {
                CameraUtil.decodeYV12PackedSemi(iaRgbData, data, width, height);
                break;
            }
            case ImageFormat.NV21:
            {
                CameraUtil.convertNV21ToARGBInt(iaRgbData, data, width, height);
                break;
            }
            case ImageFormat.FLEX_RGB_888:
            {
                CameraUtil.convertRGBToARGBInt(iaRgbData, data, width, height);
                break;
            }
            default:
            {
                Log.e("Wrong color format");
                throw new RuntimeException();
            }
        }
    }

    @Override
    public void notify(Event event)
    {
        if(event.type == Cons.Type.STRING)
            bestMatch = event.ptrStr();
        else
            Log.w("unsupported event format (" + event.type.toString() + "). Expecting STRING events.");
    }
}