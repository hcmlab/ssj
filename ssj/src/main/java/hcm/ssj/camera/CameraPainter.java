/*
 * CameraPainter.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.stream.Stream;

/**
 * Camera painter for SSJ.<br>
 * Created by Frank Gaibler on 21.01.2016.
 */
public class CameraPainter extends Consumer
{
    /**
     * All options for the camera painter
     */
    public class Options
    {
        //values should be the same as in camera
        public int width = 640;
        public int height = 480;
        public int orientation = 90;
        public boolean scale = false;
        public int colorFormat = ColorFormat.NV21_DEFAULT.value;
        public SurfaceView surfaceView = null;
    }

    /**
     * Switches color before encoding happens
     */
    public enum ColorFormat
    {
        DEFAULT(0),
        YV12_PLANAR(1),
        YV12_PACKED_SEMI(2),
        NV21_DEFAULT(3),
        NV21_UV_SWAPPED(4);

        public final int value;

        /**
         * @param i int
         */
        ColorFormat(int i)
        {
            value = i;
        }

        /**
         * @param value int
         * @return ColorFormat
         */
        private static ColorFormat getColorFormat(int value)
        {
            if (value == YV12_PLANAR.value)
            {
                return YV12_PLANAR;
            }
            if (value == YV12_PACKED_SEMI.value)
            {
                return YV12_PACKED_SEMI;
            }
            if (value == NV21_DEFAULT.value)
            {
                return NV21_DEFAULT;
            }
            if (value == NV21_UV_SWAPPED.value)
            {
                return NV21_UV_SWAPPED;
            }
            return DEFAULT;
        }
    }

    public Options options = new Options();
    //buffers
    private byte[] byaShuffle;
    private int[] iaRgbData;
    private Bitmap bitmap;
    //
    private SurfaceHolder surfaceHolder;
    //
    private ColorFormat colorFormat;

    /**
     *
     */
    public CameraPainter()
    {
        _name = "SSJ_consumer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void enter(Stream[] stream_in)
    {
        if (stream_in.length != 1)
        {
            Log.e(_name, "Stream count not supported");
            return;
        }
        if (stream_in[0].type != Cons.Type.BYTE)
        {
            Log.e(_name, "Stream type not supported");
            return;
        }
        int reqBuffSize = options.width * options.height;
        reqBuffSize += reqBuffSize >> 1;
        byaShuffle = new byte[reqBuffSize];
        surfaceHolder = options.surfaceView.getHolder();
        iaRgbData = new int[options.width * options.height];
        //set bitmap
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        bitmap = Bitmap.createBitmap(options.width, options.height, conf);
        //get colorFormat
        colorFormat = ColorFormat.getColorFormat(options.colorFormat);
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in)
    {
        byte[] in = stream_in[0].ptrB();
        //only draw first frame per call, since drawing multiple frames doesn't make sense without delay
        if (in.length >= byaShuffle.length)
        {
            System.arraycopy(in, 0, byaShuffle, 0, byaShuffle.length);
            draw(byaShuffle);
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[])
    {
        iaRgbData = null;
        byaShuffle = null;
        surfaceHolder = null;
        bitmap.recycle();
        bitmap = null;
    }

    /**
     * @param data byte[]
     */
    private void draw(final byte[] data)
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
                canvas = surfaceHolder.lockCanvas(null);
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();
                //rotate canvas
                canvas.rotate(options.orientation, canvasWidth >> 1, canvasHeight >> 1);
                //decode color format
                decodeColor(data, bitmapWidth, bitmapHeight);
                //fill bitmap with picture
                bitmap.setPixels(iaRgbData, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
                if (options.scale)
                {
                    //scale picture to surface size
                    canvas.drawBitmap(bitmap, null, new Rect(0, 0, canvasWidth, canvasHeight), null);
                } else
                {
                    //center picture on canvas
                    canvas.drawBitmap(bitmap,
                            canvasWidth - ((bitmapWidth + canvasWidth) >> 1),
                            canvasHeight - ((bitmapHeight + canvasHeight) >> 1),
                            null);
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
    private void decodeColor(final byte[] data, int width, int height)
    {
        //@todo allow for different formats. Current only works for NV21 back camera
        switch (colorFormat)
        {
            case DEFAULT:
            {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
            case YV12_PLANAR:
            {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
            case YV12_PACKED_SEMI:
            {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
            case NV21_DEFAULT:
            {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
            case NV21_UV_SWAPPED:
            {
                NV21_UV_SWAPPED(iaRgbData, data, width, height);
                break;
            }
            default:
            {
                Log.e(_name, "Wrong color format");
                throw new RuntimeException();
            }
        }
    }

    /**
     * Decodes YUV frame to a RGB buffer
     *
     * @param out    the outgoing array of RGB bytes
     * @param yuv    the incoming frame bytes
     * @param width  of source frame
     * @param height of source frame
     */
    private void NV21_UV_SWAPPED(int[] out, byte[] yuv, int width, int height)
    {
        int sz = width * height;
        int i, j;
        int Y, Cr = 0, Cb = 0;
        for (j = 0; j < height; j++)
        {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++)
            {
                Y = yuv[pixPtr];
                if (Y < 0)
                    Y += 255;
                if ((i & 0x1) != 1)
                {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = yuv[cOff];
                    if (Cb < 0)
                    {
                        Cb += 127;
                    } else
                    {
                        Cb -= 128;
                    }
                    Cr = yuv[cOff + 1];
                    if (Cr < 0)
                    {
                        Cr += 127;
                    } else
                    {
                        Cr -= 128;
                    }
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0)
                {
                    R = 0;
                } else if (R > 255)
                {
                    R = 255;
                }
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0)
                {
                    G = 0;
                } else if (G > 255)
                {
                    G = 255;
                }
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0)
                {
                    B = 0;
                } else if (B > 255)
                {
                    B = 255;
                }
                out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
            }
        }
    }
}