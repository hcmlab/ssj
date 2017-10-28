/*
 * GSRChannel.java
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

import com.microsoft.band.sensors.GsrSampleRate;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Provides skin resistance in kOhms
 *
 * Created by Michael Dietz on 06.07.2016.
 */
public class GSRChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Float> sampleRate = new Option<>("sampleRate", 5.0f, Float.class, "supported sample rates: 0.2, 5.0 Hz");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	protected BandListener _listener;

	public GSRChannel()
	{
		_name = "MSBand_GSR";
	}

	@Override
	public void init() throws SSJException
	{
		GsrSampleRate sr;
		if (options.sampleRate.get() <= 0.2)
		{
			sr = GsrSampleRate.MS5000;
		}
		else
		{
			sr = GsrSampleRate.MS200;
		}

		((MSBand)_sensor).configureChannel(MSBand.Channel.GSR, true, sr.ordinal());
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
		out[0] = (float)_listener.getGsr();

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
		return 1;
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
		stream_out.desc[0] = "GSR";
	}
}
