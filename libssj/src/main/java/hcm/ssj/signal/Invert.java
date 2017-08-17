/*
 * Invert.java
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

package hcm.ssj.signal;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * inverts a signal by subtracting it from a predefined value
 *
 * Created by Johnny on 01.04.2015.
 */
public class Invert extends Transformer {

    public class Options extends OptionList
    {
        public final Option<Float> max  = new Option<>("max", 1.0f, Float.class, "");
        /**
         *
         */
        private Options() {addOptions();}
    }
    public final Options options = new Options();


    public Invert()
    {
        _name = "Invert";
    }

    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] out = stream_out.ptrF();

        for(int i = 0; i < stream_in[0].dim; i++)
        {
            for(int j = 0; j < stream_in[0].num; j++)
            {
                int index = j * stream_in[0].dim + i;
                switch(stream_in[0].type)
                {
                    case CHAR:
                        out[index] = options.max.get() - (float)stream_in[0].ptrC()[index];
                        break;
                    case SHORT:
                        out[index] = options.max.get() - (float)stream_in[0].ptrS()[index];
                        break;
                    case INT:
                        out[index] = options.max.get() - (float)stream_in[0].ptrI()[index];
                        break;
                    case FLOAT:
                        out[index] = options.max.get() - stream_in[0].ptrF()[index];
                        break;
                    case DOUBLE:
                        out[index] = options.max.get() - (float)stream_in[0].ptrD()[index];
                        break;
                }
            }
        }
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        return stream_in[0].dim;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return sampleNumber_in;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return 4; //float
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        switch(stream_in[0].type)
        {
            case CHAR:
            case SHORT:
            case INT:
            case FLOAT:
            case DOUBLE:
                return Cons.Type.FLOAT;
            default:
                Log.e("unsupported input type");
                return Cons.Type.UNDEF;
        }
    }

    @Override
    public void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[stream_in[0].dim];
        System.arraycopy(stream_in[0].desc, 0, stream_out.desc, 0, stream_in[0].desc.length);
    }

}
