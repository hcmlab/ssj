/*
 * AudioWriter.java
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
package hcm.ssj.audio;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.Mp4Writer;

/**
 * Audio writer for SSJ to create mp4-audio-files.<br>
 * Created by Frank Gaibler on 21.01.2016.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioWriter extends Mp4Writer
{
    /**
     * All options for the audio writer
     */
    public class Options extends Mp4Writer.Options
    {
        public final Option<String> mimeType = new Option<>("mimeType", "audio/mp4a-latm", String.class, "");
        public final Option<Integer> audioFormat = new Option<>("audioFormat", AudioFormat.ENCODING_DEFAULT, Integer.class, "");

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
     *
     */
    private enum DataFormat
    {
        BYTE(Microphone.audioFormatSampleBytes(AudioFormat.ENCODING_PCM_8BIT)),
        SHORT(Microphone.audioFormatSampleBytes(AudioFormat.ENCODING_PCM_16BIT)),
        FLOAT_8(BYTE.size),
        FLOAT_16(SHORT.size);

        private int size;

        /**
         * @param i int
         */
        DataFormat(int i)
        {
            size = i;
        }
    }

    public final Options options = new Options();
    //
    private int iSampleRate;
    private int iSampleNumber;
    private int iSampleDimension;
    //
    private DataFormat dataFormat = null;

    /**
     *
     */
    public AudioWriter()
    {
        _name = "SSJ_consumer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void init(Stream[] stream_in)
    {
        initFiles(options);
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
        switch (stream_in[0].type)
        {
            case BYTE:
            {
                dataFormat = DataFormat.BYTE;
                break;
            }
            case SHORT:
            {
                dataFormat = DataFormat.SHORT;
                break;
            }
            case FLOAT:
            {
                if (options.audioFormat.getValue() == AudioFormat.ENCODING_DEFAULT || options.audioFormat.getValue() == AudioFormat.ENCODING_PCM_16BIT)
                {
                    dataFormat = DataFormat.FLOAT_16;
                } else if (options.audioFormat.getValue() == AudioFormat.ENCODING_PCM_8BIT)
                {
                    dataFormat = DataFormat.FLOAT_8;
                } else
                {
                    Log.e("Audio format not supported");
                }
                break;
            }
            default:
            {
                Log.e("Stream type not supported");
                return;
            }
        }
        Log.d("Format: " + dataFormat.toString());
        iSampleRate = (int) stream_in[0].sr;
        iSampleDimension = stream_in[0].dim;
        iSampleNumber = iSampleRate * dataFormat.size * iSampleDimension;
        //recalculate frame rate
        dFrameRate = stream_in[0].sr / stream_in[0].num;
        aByShuffle = new byte[(int) (iSampleNumber / dFrameRate + 0.5)];
        lFrameIndex = 0;
        prepareEncoder();
        bufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in)
    {
        switch (dataFormat)
        {
            case BYTE:
            {
                byte[] in = stream_in[0].ptrB();
                for (int i = 0; i < in.length; i += aByShuffle.length)
                {
                    System.arraycopy(in, i, aByShuffle, 0, aByShuffle.length);
                    encode(aByShuffle);
                    save(false);
                }
                break;
            }
            case SHORT:
            {
                short[] in = stream_in[0].ptrS();
                for (int i = 0; i < in.length; i += aByShuffle.length)
                {
                    ByteBuffer.wrap(aByShuffle).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(in, i / 2, aByShuffle.length / 2);
                    encode(aByShuffle);
                    save(false);
                }
                break;
            }
            case FLOAT_8:
                float[] in = stream_in[0].ptrF();
                for (int i = 0; i < in.length; )
                {
                    for (int j = 0; j < aByShuffle.length; j++, i += aByShuffle.length)
                    {
                        aByShuffle[j] = (byte) (in[i] * 128);
                    }
                    encode(aByShuffle);
                    save(false);
                }
                break;
            case FLOAT_16:
            {
                float[] in16 = stream_in[0].ptrF();
                for (int i = 0; i < in16.length; )
                {
                    for (int j = 0; j < aByShuffle.length; i++, j += 2)
                    {
                        short value = (short) (in16[i] * 32768);
                        aByShuffle[j] = (byte) (value & 0xff);
                        aByShuffle[j + 1] = (byte) ((value >> 8) & 0xff);
                    }
                    encode(aByShuffle);
                    save(false);
                }
                break;
            }
            default:
            {
                Log.e("Data format not supported");
                break;
            }
        }
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[])
    {
        super.flush(stream_in);
        dataFormat = null;
    }

    /**
     * Configures the encoder
     */
    private void prepareEncoder()
    {
        //set format properties
        MediaFormat audioFormat = new MediaFormat();
        audioFormat.setString(MediaFormat.KEY_MIME, options.mimeType.getValue());
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, iSampleRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, iSampleDimension);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, iSampleNumber);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, aByShuffle.length);
        //prepare encoder
        super.prepareEncoder(audioFormat, options.mimeType.getValue(), file.getPath());
    }

    /**
     * @param inputBuf  ByteBuffer
     * @param frameData byte[]
     */
    protected final void fillBuffer(ByteBuffer inputBuf, byte[] frameData)
    {
        inputBuf.put(frameData);
    }
}
