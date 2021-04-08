/*
 * InfraredChannel.java
 * Copyright (c) 2018
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

package hcm.ssj.glass;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Infrared provider for google glass.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class InfraredChannel extends SensorChannel
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     * All options for the sensor provider
     */
    public class Options extends OptionList
    {
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 50, Integer.class, "The rate in which the provider samples data from the sensor");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private static final int DIMENSION = 1;

    InfraredSensor _irSensor;

    public InfraredChannel()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream stream_out) throws SSJFatalException
    {
        if (_sensor instanceof InfraredSensor)
        {
            _irSensor = (InfraredSensor)_sensor;
        } else
        {
            throw new RuntimeException(this.getClass() + ": " + _name + ": No Infrared sensor found");
        }
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected boolean process(Stream stream_out) throws SSJFatalException
    {
        float[] out = stream_out.ptrF();
        out[0] = _irSensor.getData();

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
        return DIMENSION;
    }

    /**
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
        int dimension = getSampleDimension();
        stream_out.desc = new String[dimension];
        if (dimension == 1)
        {
            stream_out.desc[0] = "Infrared";
        } else
        {
            for (int i = 0; i < dimension; i++)
            {
                stream_out.desc[i] = "Infrared" + i;
            }
        }
    }
}
