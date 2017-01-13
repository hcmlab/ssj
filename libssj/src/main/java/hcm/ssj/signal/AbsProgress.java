/*
 * Progress.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer to calculate the absolute progress.<br>
 * Created by Frank Gaibler on 13.01.2017.
 */
public class AbsProgress extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    //helper variables
    private boolean initState = true;
    private float[] oldValues;

    /**
     *
     */
    public AbsProgress()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        //no check for a specific type to allow for different providers
        if (stream_in.length != 1 || stream_in[0].dim < 1)
        {
            Log.e("invalid input stream");
            return;
        }
        initState = true;
        oldValues = new float[stream_out.dim];
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {
        super.flush(stream_in, stream_out);
        oldValues = null;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] in = stream_in[0].ptrF(), out = stream_out.ptrF();
        if (initState)
        {
            initState = false;
            for (int i = 0; i < stream_in[0].dim; i++)
            {
                oldValues[i] = in[i];
            }
        }
        for (int c = 0; c < stream_in[0].dim; c++)
        {
            out[c] = 0;
            for (int i = c, k = 0; k < stream_in[0].num; k++, i += stream_in[0].dim)
            {
                float val = oldValues[c] - in[i];
                oldValues[c] = in[i];
                if (val < 0) {
                    val *= -1;
                }
                out[c] += val;
            }
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        return stream_in[0].dim;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return 1;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.dataclass = new String[overallDimension];
        if (options.outputClass.get() != null && overallDimension == options.outputClass.get().length)
        {
            System.arraycopy(options.outputClass.get(), 0, stream_out.dataclass, 0, options.outputClass.get().length);
        } else
        {
            if (options.outputClass.get() != null && overallDimension != options.outputClass.get().length)
            {
                Log.w("invalid option outputClass length");
            }
            for (int i = 0; i < overallDimension; i++)
            {
                stream_out.dataclass[i] = "abprgs" + i;
            }
        }
    }
}