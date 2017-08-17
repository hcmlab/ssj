/*
 * Pitch.java
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
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
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

    public class Options extends OptionList
    {
        public final Option<Integer> detector = new Option<>("detector", YIN, Integer.class, "");
        public final Option<Boolean> computePitch = new Option<>("computePitch", true, Boolean.class, "");
        public final Option<Boolean> computePitchEnvelope = new Option<>("computePitchEnvelope", false, Boolean.class, "if pitch is invalid, provide old value again");
        public final Option<Boolean> computeVoicedProb = new Option<>("computeVoicedProb", false, Boolean.class, "");
        public final Option<Boolean> computePitchedState = new Option<>("computePitchedState", false, Boolean.class, "");
        public final Option<Float> minPitch = new Option<>("minPitch", 52.0f, Float.class, "");
        public final Option<Float> maxPitch = new Option<>("maxPitch", 620.0f, Float.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    protected PitchDetector _detector;

    protected float _lastPitch = 0;

    public Pitch()
    {
        _name = "Pitch";
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

        switch(options.detector.get())
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
        if(pitch > options.maxPitch.get() || pitch < options.minPitch.get())
            pitch = -1;

        int dim = 0;

        if (options.computePitch.get())
            out[dim++] = pitch;

        if (options.computePitchEnvelope.get()) {
            if (pitch < 0) {
                out[dim++] = _lastPitch;
            } else {
                out[dim++] = pitch;
                _lastPitch = pitch;
            }
        }

        if(options.computeVoicedProb.get())
            out[dim++] = result.getProbability();

        if(options.computePitchedState.get())
            out[dim++] = (result.isPitched() && pitch > 0) ? 1.0f : 0.0f;
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int dim = 0;

        if(options.computePitch.get()) dim++;
        if(options.computePitchEnvelope.get()) dim++;
        if(options.computeVoicedProb.get()) dim++;
        if(options.computePitchedState.get()) dim++;

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
    public void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[stream_out.dim];

        int i = 0;
        if(options.computePitch.get()) stream_out.desc[i++] = "Pitch";
        if(options.computePitchEnvelope.get()) stream_out.desc[i++] = "Pitch";
        if(options.computeVoicedProb.get()) stream_out.desc[i++] = "VoicedProb";
        if(options.computePitchedState.get()) stream_out.desc[i++] = "PitchedState";
    }

}
