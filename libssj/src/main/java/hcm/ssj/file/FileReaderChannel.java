/*
 * FileReaderChannel.java
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

package hcm.ssj.file;

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * File reader provider for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class FileReaderChannel extends SensorChannel
{
    /**
     *
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");
        public final Option<String> separator = new Option<>("separator", LoggingConstants.DELIMITER_ATTRIBUTE, String.class, "Attribute separator of the file");
        public final Option<Double> offset = new Option<>("offset", 0.0, Double.class, "start reading from indicated time (in seconds)");
        public final Option<Double> chunk = new Option<>("chunk", 0.1, Double.class, "how many samples to read at once (in seconds)");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private FileReader fileReader;
    private byte[] buffer;
    private double sampleRate;
    private int dimension;
    private int num;
    private int bytes;
    private Cons.Type type;
    private Cons.FileType ftype;

    /**
     *
     */
    public FileReaderChannel()
    {
        super();
        _name = this.getClass().getSimpleName();
    }

    /**
     *
     */
    @Override
    protected void init()
    {
        fileReader = (FileReader) _sensor;
        fileReader.readerInit();
        SimpleHeader simpleHeader = fileReader.getSimpleHeader();
        sampleRate = Double.parseDouble(simpleHeader._sr);
        dimension = Integer.parseInt(simpleHeader._dim);
        bytes = Integer.parseInt(simpleHeader._byte);

        double minChunk = 1.0 / sampleRate;
        if(options.chunk.get() < minChunk) {
            Log.w("chunk size too small, setting to " + minChunk + "s");
            options.chunk.set(minChunk);
        }

        num = (int)(sampleRate * options.chunk.get() + 0.5);
        type = Cons.Type.valueOf(simpleHeader._type);
        ftype = Cons.FileType.valueOf(simpleHeader._ftype);

        buffer = new byte[num*dimension*bytes];
    }



    @Override
    public void enter(Stream stream_out)
    {
        if(options.offset.get() > 0)
            fileReader.skip((int)(stream_out.sr * options.offset.get()));
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected boolean process(Stream stream_out)
    {
        if(ftype == Cons.FileType.ASCII)
        {
            for(int i = 0; i < num; ++i) {
                switch (type) {
                    case BOOL: {
                        boolean[] out = stream_out.ptrBool();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Boolean.valueOf(separated[k]);
                            }
                        break;
                    }
                    case BYTE: {
                        byte[] out = stream_out.ptrB();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Byte.valueOf(separated[k]);
                            }
                        break;
                    }
                    case CHAR: {
                        char[] out = stream_out.ptrC();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = separated[k].charAt(0);
                            }
                        break;
                    }
                    case SHORT: {
                        short[] out = stream_out.ptrS();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Short.valueOf(separated[k]);
                            }
                        break;
                    }
                    case INT: {
                        int[] out = stream_out.ptrI();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Integer.valueOf(separated[k]);
                            }
                        break;
                    }
                    case LONG: {
                        long[] out = stream_out.ptrL();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Long.valueOf(separated[k]);
                            }
                        break;
                    }
                    case FLOAT: {
                        float[] out = stream_out.ptrF();

                            String[] separated = getData();
                            for (int k = 0; k < dimension; k++) {
                                out[i * dimension + k] = Float.parseFloat(separated[k]);
                            }
                        break;
                    }
                    case DOUBLE: {
                        double[] out = stream_out.ptrD();
                        String[] separated = getData();
                        for (int k = 0; k < dimension; k++) {
                            out[i * dimension + k] = Double.valueOf(separated[k]);
                        }
                        break;
                    }
                    default:
                        Log.w("unsupported data type");
                        return false;
                }
            }
        }
        else if(ftype == Cons.FileType.BINARY)
        {
            int numBytes = num * dimension * bytes;
            fileReader.getDataBinary(buffer, numBytes);
            Util.arraycopy(buffer, 0, stream_out.ptr(), 0, numBytes);
        }

        return true;
    }

    /**
     * @return String[]
     */
    private String[] getData()
    {
        String data = fileReader.getDataASCII();
        String[] result;

        if (data != null)
        {
            result = data.split(options.separator.get());
        }
        else
        {
            //notify listeners
            Monitor.notifyMonitor();
            result = new String[dimension];
            Arrays.fill(result, "0");
        }

        return result;
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
    final public int getSampleNumber()
    {
        return num;
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
    protected void describeOutput(Stream stream_out)
    {
        stream_out.desc = new String[dimension];
        if (options.outputClass.get() != null)
        {
            if (dimension == options.outputClass.get().length)
            {
                System.arraycopy(options.outputClass.get(), 0, stream_out.desc, 0, options.outputClass.get().length);
                return;
            } else
            {
                Log.w("invalid option outputClass length");
            }
        }
        for (int i = 0; i < dimension; i++)
        {
            stream_out.desc[i] = "SFRP" + i;
        }
    }
}
