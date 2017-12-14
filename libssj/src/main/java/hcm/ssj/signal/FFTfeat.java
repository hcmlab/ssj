/*
 * FFTfeat.java
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

package hcm.ssj.signal;

import org.jtransforms.fft.FloatFFT_1D;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 18.10.2016.
 */

public class FFTfeat extends Transformer
{
	private FloatFFT_1D fft;
	private float[][] fft_in;
	private float[][] fft_out;

	private int fft_dim = 0;
	private int fft_size = 0;
	private int rfft = 0;

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		fft_dim = stream_in[0].dim;
		fft_size = stream_in[0].num;

		fft = new FloatFFT_1D(fft_size);
		fft_out = new float[fft_dim][];
		fft_in = new float[fft_dim][];

		for(int i = 0; i < fft_dim; i++){
			fft_in[i]= new float[fft_size];
			fft_out[i] = new float[rfft];
		}
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		float[] in = stream_in[0].ptrF();
		float[] out = stream_out.ptrF();

		for (int j = 0; j < fft_size; j++) {
			for (int i = 0; i < fft_dim; i++) {
				if (j < stream_in[0].num){
					fft_in[i][j] = in[j * fft_dim + i];
				} else {
					fft_in[i][j] = 0;
				}
			}
		}

		for (int i = 0; i < fft_dim; i++)
		{
			// Calculate FFT
			fft.realForward(fft_in[i]);

			// Format values like in SSI
			Util.joinFFT(fft_in[i], fft_out[i]);
		}

		for (int j = 0; j < rfft; j++){
			for (int i = 0; i < fft_dim; i++){
				out[j * fft_dim + i] = fft_out[i][j];
			}
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		rfft = ((stream_in[0].num >> 1) + 1);
		return stream_in[0].dim * rfft;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if(stream_in[0].type != Cons.Type.FLOAT)
		{
			Log.e("Unsupported input stream type");
		}
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
		stream_out.desc = new String[]{"fft"};
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}
}
