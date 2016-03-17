/*
 * Pitch.java
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

import be.tarsos.dsp.pitch.AMDF;
import be.tarsos.dsp.pitch.DynamicWavelet;
import be.tarsos.dsp.pitch.FFTPitch;
import be.tarsos.dsp.pitch.FastYin;
import be.tarsos.dsp.pitch.McLeodPitchMethod;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.Yin;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.stream.Stream;

/**
 * Computes the pitch (F0) of the input signal
 * - uses TarsosDSP
 * Created by Johnny on 05.03.2015.
 */
public class Pitch extends Transformer {

    public final static int DETECTOR_MPM = 0;
    public final static int DYNAMIC_WAVELET = 1;
    public final static int FFT_YIN = 2;
    public final static int AMDF = 3;
    public final static int FFT_PITCH = 4;
    public final static int YIN = 5;

    public class Options
    {
        public int detector = YIN;

        public boolean computePitch = true;
        public boolean computePitchEnvelope = false; //if pitch is invalid, provide old value again
        public boolean computeVoicedProb = true;
        public boolean computePitchedState = false;

        public float minPitch = 52.0f;
        public float maxPitch = 620.0f;
    }
    public Options options = new Options();

    protected PitchDetector _detector;

    protected float _lastPitch = 0;

    public Pitch()
    {
        _name = "SSJ_transformer_Pitch";
    }

    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        Stream audio = null;
        for(Stream s : stream_in) {
            if (s.findDataClass("Audio") >= 0)
                audio = s;
        }
        if(audio == null) {
            Log.w("invalid input stream");
            return;
        }

        switch(options.detector)
        {
            case DETECTOR_MPM:
                _detector = new McLeodPitchMethod((float)audio.sr, audio.num * audio.dim);
                break;
            case DYNAMIC_WAVELET:
                _detector = new DynamicWavelet((float)audio.sr, audio.num * audio.dim);
                break;
            case FFT_YIN:
                _detector = new FastYin((float)audio.sr, audio.num * audio.dim);
                break;
            case AMDF:
                _detector = new AMDF((float)audio.sr, audio.num * audio.dim);
                break;
            case FFT_PITCH:
                _detector = new FFTPitch((int)audio.sr, audio.num * audio.dim);
                break;
            case YIN:
            default:
                _detector = new Yin((float)audio.sr, audio.num * audio.dim);
                break;
        }
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] data = stream_in[0].ptrF();
        float[] out = stream_out.ptrF();

        PitchDetectionResult result = _detector.getPitch(data);

        float pitch = result.getPitch();
        if(pitch > options.maxPitch || pitch < options.minPitch)
            pitch = -1;

        int dim = 0;

        if (options.computePitch)
            out[dim++] = pitch;

        if (options.computePitchEnvelope) {
            if (pitch < 0) {
                out[dim++] = _lastPitch;
            } else {
                out[dim++] = pitch;
                _lastPitch = pitch;
            }
        }

        if(options.computeVoicedProb)
            out[dim++] = result.getProbability();

        if(options.computePitchedState)
            out[dim++] = (result.isPitched() && pitch > 0) ? 1.0f : 0.0f;
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int dim = 0;

        if(options.computePitch) dim++;
        if(options.computePitchEnvelope) dim++;
        if(options.computeVoicedProb) dim++;
        if(options.computePitchedState) dim++;

        return dim;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return 1;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        if(stream_in[0].bytes != 2 && stream_in[0].bytes != 4)
            Log.e("Unsupported input stream type");

        return 4;
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        if(stream_in[0].type != Cons.Type.SHORT && stream_in[0].type != Cons.Type.FLOAT)
            Log.e("Unsupported input stream type");

        return Cons.Type.FLOAT;
    }

    @Override
    public void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        stream_out.dataclass = new String[stream_out.dim];

        int i = 0;
        if(options.computePitch) stream_out.dataclass[i++] = "Pitch";
        if(options.computePitchEnvelope) stream_out.dataclass[i++] = "Pitch";
        if(options.computeVoicedProb) stream_out.dataclass[i++] = "VoicedProb";
        if(options.computePitchedState) stream_out.dataclass[i++] = "PitchedState";
    }

}
