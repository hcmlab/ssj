/*
 * EventSender.java
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

package hcm.ssj.event;

import java.util.EnumSet;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 07.07.2016.
 */
public class ValueEventSender extends Consumer
{
	public class Options extends OptionList
	{
		public final Option<String>    sender   = new Option<>("sender", null, String.class, "");
		public final Option<String>    event    = new Option<>("event", "event", String.class, "");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	public ValueEventSender()
	{
		_name = "SSJ_consumer_ValueEventSender";
		options.sender.set(_name);
	}

	@Override
	public void enter(Stream[] stream_in)
	{
		if (!EnumSet.of(Cons.Type.BYTE, Cons.Type.CHAR, Cons.Type.SHORT, Cons.Type.INT, Cons.Type.LONG, Cons.Type.FLOAT, Cons.Type.DOUBLE, Cons.Type.BOOL).contains(stream_in[0].type))
		{
			throw new RuntimeException("type "+ stream_in[0].type +" not supported");
		}
	}

	@Override
	protected void consume(Stream[] stream_in)
	{
		Object ptr = stream_in[0].ptr();

		Event ev = new Event();
		ev.name = options.event.get();
		ev.sender = options.sender.get();
		ev.time = (int) (1000 * stream_in[0].time + 0.5);
		ev.dur = (int) (1000 * (stream_in[0].num / stream_in[0].sr) + 0.5);
		ev.state = Event.State.COMPLETED;

		ev.msg = "";

		for (int i = 0; i < stream_in[0].num; i++)
		{
			for (int j = 0; j < stream_in[0].dim; j++)
			{
				switch (stream_in[0].type)
				{
					case BYTE:
						ev.msg += String.valueOf(((byte[])ptr)[i * stream_in[0].dim + j]);
						break;
					case CHAR:
						ev.msg += String.valueOf(((char[])ptr)[i * stream_in[0].dim + j]);
						break;
					case SHORT:
						ev.msg += String.valueOf(((short[])ptr)[i * stream_in[0].dim + j]);
						break;
					case INT:
						ev.msg += String.valueOf(((int[])ptr)[i * stream_in[0].dim + j]);
						break;
					case LONG:
						ev.msg += String.valueOf(((long[])ptr)[i * stream_in[0].dim + j]);
						break;
					case FLOAT:
						ev.msg += String.valueOf(((float[])ptr)[i * stream_in[0].dim + j]);
						break;
					case DOUBLE:
						ev.msg += String.valueOf(((double[])ptr)[i * stream_in[0].dim + j]);
						break;
					case BOOL:
						ev.msg += String.valueOf(((boolean[])ptr)[i * stream_in[0].dim + j]);
						break;
				}

				if (j < stream_in[0].dim - 1)
				{
					ev.msg += " ";
				}
			}
		}

		_evchannel_out.pushEvent(ev);
	}
}