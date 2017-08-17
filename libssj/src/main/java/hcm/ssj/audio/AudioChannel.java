/*
 * AudioChannel.java
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

package hcm.ssj.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class AudioChannel extends SensorChannel
{
    public class Options extends OptionList
    {
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 8000, Integer.class, "");
        public final Option<Cons.ChannelFormat> channelConfig = new Option<>("channelConfig", Cons.ChannelFormat.CHANNEL_IN_MONO, Cons.ChannelFormat.class, "");
        public final Option<Cons.AudioFormat> audioFormat = new Option<>("audioFormat", Cons.AudioFormat.ENCODING_PCM_16BIT, Cons.AudioFormat.class, "");
        public final Option<Boolean> scale = new Option<>("scale", true, Boolean.class, "");
        public final Option<Double> chunk = new Option<>("chunk", 0.1, Double.class, "how many samples to read at once (in seconds)");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    protected AudioRecord _recorder;

    byte[] _data = null;

    public AudioChannel()
    {
        _name = "Microphone_Audio";
    }

    @Override
    public void enter(Stream stream_out)
    {
        //setup android audio middleware
        _recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, options.sampleRate.get(), options.channelConfig.get().val, options.audioFormat.get().val, stream_out.tot*10);

        int state = _recorder.getState();
        if(state != 1)
            Log.w("unexpected AudioRecord state = " + state);

        if(options.scale.get())
        {
            if(options.audioFormat.get() != Cons.AudioFormat.ENCODING_PCM_8BIT && options.audioFormat.get() != Cons.AudioFormat.ENCODING_PCM_16BIT)
                Log.e("unsupported audio format for normalization");

            int numBytes = Microphone.audioFormatSampleBytes(options.audioFormat.get().val);
            _data = new byte[stream_out.num * stream_out.dim * numBytes];
        }

        //startRecording has to be called as close to the first read as possible.
        _recorder.startRecording();
        Log.i("Audio capturing started");
    }

    @Override
    protected boolean process(Stream stream_out)
    {
        if(!options.scale.get())
        {
            //read data
            // this is blocking and thus defines the update rate
            switch (options.audioFormat.get())
            {
                case ENCODING_PCM_8BIT:
                    _recorder.read(stream_out.ptrB(), 0, stream_out.num * stream_out.dim);
                    break;
                case ENCODING_PCM_16BIT:
                case ENCODING_DEFAULT:
                    _recorder.read(stream_out.ptrS(), 0, stream_out.num * stream_out.dim);
                    break;
                default:
                    Log.w("unsupported audio format");
                    return false;
            }
        }
        else
        {
            //read data
            // this is blocking and thus defines the update rate
            _recorder.read(_data, 0, _data.length);

            //normalize it and convert it to floats
            float[] outf = stream_out.ptrF();
            int i = 0, j = 0;
            while (i < _data.length)
            {
                switch (options.audioFormat.get())
                {
                    case ENCODING_PCM_8BIT:
                        outf[j++] = _data[i++] / 128.0f;
                        break;
                    case ENCODING_PCM_16BIT:
                    case ENCODING_DEFAULT:
                        outf[j++] = (short) ((_data[i+1] & 0xFF) << 8 | (_data[i+0] & 0xFF)) / 32768.0f;
                        i += 2;
                        break;
                    default:
                        Log.w("unsupported audio format");
                        return false;
                }
            }
        }

        return true;
    }

    @Override
    public void flush(Stream stream_out)
    {
        _recorder.stop();
        _recorder.release();
    }

    @Override
    public int getSampleDimension()
    {
        switch(options.channelConfig.get())
        {
            case CHANNEL_IN_MONO:
                return 1;

            case CHANNEL_IN_STEREO:
                return 2;
        }

        return 0;
    }

    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    @Override
    public int getSampleNumber()
    {
        int minBufSize = AudioRecord.getMinBufferSize(options.sampleRate.get(), options.channelConfig.get().val, options.audioFormat.get().val);
        int bytesPerSample = Microphone.audioFormatSampleBytes(options.audioFormat.get().val);
        int dim = getSampleDimension();

        double sr = getSampleRate();
        int minSampleNum = (minBufSize / (bytesPerSample * dim));
        double minFrameSize = minSampleNum / sr;

        if(options.chunk.get() < minFrameSize) {
            Log.w("requested chunk size too small, setting it to " + minFrameSize + "s");
            options.chunk.set(minFrameSize);
        }

        return (int)(options.chunk.get() * sr + 0.5);
    }

    @Override
    public int getSampleBytes()
    {
       if(options.scale.get())
            return 4;
        else
            return Microphone.audioFormatSampleBytes(options.audioFormat.get().val);
    }

    @Override
    public Cons.Type getSampleType()
    {
        if(options.scale.get())
            return Cons.Type.FLOAT;
        else
            return Microphone.audioFormatSampleType(options.audioFormat.get().val);
    }

    @Override
    public void describeOutput(Stream stream_out)
    {
        stream_out.desc = new String[1];
        stream_out.desc[0] = "Audio";
    }
}
