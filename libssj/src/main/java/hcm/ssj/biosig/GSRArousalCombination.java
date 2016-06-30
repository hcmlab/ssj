/*
 * GSRArousalCombination.java
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

package hcm.ssj.biosig;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 13.08.2015.
 */
public class GSRArousalCombination extends Transformer
{
	public GSRArousalCombination()
	{
		_name = "SSJ_transformer_GSRArousalCombination";
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		int n = stream_in[0].num;
		double sr = stream_in[0].sr;

		float[] ptrLongTerm = stream_in[0].ptrF();
		float[] ptrShortTerm = stream_in[1].ptrF();
		float[] ptrOut = stream_out.ptrF();

		for (int nSamp = 0; nSamp < stream_in[0].num; nSamp++)
		{
			if (ptrLongTerm[nSamp] >= 0.0f && ptrShortTerm[nSamp] > 0.0f)
			{
				ptrOut[nSamp] = (float) (Math.sqrt(ptrShortTerm[nSamp]) * Math.sqrt(ptrLongTerm[nSamp])) * 1.5f;

				if (ptrOut[nSamp] >= 1.0f)
				{
					ptrOut[nSamp] = 1.0f;
				}
				if (ptrOut[nSamp] <= 0.0f)
				{
					ptrOut[nSamp] = 0.0f;
				}
			}
			else
			{
				ptrOut[nSamp] = 0.0f;
			}
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return 1;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if (stream_in[0].type != Cons.Type.FLOAT)
		{
			Log.e("unsupported input type");
		}

		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
	{
		stream_out.dataclass = new String[stream_in[0].dim];
		System.arraycopy(stream_in[0].dataclass, 0, stream_out.dataclass, 0, stream_in[0].dataclass.length);
	}
}
