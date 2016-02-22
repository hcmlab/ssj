/*
 * Envelope.java
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

package hcm.ssj.signal;

import android.util.Log;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.stream.Stream;

/**
 * Computes the envelope of the input signal
 *
 * An envelope detector is an electronic circuit that takes a high-frequency signal as input and provides
 * an output which is the envelope of the original signal.
 * The capacitor in the circuit stores up charge on the rising edge, and releases it slowly through the
 * resistor when the signal falls. The diode in series rectifies the incoming signal, allowing current
 * flow only when the positive input terminal is at a higher potential than the negative input terminal.
 * (Wikipedia)
 *
 * Created by Johnny on 01.04.2015.
 */
public class Envelope extends Transformer {

    public class Options
    {
        public float attackSlope = 0.1f; //increment by which the envelope should increase each sample
        public float releaseSlope = 0.1f; //increment by which the envelope should decrease each sample

    }
    public Options options = new Options();

    float[] _lastValue;

    public Envelope()
    {
        _name = "SSJ_transformer_Envelope";
    }

    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        _lastValue = new float[stream_in[0].dim];
        for(int i = 0; i < _lastValue.length; ++i)
            _lastValue[i] = 0;
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] data = stream_in[0].ptrF();
        float[] out = stream_out.ptrF();
        int dim = stream_in[0].dim;
        float valNew, valOld;

        for(int j = 0; j < stream_in[0].dim; ++j)
        {
            for(int i = 0; i < stream_in[0].num; ++i)
            {
                valNew = data[i * dim + j];
                valOld = (i > 1) ? out[(i-1) * dim + j] : _lastValue[j];

                if(valNew > valOld)
                    out[i * dim + j] = (valOld + options.attackSlope > valNew) ? valNew : valOld + options.attackSlope;
                else if(valNew < valOld)
                    out[i * dim + j] = (valOld - options.releaseSlope < valNew) ? valNew : valOld - options.releaseSlope;
                else if(valNew == valOld)
                    out[i * dim + j] = valOld;
            }
            _lastValue[j] = out[(stream_in[0].num -1) * dim + j];
        }
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        if(stream_in[0].dim != 1)
            Log.e(_name, "can only handle 1-dimensional streams");

        return 1;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return sampleNumber_in;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        if(stream_in[0].bytes != 4) //float
            Log.e(_name, "Unsupported input stream type");

        return 4; //float
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        if(stream_in[0].type != Cons.Type.FLOAT)
            Log.e(_name, "Unsupported input stream type");

        return Cons.Type.FLOAT;
    }

    @Override
    public void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        stream_out.dataclass = new String[stream_in[0].dim];
        System.arraycopy(stream_in[0].dataclass, 0, stream_out.dataclass, 0, stream_in[0].dataclass.length);
    }

}
