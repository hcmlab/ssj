/*
 * BitalinoChannel.java
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
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class BitalinoChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Bitalino.Channel> channel = new Option<>("channel", Bitalino.Channel.A6_LUX, Bitalino.Channel.class, "channel type to use");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	protected BitalinoListener _listener;

	public BitalinoChannel()
	{
		_name = "Bitalino_Channel";
	}

	@Override
	public void init()
	{
		((Bitalino)_sensor).addChannel(options.channel.get());
	}

	@Override
	public void enter(Stream stream_out)
	{
		_listener = ((Bitalino) _sensor).listener;
	}

	@Override
	protected boolean process(Stream stream_out)
	{
		if(!_listener.isConnected())
			return false;

		int[] out = stream_out.ptrI();

		if(options.channel.get().isAnalogue())
			out[0] = _listener.getAnalogData(options.channel.get().getDataPosition());
		else
			out[0] = _listener.getDigitalData(options.channel.get().getDataPosition());

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
		return Cons.Type.INT;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];
		stream_out.desc[0] = options.channel.get().toString();
	}
}
