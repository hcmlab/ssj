/*
 * AndroidSensorChannel.java
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

package hcm.ssj.androidSensor;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Standard provider for android sensors.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class AndroidSensorChannel extends SensorChannel
{
    /**
     * All options for the sensor provider
     */
    public class Options extends OptionList
    {
        /**
         * The rate in which the provider samples data from the sensor.<br>
         * <b>Attention!</b> The sensor will provide new data according to its sensor delay.
         */
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 50, Integer.class, "sample rate of sensor to provider");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    protected SensorListener _listener;
    private SensorType sensorType;

    /**
     *
     */
    public AndroidSensorChannel()
    {
        super();
        _name = "Android";
    }

    /**
     *
     */
    @Override
    public void init()
    {
        ((AndroidSensor) _sensor).init();
        this.sensorType = ((AndroidSensor) _sensor).getSensorType();
        _name = sensorType.getName();
    }

    /**
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream stream_out)
    {
        _listener = ((AndroidSensor) _sensor).listener;
        if (stream_out.num != 1)
        {
            Log.w("[" + sensorType.getName() + "] unsupported stream format. sample number = " + stream_out.num);
        }
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected boolean process(Stream stream_out)
    {
        int dimension = getSampleDimension();
        float[] out = stream_out.ptrF();
        for (int k = 0; k < dimension; k++)
        {
            out[k] = _listener.getData().getData(k);
        }

        return true;
    }

    /**
     * @return double
     */
    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    /**
     * @return int
     */
    @Override
    final public int getSampleDimension()
    {
        return sensorType.getDataSize();
    }

    /**
     * @return int
     */
    @Override
    public int getSampleBytes()
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /*
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType()
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream stream_out)
    {
        stream_out.desc = sensorType.getOutput();
    }
}
