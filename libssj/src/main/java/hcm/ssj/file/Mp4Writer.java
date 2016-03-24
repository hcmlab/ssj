/*
 * Mp4Writer.java
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
package hcm.ssj.file;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;

/**
 * Parent class to create mp4-files.<br>
 * Created by Frank Gaibler on 18.02.2016.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class Mp4Writer extends Consumer
{
    private static final int SENDEND_TIMEOUT = 2000;
    private static final int SENDEND_SLEEP = 100;

    /**
     * All options for the mp4 writer
     */
    public class Options
    {
        public File file = null;
    }

    //encoder
    protected double dFrameRate;
    protected MediaCodec mediaCodec;
    //muxer
    protected MediaMuxer mediaMuxer;
    protected int iTrackIndex;
    protected boolean bMuxerStarted;
    protected MediaCodec.BufferInfo bufferInfo;
    //
    protected byte[] byaShuffle;
    protected long iFrameIndex;
    protected final static int TIMEOUT_USEC = 10000;

    /**
     * @param inputBuf  ByteBuffer
     * @param frameData byte[]
     */
    protected abstract void fillBuffer(ByteBuffer inputBuf, byte[] frameData);

    /**
     * @param stream_in Stream[]
     */
    @Override
    public void flush(Stream stream_in[])
    {
        releaseEncoder();
        byaShuffle = null;
        bufferInfo = null;
    }

    /**
     * Configures the encoder
     *
     * @param mediaFormat MediaFormat
     * @param mimeType    String
     * @param filePath    String
     */
    protected final void prepareEncoder(MediaFormat mediaFormat, String mimeType, String filePath)
    {
        //create and start encoder
        try
        {
            mediaCodec = MediaCodec.createEncoderByType(mimeType);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException ex)
        {
            Log.e("MediaCodec creation failed: " + ex.getMessage());
            throw new RuntimeException("MediaCodec creation failed", ex);
        }
        //create muxer
        try
        {
            mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ex)
        {
            Log.e("MediaMuxer creation failed: " + ex.getMessage());
            throw new RuntimeException("MediaMuxer creation failed", ex);
        }
        iTrackIndex = -1;
        bMuxerStarted = false;
    }

    /**
     * Releases encoder resources
     */
    protected final void releaseEncoder()
    {
        if (mediaCodec != null)
        {
            sendEnd();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mediaMuxer != null)
        {
            if (bMuxerStarted)
            {
                mediaMuxer.stop();
            }
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }

    /**
     * @param frameData byte[]
     */
    protected final void encode(byte[] frameData)
    {
        ByteBuffer[] encoderInputBuffers = mediaCodec.getInputBuffers();
        int inputBufIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufIndex >= 0)
        {
            long ptsUsec = computePresentationTime(iFrameIndex);
            ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];
            //the buffer should be sized to hold one full frame
            if (inputBuf.capacity() < frameData.length)
            {
                Log.e("Buffer capacity too small: " + inputBuf.capacity() + "\tdata: " + frameData.length);
            }
            else
            {
                inputBuf.clear();
                fillBuffer(inputBuf, frameData);
                mediaCodec.queueInputBuffer(inputBufIndex, 0, frameData.length, ptsUsec, 0);
                iFrameIndex++;
            }
        } else
        {
            //either all in use, time out during initial setup
            Log.w("Input buffer not available: " + iFrameIndex);
        }
    }

    /**
     * @param last boolean
     */
    protected final synchronized void save(boolean last)
    {
        //save data until none is available
        while (true)
        {
            ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                //no output available yet
                if (last)
                {
                    //try again until buffer sends the end event flag
                    try
                    {
                        //small timeout to ease CPU usage
                        wait(0, 10000);
                    } catch (InterruptedException ex)
                    {
                        Log.e(ex.getMessage());
                        ex.printStackTrace();
                    }
                } else
                {
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
            {
                //not expected for an encoder
                Log.d("Encoder output buffers changed: " + iFrameIndex);
                break;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                //should happen before receiving buffers and should only happen once
                if (bMuxerStarted)
                {
                    Log.e("Format changed twice");
                    throw new RuntimeException("Format changed twice");
                }
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                Log.d("Encoder output format changed: " + newFormat);
                //start muxer
                iTrackIndex = mediaMuxer.addTrack(newFormat);
                mediaMuxer.start();
                bMuxerStarted = true;
            } else if (encoderStatus < 0)
            {
                Log.e("Unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                break;
            }
            else if (encoderStatus >= 0)
            {
                //get data from encoder and send it to muxer
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null)
                {
                    Log.e("EncoderOutputBuffer " + encoderStatus + " was null" + ": " + iFrameIndex);
                    throw new RuntimeException("EncoderOutputBuffer " + encoderStatus + " was null" + ": " + iFrameIndex);
                }
                encodedData.position(bufferInfo.offset);
                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                mediaMuxer.writeSampleData(iTrackIndex, encodedData, bufferInfo);
                mediaCodec.releaseOutputBuffer(encoderStatus, false);
                //check for end of stream
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                {
                    break;
                }
            }
        }
    }

    /**
     * Send end of stream to encoder
     */
    private void sendEnd()
    {
        int inputBufIndex = -1;
        double time = _frame.getTime();

        while(inputBufIndex < 0)
        {
            inputBufIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);

            if (_frame.getTime() > time + SENDEND_TIMEOUT)
                break;

            try{
                Thread.sleep(SENDEND_SLEEP);
            } catch (InterruptedException e) {}
        }

        if (inputBufIndex >= 0)
        {
            long ptsUsec = computePresentationTime(iFrameIndex);
            //send an empty frame with the end-of-stream flag set
            mediaCodec.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            //save every frame still unprocessed
            save(true);
        } else
        {
            //either all in use, time out during initial setup
            Log.w("Input buffer not available on last frame: " + iFrameIndex);
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds
     */
    private long computePresentationTime(long frameIndex)
    {
        return (long) (132L + frameIndex * 1000000L / dFrameRate);
    }
}
