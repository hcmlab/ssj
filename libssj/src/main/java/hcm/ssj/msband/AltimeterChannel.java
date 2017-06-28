/*
 * AltimeterProvider.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

import hcm.ssj.core.Cons;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class AltimeterChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Integer> sampleRate = new Option<>("sampleRate", 1, Integer.class, "");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	protected BandListener _listener;

	public AltimeterChannel()
	{
		_name = "MSBand_Altimeter";
	}

	@Override
	public void init()
	{
		((MSBand)_sensor).configureChannel(MSBand.Channel.Altimeter, true, 0);
	}

	@Override
	public void enter(Stream stream_out)
	{
		_listener = ((MSBand) _sensor).listener;
	}

	@Override
	protected boolean process(Stream stream_out)
	{
		if(!_listener.isConnected())
			return false;

		long[] out = stream_out.ptrL();
		out[0] = _listener.getFlightsAscended();
		out[1] = _listener.getFlightsDescended();
		out[2] = _listener.getSteppingGain();
		out[3] = _listener.getSteppingLoss();
		out[4] = _listener.getStepsAscended();
		out[5] = _listener.getStepsDescended();
		out[6] = _listener.getAltimeterGain();
		out[7] = _listener.getAltimeterLoss();

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
		return 8;
	}

	@Override
	protected Cons.Type getSampleType()
	{
		return Cons.Type.LONG;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];
		stream_out.desc[0] = "FlightsAscended";
		stream_out.desc[1] = "FlightsDescended";
		stream_out.desc[2] = "SteppingGain";
		stream_out.desc[3] = "SteppingLoss";
		stream_out.desc[4] = "StepsAscended";
		stream_out.desc[5] = "StepsDescended";
		stream_out.desc[6] = "AltimeterGain";
		stream_out.desc[7] = "AltimeterLoss";
	}
}
