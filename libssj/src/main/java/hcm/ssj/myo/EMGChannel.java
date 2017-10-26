/*
 * EMGChannel.java
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

package hcm.ssj.myo;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 01.04.2015.
 */
public class EMGChannel extends SensorChannel
{
    public class Options extends OptionList
    {
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 50, Integer.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
    public final Options options = new Options();

    protected MyoListener _listener;

	public EMGChannel()
	{
        _name = "Myo_EMG";
	}

    @Override
	public void enter(Stream stream_out) throws SSJFatalException
    {
        _listener = ((Myo)_sensor).listener;

		if (stream_out.num != 1)
		{
			Log.w("unsupported stream format. sample number = " + stream_out.num);
		}
    }

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		int[] out = stream_out.ptrI();

		for (int j = 0; j < stream_out.dim; j++)
		{
			out[j] = _listener.emg[j];
		}

        return true;
	}

    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    @Override
    public int getSampleDimension()
    {
        return 8;
    }

    @Override
    public int getSampleBytes()
    {
        return 4;
    }

    @Override
    public Cons.Type getSampleType()
    {
        return Cons.Type.INT;
    }

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

        for (int j = 0; j < stream_out.dim; j++)
		    stream_out.desc[j] = "EMG";
	}
}
