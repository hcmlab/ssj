/*
 * HRVSpectral.java
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

package hcm.ssj.biosig;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 07.04.2017.
 * computes common spectral features for heart rate
 *
 * code adapted from SSI's QRSHRVspectral.cpp
 */
public class HRVSpectral extends Transformer
{

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (stream_in[0].num != 1)
		{
			Log.e("ambiguous call: more than one sample gathered from spectogram");
		}

		float ptr_in[] = stream_in[0].ptrF();
		float ptr_out[] = stream_out.ptrF();

		float VLF = 0.0f;
		float LF = 0.0f;
		float HF = 0.0f;
		float nVLF = 0.0f;
		float nLF = 0.0f;
		float nHF = 0.0f;
		float dLFHF = 0.0f;
		float SMI = 0.0f;
		float VMI = 0.0f;
		float SVI = 0.0f;

		VLF = ptr_in[0];
		LF = ptr_in[1];
		HF = ptr_in[2];

		nVLF = (VLF * 100.0f) / (VLF + LF + HF);
		nLF = (LF * 100.0f) / (VLF + LF + HF);
		nHF = (HF * 100.0f) / (VLF + LF + HF);

		dLFHF = Math.abs(nLF - nHF);

		SMI = LF / (LF + HF);
		VMI = HF / (LF + HF);
		SVI = ( Math.abs(HF) < 0.0001) ? 0 : LF / HF;

		int iter = 0;
		ptr_out[iter++] = VLF;
		ptr_out[iter++] = LF;
		ptr_out[iter++] = HF;
		ptr_out[iter++] = nVLF;
		ptr_out[iter++] = nLF;
		ptr_out[iter++] = nHF;
		ptr_out[iter++] = dLFHF;
		ptr_out[iter++] = SMI;
		ptr_out[iter++] = VMI;
		ptr_out[iter] = SVI;
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		if(stream_in[0].dim != 3)
			Log.e("dimension > 1 not supported");

		return 10;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(Cons.Type.FLOAT);
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if(stream_in[0].type != Cons.Type.FLOAT)
			Log.e("input stream type not supported");

		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return 1;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[]{"VLF", "LF", "HF", "nVLF", "nLF", "nHF", "dLFHF", "SMI", "VMI", "SVI"};
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}
}
