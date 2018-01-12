/*
 * CameraWriter.java
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

package hcm.ssj.camera;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
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
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     * All options for the camera writer
     */
    public class Options extends Mp4Writer.Options
    {
        public final Option<Integer> width = new Option<>("width", 640, Integer.class, "should be the same as in camera");
        //arbitrary but popular values
        public final Option<Integer> height = new Option<>("height", 480, Integer.class, "should be the same as in camera");
        public final Option<String> mimeType = new Option<>("mimeType", "video/avc", String.class, "H.264 Advanced Video Coding");
        public final Option<Integer> iFrameInterval = new Option<>("iFrameInterval", 15, Integer.class, "Interval between complete frames");
        public final Option<Integer> bitRate = new Option<>("bitRate", 100000, Integer.class, "Mbps");
        public final Option<Integer> orientation = new Option<>("imageFormat", 270, Integer.class, "0, 90, 180, 270 (portrait: 90 back, 270 front)");
        public final Option<Integer> colorFormat = new Option<>("colorFormat", 0, Integer.class, "MediaCodecInfo.CodecCapabilities");
        public final Option<ColorSwitch> colorSwitch = new Option<>("colorSwitch", ColorSwitch.DEFAULT, ColorSwitch.class, "");

        /**
         *
         */
        private Options()
        {
            super();
            addOptions();
        }
    }

    /**
     * Switches color before encoding happens
     */
    public enum ColorSwitch
    {
        DEFAULT,
        YV12_PLANAR,         //MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        YV12_PACKED_SEMI,    //MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
        NV21_UV_SWAPPED
    }

    public final Options options = new Options();
    //
    byte[] aByColorChange;
    private int planeSize;
    private int planeSizeCx;
    private ColorSwitch colorSwitch;

    /**
     *
     */
    public CameraWriter()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in Stream[]
	 */
    @Override
    public final void enter(Stream[] stream_in) throws SSJFatalException
    {
        if (stream_in.length != 1)
        {
            throw new SSJFatalException("Stream count not supported");
        }
        if(stream_in[0].type != Cons.Type.IMAGE || ((ImageStream)stream_in[0]).format != ImageFormat.NV21)
        {
            throw new SSJFatalException("invalid input, writer only supports NV21 images");
        }

        dFrameRate = stream_in[0].sr;
        initFiles(stream_in[0], options);

        try
        {
            prepareEncoder(options.width.get(), options.height.get(), options.bitRate.get());
        }
        catch (IOException e)
        {
            throw new SSJFatalException("error preparing encoder", e);
        }

        bufferInfo = new MediaCodec.BufferInfo();

        int reqBuffSize = stream_in[0].dim;
        aByShuffle = new byte[reqBuffSize];
        lFrameIndex = 0;
        colorSwitch = options.colorSwitch.get();
        switch (colorSwitch)
        {
            case YV12_PACKED_SEMI:
            {
                planeSize = options.width.get() * options.height.get();
                planeSizeCx = planeSize >> 2;
                aByColorChange = new byte[planeSizeCx * 2];
                break;
            }
            case NV21_UV_SWAPPED:
            {
                planeSize = options.width.get() * options.height.get();
                planeSizeCx = planeSize >> 1;
                aByColorChange = new byte[planeSizeCx];
                break;
            }
        }
    }

    /**
     * @param stream_in Stream[]
	 * @param trigger
     */
    @Override
    protected final void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        byte[] in = stream_in[0].ptrB();
        for (int i = 0; i < in.length; i += aByShuffle.length)
        {
            System.arraycopy(in, i, aByShuffle, 0, aByShuffle.length);

            try
            {
                encode(aByShuffle);
            }
            catch (IOException e)
            {
                throw new SSJFatalException("exception during encoding", e);
            }

            save(false);
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[]) throws SSJFatalException
    {
        super.flush(stream_in);
        aByColorChange = null;
    }

    /**
     * Configures the encoder
     */
    private void prepareEncoder(int width, int height, int bitRate) throws IOException
    {
        MediaCodecInfo mediaCodecInfo = CameraUtil.selectCodec(options.mimeType.get());
        if (mediaCodecInfo == null)
        {
            throw new IOException("Unable to find an appropriate codec for " + options.mimeType.get());
        }
        //set format properties
        MediaFormat videoFormat = MediaFormat.createVideoFormat(options.mimeType.get(), width, height);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, options.colorFormat.get() <= 0
                ? CameraUtil.selectColorFormat(mediaCodecInfo, options.mimeType.get()) : options.colorFormat.get());
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setFloat(MediaFormat.KEY_FRAME_RATE, (float) dFrameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, options.iFrameInterval.get());
        //prepare encoder
        super.prepareEncoder(videoFormat, options.mimeType.get(), file.getPath());
        //set video orientation
        if (options.orientation.get() % 90 == 0 || options.orientation.get() % 90 == 90)
        {
            mediaMuxer.setOrientationHint(options.orientation.get());
        } else
        {
            Log.w("Orientation is not valid: " + options.orientation.get());
        }
    }

    /**
     * @param inputBuf  ByteBuffer
     * @param frameData byte[]
     */
    protected final void fillBuffer(ByteBuffer inputBuf, byte[] frameData) throws IOException
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
                    throw new IOException("Wrong color switch");
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
            aByColorChange[i * 2] = YV12[planeSize + i + planeSizeCx];
            aByColorChange[i * 2 + 1] = YV12[planeSize + i];
        }
        return aByColorChange;
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
            aByColorChange[i] = NV21[planeSize + i + 1];
            aByColorChange[i + 1] = NV21[planeSize + i];
        }
        return aByColorChange;
    }
}
