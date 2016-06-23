/*
 * AvgVar.java
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

package hcm.ssj.androidSensor.transformer;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * A general transformer to calculate average and/or variance for every dimension in the provided streams.<br>
 * The output is ordered for every dimension average than variance.<br>
 * Created by Frank Gaibler on 02.09.2015.
 */
public class AvgVar extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, Cons.Type.CUSTOM, "Describes the output names for every dimension in e.g. a graph");
        public final Option<Boolean> avg = new Option<>("avg", true, Cons.Type.BOOL, "Calculate average for each frame");
        public final Option<Boolean> var = new Option<>("var", true, Cons.Type.BOOL, "Calculate variance for each frame");

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
    private int multiplier;
    private int[] streamDimensions;

    /**
     *
     */
    public AvgVar()
    {
        _name = "SSJ_transformer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
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
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        if (multiplier > 0)
        {
            float[] out = stream_out.ptrF();
            float[] avgValues = new float[stream_out.dim / multiplier];
            //add up average values
            for (int i = 0; i < stream_in[0].num; i++)
            {
                int t = 0;
                for (Stream aStream_in : stream_in)
                {
                    float[] in = UtilAsTrans.getValuesAsFloat(aStream_in, _name);
                    for (int k = 0; k < aStream_in.dim; k++, t++)
                    {
                        float value = in[i * aStream_in.dim + k];
                        avgValues[t] += value;
                    }
                }
            }
            //calculate average
            for (int i = 0; i < avgValues.length; i++)
            {
                avgValues[i] = avgValues[i] / stream_in[0].num;
            }
            if (options.var.getValue())
            {
                float[] varValues = new float[stream_out.dim / multiplier];
                //add up variance values
                for (int i = 0; i < stream_in[0].num; i++)
                {
                    int t = 0;
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, t++)
                        {
                            float value = aStream_in.ptrF()[i * aStream_in.dim + k];
                            varValues[t] += (value - avgValues[t]) * (value - avgValues[t]);
                        }
                    }
                }
                //calculate variance
                for (int i = 0; i < varValues.length; i++)
                {
                    varValues[i] = varValues[i] / stream_in[0].num;
                }
                if (!options.avg.getValue())
                {
                    System.arraycopy(varValues, 0, out, 0, varValues.length);
                } else
                {
                    for (int i = 0, j = 0; i < varValues.length; i++)
                    {
                        out[j++] = avgValues[i];
                        out[j++] = varValues[i];
                    }
                }
                return;
            }
            System.arraycopy(avgValues, 0, out, 0, avgValues.length);
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
        multiplier = options.avg.getValue() ? multiplier + 1 : multiplier;
        multiplier = options.var.getValue() ? multiplier + 1 : multiplier;
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
    protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.dataclass = new String[overallDimension];
        if (options.outputClass.getValue() != null)
        {
            if (overallDimension == options.outputClass.getValue().length)
            {
                System.arraycopy(options.outputClass.getValue(), 0, stream_out.dataclass, 0, options.outputClass.getValue().length);
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
                if (options.avg.getValue())
                {
                    stream_out.dataclass[k++] = "avg" + i + "." + m;
                }
                if (options.var.getValue())
                {
                    stream_out.dataclass[k++] = "var" + i + "." + m;
                }
            }
        }
    }
}