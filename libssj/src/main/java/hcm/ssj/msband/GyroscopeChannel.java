/*
 * GyroscopeChannel.java
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

package hcm.ssj.msband;

import com.microsoft.band.sensors.SampleRate;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class GyroscopeChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Float> sampleRate = new Option<>("sampleRate", 62.5f, Float.class, "supported sample rates: 7.8125, 31.25, 62.5 Hz");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	protected BandListener _listener;

	public GyroscopeChannel()
	{
		_name = "MSBand_Gyroscope";
	}

	@Override
	public void init() throws SSJException
	{
		SampleRate sr;
		if (options.sampleRate.get() <= 7.8125)
		{
			sr = SampleRate.MS128;
		}
		else if (options.sampleRate.get() <= 31.25)
		{
			sr = SampleRate.MS32;
		}
		else
		{
			sr = SampleRate.MS16;
		}

		((MSBand)_sensor).configureChannel(MSBand.Channel.Gyroscope, true, sr.ordinal());
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		_listener = ((MSBand) _sensor).listener;
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		if (!_listener.isConnected())
		{
			return false;
		}

		float[] out = stream_out.ptrF();
		out[0] = _listener.getAngularVelocityX();
		out[1] = _listener.getAngularVelocityY();
		out[2] = _listener.getAngularVelocityZ();
		out[3] = _listener.getAngularAccelerationX();
		out[4] = _listener.getAngularAccelerationY();
		out[5] = _listener.getAngularAccelerationZ();

		return true;
	}

	@Override
	protected double getSampleRate()
	{
		return options.sampleRate.get();
	}

	@Override
	protected int getSampleDimension()
	{
		return 6;
	}

	@Override
	protected Cons.Type getSampleType()
	{
		return Cons.Type.FLOAT;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];
		stream_out.desc[0] = "AngularVelX";
		stream_out.desc[1] = "AngularVelY";
		stream_out.desc[2] = "AngularVelZ";
		stream_out.desc[3] = "AngularAccX";
		stream_out.desc[4] = "AngularAccY";
		stream_out.desc[5] = "AngularAccZ";
	}
}
