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
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Ionut Damian on 06.07.2016.
 * Outputs data from any channel of the BITalino board.
 * No data processing is performed.
 */
public class GenericChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Integer> channel = new Option<>("channel", 5, Integer.class, "channel id (between 0 and 5)");
		public final Option<Integer> channelType = new Option<>("channelType", 0, Integer.class, "analogue (0) or digital (1)");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	protected BitalinoListener _listener;

	public GenericChannel()
	{
		_name = "Bitalino_Channel";
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

		int[] out = stream_out.ptrI();

		if (options.channelType.get() == 0)
		{
			out[0] = _listener.getAnalogData(options.channel.get());
		}
		else
		{
			out[0] = _listener.getDigitalData(options.channel.get());
		}

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
		stream_out.desc[0] = "Ch" + options.channel.get();
	}
}
