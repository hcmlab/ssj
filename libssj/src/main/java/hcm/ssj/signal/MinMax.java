/*
 * MinMax.java
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

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * A general transformer to calculate min and/or max for every dimension in the provided streams.<br>
 * The output is ordered for every dimension as min than max.
 * Created by Frank Gaibler on 27.08.2015.
 */
public class MinMax extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");
        public final Option<Boolean> min = new Option<>("min", true, Boolean.class, "Calculate minimum for each frame");
        public final Option<Boolean> max = new Option<>("max", true, Boolean.class, "Calculate maximum for each frame");

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
    private float[][] floats;
    private int multiplier;
    private int[] streamDimensions;
    float[] minValues;
    float[] maxValues;

    /**
     *
     */
    public MinMax()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in  Stream[]
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        //no check for a specific type to allow for different providers
        if (stream_in.length < 1 || stream_in[0].dim < 1)
        {
            Log.e("invalid input stream");
            return;
        }
        //every stream should have the same sample number
        int num = stream_in[0].num;
        for (int i = 1; i < stream_in.length; i++)
        {
            if (num != stream_in[i].num)
            {
                Log.e("invalid input stream num for stream " + i);
                return;
            }
        }
        floats = new float[stream_in.length][];
        for (int i = 0; i < floats.length; i++)
        {
            floats[i] = new float[stream_in[i].num * stream_in[i].dim];
        }
        if (multiplier > 0)
        {
            minValues = new float[stream_out.dim / multiplier];
            maxValues = new float[stream_out.dim / multiplier];
        }
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        super.flush(stream_in, stream_out);
        floats = null;
        minValues = null;
        maxValues = null;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        if (multiplier > 0)
        {
            float[] out = stream_out.ptrF();
            if (options.min.get())
            {
                Arrays.fill(minValues, Float.MAX_VALUE);
            }
            if (options.max.get())
            {
                Arrays.fill(maxValues, -Float.MAX_VALUE); //Float.MIN_VALUE is the value closest to zero and not the lowest float value possible
            }
            //calculate values for each stream
            for (int i = 0; i < stream_in[0].num; i++)
            {
                int t = 0;
                for (int j = 0; j < stream_in.length; j++)
                {
                    Util.castStreamPointerToFloat(stream_in[j], floats[j]);
                    for (int k = 0; k < stream_in[j].dim; k++, t++)
                    {
                        float value = floats[j][i * stream_in[j].dim + k];
                        if (options.min.get())
                        {
                            minValues[t] = minValues[t] < value ? minValues[t] : value;
                        }
                        if (options.max.get())
                        {
                            maxValues[t] = maxValues[t] > value ? maxValues[t] : value;
                        }
                    }
                }
            }
            if (options.min.get() && !options.max.get())
            {
                System.arraycopy(minValues, 0, out, 0, minValues.length);
            } else if (!options.min.get() && options.max.get())
            {
                System.arraycopy(maxValues, 0, out, 0, maxValues.length);
            } else
            {
                for (int i = 0, j = 0; i < maxValues.length; i++)
                {
                    out[j++] = minValues[i];
                    out[j++] = maxValues[i];
                }
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
        multiplier = 0;
        multiplier = options.min.get() ? multiplier + 1 : multiplier;
        multiplier = options.max.get() ? multiplier + 1 : multiplier;
        if (multiplier <= 0)
        {
            Log.e("no option selected");
        }
        int overallDimension = 0;
        streamDimensions = new int[stream_in.length];
        for (int i = 0; i < streamDimensions.length; i++)
        {
            streamDimensions[i] = stream_in[i].dim * multiplier;
            overallDimension += streamDimensions[i];
        }
        return overallDimension;
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
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];
        if (options.outputClass.get() != null)
        {
            if (overallDimension == options.outputClass.get().length)
            {
                System.arraycopy(options.outputClass.get(), 0, stream_out.desc, 0, options.outputClass.get().length);
                return;
            } else
            {
                Log.w("invalid option outputClass length");
            }
        }
        for (int i = 0, k = 0; i < streamDimensions.length; i++)
        {
            for (int j = 0, m = 0; j < streamDimensions[i]; j += multiplier, m++)
            {
                if (options.min.get())
                {
                    stream_out.desc[k++] = "min" + i + "." + m;
                }
                if (options.max.get())
                {
                    stream_out.desc[k++] = "max" + i + "." + m;
                }
            }
        }
    }
}