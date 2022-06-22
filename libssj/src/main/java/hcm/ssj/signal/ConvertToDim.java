/*
 * ConvertToDim.java
 * Copyright (c) 2022
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

package hcm.ssj.signal;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 23.04.2022.
 */
public class ConvertToDim extends Transformer
{
	public ConvertToDim()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		for (int i = 0; i < stream_in[0].num; i++)
		{
			switch (stream_out.type)
			{
				case BOOL:
					stream_out.ptrBool()[i] = stream_in[0].ptrBool()[i];
					break;
				case BYTE:
					stream_out.ptrB()[i] = stream_in[0].ptrB()[i];
					break;
				case CHAR:
					stream_out.ptrC()[i] = stream_in[0].ptrC()[i];
					break;
				case DOUBLE:
					stream_out.ptrD()[i] = stream_in[0].ptrD()[i];
					break;
				case FLOAT:
					stream_out.ptrF()[i] = stream_in[0].ptrF()[i];
					break;
				case INT:
					stream_out.ptrI()[i] = stream_in[0].ptrI()[i];
					break;
				case LONG:
					stream_out.ptrL()[i] = stream_in[0].ptrL()[i];
					break;
				case SHORT:
					stream_out.ptrS()[i] = stream_in[0].ptrS()[i];
					break;
			}
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return stream_in[0].num;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return stream_in[0].type;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return 1;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		for (int i = 0; i < stream_out.dim; i++)
		{
			stream_out.desc[i] = stream_in[0].desc[0] + "_dim" + i;
		}
	}
}
