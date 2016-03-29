/*
 * CameraWriter.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.nio.ByteBuffer;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.Mp4Writer;

/**
 * Camera writer for SSJ to create mp4-video-files.<br>
 * Created by Frank Gaibler on 21.01.2016.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CameraWriter extends Mp4Writer
{
    /**
     * All options for the camera writer
     */
    public class Options extends Mp4Writer.Options
    {
        public int width = 640;                 //should be the same as in camera
        public int height = 480;                //should be the same as in camera
        public String mimeType = "video/avc";   //H.264 Advanced Video Coding
        public int iFrameInterval = 15;
        public int bitRate = 100000;            //Mbps
        public int orientation = 270;           //0, 90, 180, 270 (portrait: 90 back, 270 front)
        public int colorFormat = 0;             //MediaCodecInfo.CodecCapabilities
        public int colorSwitch = ColorSwitch.DEFAULT.value;
    }

    /**
     * Switches color before encoding happens
     */
    public enum ColorSwitch
    {
        DEFAULT(0),
        YV12_PLANAR(1),         //MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        YV12_PACKED_SEMI(2),    //MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
        NV21_UV_SWAPPED(3);

        public final int value;

        /**
         * @param i int
         */
        ColorSwitch(int i)
        {
            value = i;
        }

        /**
         * @param value int
         * @return ColorSwitch
         */
        private static ColorSwitch getColorSwitch(int value)
        {
            if (value == YV12_PLANAR.value)
            {
                return YV12_PLANAR;
            }
            if (value == YV12_PACKED_SEMI.value)
            {
                return YV12_PACKED_SEMI;
            }
            if (value == NV21_UV_SWAPPED.value)
            {
                return NV21_UV_SWAPPED;
            }
            return DEFAULT;
        }
    }

    public Options options = new Options();
    //
    byte[] byaColorChange;
    private int planeSize;
    private int planeSizeCx;
    private ColorSwitch colorSwitch;

    /**
     *
     */
    public CameraWriter()
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
            Log.e("Stream count not supported");
            return;
        }
        if (stream_in[0].type != Cons.Type.BYTE)
        {
            Log.e("Stream type not supported");
            return;
        }
        dFrameRate = stream_in[0].sr;
        prepareEncoder(options.width, options.height, options.bitRate);
        bufferInfo = new MediaCodec.BufferInfo();
        int reqBuffSize = options.width * options.height;
        reqBuffSize += reqBuffSize >> 1;
        byaShuffle = new byte[reqBuffSize];
        lFrameIndex = 0;
        colorSwitch = ColorSwitch.getColorSwitch(options.colorSwitch);
        switch (colorSwitch)
        {
            case YV12_PACKED_SEMI:
            {
                planeSize = options.width * options.height;
                planeSizeCx = planeSize >> 2;
                byaColorChange = new byte[planeSizeCx * 2];
                break;
            }
            case NV21_UV_SWAPPED:
            {
                planeSize = options.width * options.height;
                planeSizeCx = planeSize >> 1;
                byaColorChange = new byte[planeSizeCx];
                break;
            }
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in)
    {
        byte[] in = stream_in[0].ptrB();
        for (int i = 0; i < in.length; i += byaShuffle.length)
        {
            System.arraycopy(in, i, byaShuffle, 0, byaShuffle.length);
            encode(byaShuffle);
            save(false);
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[])
    {
        super.flush(stream_in);
        byaColorChange = null;
    }

    /**
     * Configures the encoder
     */
    private void prepareEncoder(int width, int height, int bitRate)
    {
        MediaCodecInfo mediaCodecInfo = CameraUtil.selectCodec(options.mimeType);
        if (mediaCodecInfo == null)
        {
            Log.e("Unable to find an appropriate codec for " + options.mimeType);
            return;
        }
        //set format properties
        MediaFormat videoFormat = MediaFormat.createVideoFormat(options.mimeType, width, height);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, options.colorFormat <= 0
                ? CameraUtil.selectColorFormat(mediaCodecInfo, options.mimeType) : options.colorFormat);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setFloat(MediaFormat.KEY_FRAME_RATE, (float) dFrameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, options.iFrameInterval);
        //prepare encoder
        super.prepareEncoder(videoFormat, options.mimeType, options.file.getPath());
        //set video orientation
        if (options.orientation % 90 == 0 || options.orientation % 90 == 90)
        {
            mediaMuxer.setOrientationHint(options.orientation);
        } else
        {
            Log.e("Orientation is not valid: " + options.orientation);
        }
    }

    /**
     * @param inputBuf  ByteBuffer
     * @param frameData byte[]
     */
    protected final void fillBuffer(ByteBuffer inputBuf, byte[] frameData)
    {
        if (colorSwitch == ColorSwitch.DEFAULT)
        {
            inputBuf.put(frameData);
        } else
        {
            //switch colors
            int quarterPlane = frameData.length / 6; //quarterPlane = width * height / 4
            inputBuf.put(frameData, 0, quarterPlane * 4);
            switch (colorSwitch)
            {
                case YV12_PLANAR:
                {
                    //swap YV12 to YUV420Planes
                    inputBuf.put(frameData, quarterPlane * 5, quarterPlane);
                    inputBuf.put(frameData, quarterPlane * 4, quarterPlane);
                    break;
                }
                case YV12_PACKED_SEMI:
                {
                    //swap YV12 to YUV420PackedSemiPlanar
                    inputBuf.put(swapRestYV12toYUV420PackedSemiPlanar(frameData), 0, quarterPlane + quarterPlane);
                    break;
                }
                case NV21_UV_SWAPPED:
                {
                    //swap NV21 U and V
                    inputBuf.put(swapNV21_UV(frameData), 0, quarterPlane + quarterPlane);
                    break;
                }
                default:
                {
                    Log.e("Wrong color switch");
                    throw new RuntimeException();
                }
            }
        }
    }

    /**
     * @param YV12 byte[]
     * @return byte[]
     */
    private byte[] swapRestYV12toYUV420PackedSemiPlanar(byte[] YV12)
    {
        for (int i = 0; i < planeSizeCx; i++)
        {
            byaColorChange[i * 2] = YV12[planeSize + i + planeSizeCx];
            byaColorChange[i * 2 + 1] = YV12[planeSize + i];
        }
        return byaColorChange;
    }

    /**
     * Swaps UVUV to VUVU and vice versa
     *
     * @param NV21 byte[]
     * @return byte[]
     */
    private byte[] swapNV21_UV(byte[] NV21)
    {
        for (int i = 0; i < planeSizeCx; i += 2)
        {
            byaColorChange[i] = NV21[planeSize + i + 1];
            byaColorChange[i + 1] = NV21[planeSize + i];
        }
        return byaColorChange;
    }
}
