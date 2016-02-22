/*
 * AudioProvider.java
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

package hcm.ssj.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.stream.Stream;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class AudioProvider extends SensorProvider
{
    public class Options
    {
        public int sampleRate = 8000;
        public int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        public int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        public boolean scale = false;
    }
    public Options options = new Options();

    protected AudioRecord _recorder;

    byte[] _data = null;

    public AudioProvider()
    {

        _name = "SSJ_sensor_Microphone_Audio";
    }

    @Override
    public void enter(Stream stream_out)
    {
        //setup android audio middleware
        _recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, options.sampleRate, options.channelConfig, options.audioFormat, stream_out.tot*10);

        int state = _recorder.getState();
        if(state != 1)
            Log.w(_name, "unexpected AudioRecord state = " + state);

        if(options.scale)
        {
            if(options.audioFormat != AudioFormat.ENCODING_PCM_8BIT && options.audioFormat != AudioFormat.ENCODING_PCM_16BIT)
                Log.e(_name, "unsupported audio format for normalization");

            int numBytes = Microphone.audioFormatSampleBytes(options.audioFormat);
            _data = new byte[stream_out.num * stream_out.dim * numBytes];
        }

        //startRecording has to be called as close to the first read as possible.
        _recorder.startRecording();
        Log.i(_name, "Audio capturing started");
    }

    @Override
    protected void process(Stream stream_out)
    {
        if(!options.scale)
        {
            //read data
            // this is blocking and thus defines the update rate
            switch (options.audioFormat)
            {
                case AudioFormat.ENCODING_PCM_8BIT:
                    _recorder.read(stream_out.ptrB(), 0, stream_out.num * stream_out.dim);
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                case AudioFormat.ENCODING_DEFAULT:
                    _recorder.read(stream_out.ptrS(), 0, stream_out.num * stream_out.dim);
                    break;
                default:
                    Log.w(_name, "unsupported audio format");
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
                switch (options.audioFormat)
                {
                    case AudioFormat.ENCODING_PCM_8BIT:
                        outf[j++] = _data[i++] / 128.0f;
                        break;
                    case AudioFormat.ENCODING_PCM_16BIT:
                    case AudioFormat.ENCODING_DEFAULT:
                        outf[j++] = (short) ((_data[i+1] & 0xFF) << 8 | (_data[i+0] & 0xFF)) / 32768.0f;
                        i += 2;
                        break;
                    default:
                        Log.w(_name, "unsupported audio format");
                }
            }
        }
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
        switch(options.channelConfig)
        {
            case AudioFormat.CHANNEL_IN_MONO:
                return 1;

            case AudioFormat.CHANNEL_IN_STEREO:
                return 2;
        }

        return 0;
    }

    @Override
    public double getSampleRate()
    {
        return options.sampleRate;
    }

    @Override
    public int getSampleNumber()
    {
        int minBufSize = AudioRecord.getMinBufferSize(options.sampleRate, options.channelConfig, options.audioFormat);
        int bytesPerSample = Microphone.audioFormatSampleBytes(options.audioFormat);
        int dim = getSampleDimension();

        return minBufSize / (bytesPerSample * dim);
    }

    @Override
    public int getSampleBytes()
    {
       if(options.scale)
            return 4;
        else
            return Microphone.audioFormatSampleBytes(options.audioFormat);
    }

    @Override
    public Cons.Type getSampleType()
    {
        if(options.scale)
            return Cons.Type.FLOAT;
        else
            return Microphone.audioFormatSampleType(options.audioFormat);
    }

    @Override
    public void defineOutputClasses(Stream stream_out)
    {
        stream_out.dataclass = new String[1];
        stream_out.dataclass[0] = "Audio";
    }
}
