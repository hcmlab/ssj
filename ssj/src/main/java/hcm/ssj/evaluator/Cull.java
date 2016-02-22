/*
 * Cull.java
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

package hcm.ssj.evaluator;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.SimpleXmlParser;

/**
 * Culls specific values from the input streams.<br>
 * Created by Frank Gaibler on 11.09.2015.
 */
public class Cull extends Transformer
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
         * Contains the used features.
         */
        public File fileTrainer = null;
    }

    public Options options = new Options();
    private int[] features = null;

    /**
     *
     */
    public Cull()
    {
        _name = "SSJ_transformer_" + this.getClass().getSimpleName();
    }

    /**
     * @param sources Provider[]
     * @param frame   double
     * @param delta   double
     */
    @Override
    public void setup(Provider[] sources, double frame, double delta)
    {
        if (sources.length < 1)
        {
            Log.e(_name, "sources count not supported");
        }
        loadTrainer();
        super.setup(sources, frame, delta);
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
            Log.e(_name, "invalid input stream");
        }
        //sample number should be 1
        if (stream_in[0].num > 1)
        {
            Log.e(_name, "invalid input stream num");
        }
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] out = stream_out.ptrF();
        ArrayList<Float> arrayList = new ArrayList<>();
        for (Stream stream : stream_in)
        {
            for (float f : stream.ptrF())
            {
                arrayList.add(f);
            }
        }
        //write to output
        int t = 0;
        for (int i : features)
        {
            out[t++] = arrayList.get(i);
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        return features != null ? features.length : 0;
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
        for (int i = 0; i < overallDimension; i++)
        {
            stream_out.dataclass[i] = "ftr" + i;
        }
    }

    /**
     * Load data from trainer file
     */
    private void loadTrainer()
    {
        File file = options.fileTrainer;
        if (file == null)
        {
            Log.e(_name, "trainer file not set in options");
            return;
        }
        SimpleXmlParser simpleXmlParser = new SimpleXmlParser();
        try
        {
            SimpleXmlParser.XmlValues xmlValues = simpleXmlParser.parse(
                    new FileInputStream(file),
                    new String[]{"trainer", "select", "item"},
                    new String[]{"select"}
            );
            String[] attribute = xmlValues.foundAttributes.get(0)[0].split(" ");
            features = new int[attribute.length];
            for (int i = 0; i < attribute.length; i++)
            {
                features[i] = Integer.valueOf(attribute[i]);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.e(_name, "file could not be parsed");
        }
    }
}