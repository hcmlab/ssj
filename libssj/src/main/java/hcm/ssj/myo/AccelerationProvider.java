/*
 * AccelerationProvider.java
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

package hcm.ssj.myo;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 01.04.2015.
 */
public class AccelerationProvider extends SensorProvider
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

	public AccelerationProvider()
	{
        _name = "SSJ_sensor_Myo_Acceleration";
	}

    @Override
    public void enter(Stream stream_out)
    {
        _listener = ((Myo)_sensor).listener;

        if(stream_out.num != 1)
            Log.w("unsupported stream format. sample number = " + stream_out.num);
    }

	@Override
	protected void process(Stream stream_out)
	{
		float[] out = stream_out.ptrF();

        out[0] = _listener.accelerationX;
        out[1] = _listener.accelerationY;
        out[2] = _listener.accelerationZ;
	}

    @Override
    public void flush(Stream stream_out)
    {
    }

    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    @Override
    public int getSampleDimension()
    {
        return 3;
    }

    @Override
    public int getSampleBytes()
    {
        return 4;
    }

    @Override
    public Cons.Type getSampleType()
    {
        return Cons.Type.FLOAT;
    }

	@Override
	protected void defineOutputClasses(Stream stream_out)
	{
		stream_out.dataclass = new String[stream_out.dim];

		stream_out.dataclass[0] = "AccX";
		stream_out.dataclass[1] = "AccY";
		stream_out.dataclass[2] = "AccZ";
	}
}
