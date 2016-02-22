/*
 * IBIProvider.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.empatica;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class IBIProvider extends SensorProvider
{
	public class Options
	{
		public int sampleRate = 30;
	}
	public Options options = new Options();

	protected EmpaticaListener _listener;

	public IBIProvider()
	{
		_name = "SSJ_sensor_Empatica_IBI";
	}

	@Override
	public void enter(Stream stream_out)
	{
		_listener = ((Empatica)_sensor).listener;
	}

	@Override
	protected void process(Stream stream_out)
	{
		float[] out = stream_out.ptrF();
		out[0] = _listener.getIbi();
	}

	@Override
	public double getSampleRate()
	{
		return options.sampleRate;
	}

	@Override
	public int getSampleDimension()
	{
		return 1;
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
		stream_out.dataclass[0] = "IBI";
	}
}
