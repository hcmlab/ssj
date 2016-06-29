/*
 * SimpleFileReaderProvider.java
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

package hcm.ssj.file;

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * File reader provider for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class SimpleFileReaderProvider extends SensorProvider
{
    /**
     *
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");
        public final Option<String> separator = new Option<>("separator", LoggingConstants.DELIMITER_ATTRIBUTE, String.class, "Attribute separator of the file");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private SimpleFileReader simpleFileReader;
    private double sampleRate;
    private int dimension;
    private Cons.Type type;

    /**
     *
     */
    public SimpleFileReaderProvider()
    {
        super();
        _name = "SSJ_provider_" + this.getClass().getSimpleName();
    }

    /**
     *
     */
    @Override
    protected void init()
    {
        simpleFileReader = (SimpleFileReader) _sensor;
        simpleFileReader.init();
        SimpleHeader simpleHeader = simpleFileReader.getSimpleHeader();
        sampleRate = Double.parseDouble(simpleHeader._sr);
        dimension = Integer.parseInt(simpleHeader._dim);
        type = Cons.Type.valueOf(simpleHeader._type);
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected void process(Stream stream_out)
    {
        switch (type)
        {
            case BOOL:
            {
                boolean[] out = stream_out.ptrBool();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Boolean.valueOf(separated[k]);
                }
                break;
            }
            case BYTE:
            {
                byte[] out = stream_out.ptrB();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Byte.valueOf(separated[k]);
                }
                break;
            }
            case CHAR:
            {
                char[] out = stream_out.ptrC();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = separated[k].charAt(0);
                }
                break;
            }
            case SHORT:
            {
                short[] out = stream_out.ptrS();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Short.valueOf(separated[k]);
                }
                break;
            }
            case INT:
            {
                int[] out = stream_out.ptrI();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Integer.valueOf(separated[k]);
                }
                break;
            }
            case LONG:
            {
                long[] out = stream_out.ptrL();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Long.valueOf(separated[k]);
                }
                break;
            }
            case FLOAT:
            {
                float[] out = stream_out.ptrF();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Float.valueOf(separated[k]);
                }
                break;
            }
            case DOUBLE:
            {
                double[] out = stream_out.ptrD();
                String[] separated = getData();
                for (int k = 0; k < dimension; k++)
                {
                    out[k] = Double.valueOf(separated[k]);
                }
                break;
            }
            default:
                Log.w("unsupported data type");
                break;
        }
    }

    /**
     * @return String[]
     */
    private String[] getData()
    {
        String data = simpleFileReader.getData();
        if (data != null)
        {
            return data.split(options.separator.getValue());
        }
        //notify listeners
        Monitor.notifyMonitor();
        String[] other = new String[dimension];
        Arrays.fill(other, "0");
        return other;
    }

    /**
     * @return double
     */
    @Override
    public double getSampleRate()
    {
        return sampleRate;
    }

    /**
     * @return int
     */
    @Override
    final public int getSampleDimension()
    {
        return dimension;
    }

    /**
     * @return int
     */
    @Override
    public int getSampleBytes()
    {
        return Util.sizeOf(type);
    }

    /**
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType()
    {
        return type;
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected void defineOutputClasses(Stream stream_out)
    {
        stream_out.dataclass = new String[dimension];
        if (options.outputClass.getValue() != null)
        {
            if (dimension == options.outputClass.getValue().length)
            {
                System.arraycopy(options.outputClass.getValue(), 0, stream_out.dataclass, 0, options.outputClass.getValue().length);
                return;
            } else
            {
                Log.w("invalid option outputClass length");
            }
        }
        for (int i = 0; i < dimension; i++)
        {
            stream_out.dataclass[i] = "SFRP" + i;
        }
    }
}
