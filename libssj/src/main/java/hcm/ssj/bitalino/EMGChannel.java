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

package hcm.ssj.bitalino;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Ionut Damian on 06.07.2016.
 * outputs EMG value in mV
 */
public class EMGChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Integer> channel = new Option<>("channel", 0, Integer.class, "channel id (between 0 and 5)");
		public final Option<Integer> numBits = new Option<>("numBits", 10, Integer.class, "the first 4 channels are sampled using 10-bit resolution, the last two may be sampled using 6-bit");
		public final Option<Float> vcc = new Option<>("vcc", 3.3f, Float.class, "voltage at the common collector (default 3.3)");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	protected BitalinoListener _listener;

	public EMGChannel()
	{
		_name = "Bitalino_EMGChannel";
	}

	@Override
	public void init() throws SSJException
	{
		((Bitalino)_sensor).addChannel(options.channel.get());
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		_listener = ((Bitalino) _sensor).listener;
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		if (!_listener.isConnected())
		{
			return false;
		}

		float[] out = stream_out.ptrF();

		float adc = _listener.getAnalogData(options.channel.get());
		out[0] = (float)((((adc / (2 << options.numBits.get() -1))  - 0.5) * options.vcc.get()) / 1.009);

		return true;
	}

	@Override
	protected double getSampleRate()
	{
		return ((Bitalino)_sensor).options.sr.get();
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
		stream_out.desc[0] = "EMG";
	}
}
