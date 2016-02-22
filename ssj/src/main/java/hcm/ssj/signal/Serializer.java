/*
 * Serializer.java
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
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;

/**
 * Merges input streams which the same sample rate and the same type to one stream.<br>
 * Created by Frank Gaibler on 24.11.2015.
 */
public class Serializer extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options
    {
        /**
         * Describes the output names for every dimension in e.g. a graph.
         */
        public String[] outputClass = null;
        /**
         * The output type for which the input streams have to match.
         */
        public Cons.Type outputType = Cons.Type.FLOAT;
    }

    public Options options = new Options();

    /**
     *
     */
    public Serializer()
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
        //check for valid stream
        if (stream_in.length < 1 || stream_in[0].dim < 1)
        {
            Log.e(_name, "invalid input stream");
            return;
        }
        //every stream should have the same sample number
        int num = stream_in[0].num;
        for (int i = 1; i < stream_in.length; i++)
        {
            if (num != stream_in[i].num)
            {
                Log.e(_name, "invalid sample number for stream_in " + i);
                return;
            }
        }
        //not all types are supported
        if (options.outputType == Cons.Type.CUSTOM || options.outputType == Cons.Type.UNDEF)
        {
            Log.e(_name, "output type is not supported");
        }
        //every stream should have the same type
        for (int i = 0; i < stream_in.length; i++)
        {
            if (options.outputType != stream_in[i].type)
            {
                Log.e(_name, "invalid type for stream_in " + i);
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
        switch (options.outputType)
        {
            case BOOL:
            {
                boolean[] out = stream_out.ptrBool();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrBool()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case BYTE:
            {
                byte[] out = stream_out.ptrB();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrB()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case CHAR:
            {
                char[] out = stream_out.ptrC();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrC()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case DOUBLE:
            {
                double[] out = stream_out.ptrD();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrD()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case FLOAT:
            {
                float[] out = stream_out.ptrF();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrF()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case INT:
            {
                int[] out = stream_out.ptrI();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrI()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case LONG:
            {
                long[] out = stream_out.ptrL();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrL()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            case SHORT:
            {
                short[] out = stream_out.ptrS();
                for (int i = 0, z = 0; i < stream_in[0].num; i++)
                {
                    for (Stream aStream_in : stream_in)
                    {
                        for (int k = 0; k < aStream_in.dim; k++, z++)
                        {
                            //write to output
                            out[z] = aStream_in.ptrS()[i * aStream_in.dim + k];
                        }
                    }
                }
                break;
            }
            default:
                Log.e(_name, "output type is not supported");
                break;
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int overallDimension = 0;
        for (Stream stream : stream_in)
        {
            overallDimension += stream.dim;
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
        return Util.sizeOf(options.outputType);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return options.outputType;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return sampleNumber_in;
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
        if (options.outputClass != null)
        {
            if (overallDimension == options.outputClass.length)
            {
                System.arraycopy(options.outputClass, 0, stream_out.dataclass, 0, options.outputClass.length);
                return;
            } else
            {
                Log.w(_name, "invalid option outputClass length");
            }
        }
        for (int i = 0, k = 0; i < stream_in.length; i++)
        {
            for (int j = 0; j < stream_in[i].dim; j++, k++)
            {
                stream_out.dataclass[k] = "srlz" + i + "." + j;
            }
        }
    }
}