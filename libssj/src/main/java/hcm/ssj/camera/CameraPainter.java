/*
 * CameraPainter.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
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
    public class Options extends OptionList
    {
        //values should be the same as in camera
        public final Option<Integer> width = new Option<>("width", 640, Integer.class, "width of input picture");
        public final Option<Integer> height = new Option<>("height", 480, Integer.class, "height of input picture");
        public final Option<Integer> orientation = new Option<>("orientation", 90, Integer.class, "orientation of input picture");
        public final Option<Boolean> scale = new Option<>("scale", false, Boolean.class, "scale picture to match surface size");
        public final Option<ColorFormat> colorFormat = new Option<>("colorFormat", ColorFormat.NV21_DEFAULT, ColorFormat.class, "change color format");
        public final Option<SurfaceView> surfaceView = new Option<>("surfaceView", null, SurfaceView.class, "the view on which the painter is drawn");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    /**
     * Switches color before encoding happens
     */
    public enum ColorFormat
    {
        DEFAULT, YV12_PLANAR, YV12_PACKED_SEMI, NV21_DEFAULT, NV21_UV_SWAPPED
    }

    public final Options options = new Options();
    //buffers
    private byte[] byaShuffle;
    private int[] iaRgbData;
    private Bitmap bitmap;
    //
    private SurfaceView surfaceViewInner = null;
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
    protected void init(Stream[] stream_in)
    {
        super.init(stream_in);
        if (options.surfaceView.get() == null)
        {
            Log.w("surfaceView isn't set");
            //create system dialog to show painter
            final Context context = SSJApplication.getAppContext();
            Handler handler = new Handler(Looper.getMainLooper());
            Thread thread = new Thread()
            {
                /**
                 *
                 */
                @Override
                public void run()
                {
                    surfaceViewInner = new SurfaceView(context);
                    synchronized (CameraPainter.this)
                    {
                        try
                        {
                            CameraPainter.this.notify();
                        } catch (IllegalMonitorStateException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(CameraPainter.class.getSimpleName());
                    builder.setView(surfaceViewInner);
                    //
                    AlertDialog alertDialog = builder.create();
                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alertDialog.show();
                }
            };
            handler.post(thread);
        } else
        {
            surfaceViewInner = options.surfaceView.get();
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void enter(Stream[] stream_in)
    {
        if (stream_in.length != 1)
        {
            Log.e("Stream count not supported");
            return;
        }
        if (stream_in[0].type != Cons.Type.BYTE)
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
        int reqBuffSize = options.width.get() * options.height.get();
        reqBuffSize += reqBuffSize >> 1;
        byaShuffle = new byte[reqBuffSize];
        surfaceHolder = surfaceViewInner.getHolder();
        iaRgbData = new int[options.width.get() * options.height.get()];
        //set bitmap
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        bitmap = Bitmap.createBitmap(options.width.get(), options.height.get(), conf);
        //get colorFormat
        colorFormat = options.colorFormat.get();
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
        surfaceViewInner = null;
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
                if (canvas != null)
                {
                    int canvasWidth = canvas.getWidth();
                    int canvasHeight = canvas.getHeight();
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();
                    //rotate canvas
                    canvas.rotate(options.orientation.get(), canvasWidth >> 1, canvasHeight >> 1);
                    //decode color format
                    decodeColor(data, bitmapWidth, bitmapHeight);
                    //fill bitmap with picture
                    bitmap.setPixels(iaRgbData, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
                    if (options.scale.get())
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
        //@todo implement missing conversions
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
                decodeYV12PackedSemi(iaRgbData, data, width, height);
                break;
            }
            case NV21_DEFAULT:
            {
                decodeNV21(iaRgbData, data, width, height, false);
                break;
            }
            case NV21_UV_SWAPPED:
            {
//                //perfect conversion, but uses "new"
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, width, height, null);
//                //data is the byte array of your YUV image, that you want to convert
//                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
//                byte[] bytes = out.toByteArray();
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                decodeNV21(iaRgbData, data, width, height, true);
                break;
            }
            default:
            {
                Log.e("Wrong color format");
                throw new RuntimeException();
            }
        }
    }

    /**
     * Decodes YUV frame to a RGB buffer
     *
     * @param rgba     int[]
     * @param yuv420sp byte[]
     * @param width    width
     * @param height   height
     */
    private void decodeYV12PackedSemi(int[] rgba, byte[] yuv420sp, int width, int height)
    {
        //@todo untested
        final int frameSize = width * height;
        int r, g, b, y1192, y, i, uvp, u, v;
        for (int j = 0, yp = 0; j < height; j++)
        {
            uvp = frameSize + (j >> 1) * width;
            u = 0;
            v = 0;
            for (i = 0; i < width; i++, yp++)
            {
                y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0)
                {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);
                //
                r = Math.max(0, Math.min(r, 262143));
                g = Math.max(0, Math.min(g, 262143));
                b = Math.max(0, Math.min(b, 262143));
                // rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                // 0xff00) | ((b >> 10) & 0xff);
                // rgba, divide 2^10 ( >> 10)
                rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
                        | ((b >> 2) | 0xff00);
            }
        }
    }

    /**
     * Decodes YUV frame to a RGB buffer
     *
     * @param out    int[]
     * @param yuv    byte[]
     * @param width  int
     * @param height int
     * @param swap   boolean
     */
    private void decodeNV21(int[] out, byte[] yuv, int width, int height, boolean swap)
    {
        //@todo correct colors
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
                    Cb = yuv[cOff + (swap ? 0 : 1)];
                    if (Cb < 0)
                    {
                        Cb += 127;
                    } else
                    {
                        Cb -= 128;
                    }
                    Cr = yuv[cOff + (swap ? 1 : 0)];
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