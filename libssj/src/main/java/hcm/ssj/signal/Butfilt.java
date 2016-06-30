/*
 * Butfilt.java
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

package hcm.ssj.signal;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 10.08.2015.
 */
public class Butfilt extends Transformer
{
	public enum Type
	{
		LOW,
		HIGH,
		BAND
	}

	public class Options extends OptionList
	{
		public final Option<Type> type  = new Option<>("type", Type.BAND, Type.class, "");
		public final Option<Integer> order = new Option<>("order", 1, Integer.class, "Filter order");
		public final Option<Boolean> norm = new Option<>("norm", true, Boolean.class, "Frequency values are normalized in interval [0..1], where 1 is the nyquist frequency (=half the sample rate)");
		public final Option<Double> low = new Option<>("low", 0., Double.class, "Low cutoff frequency given either as normalized value in interval [0..1] or as an absolute value in Hz (see -norm)");
		public final Option<Double> high = new Option<>("high", 1., Double.class, "High cutoff frequency given either as normalized value in interval [0..1] or as an absolute value in Hz (see -norm)");
		public final Option<Boolean> zero = new Option<>("zero", false, Boolean.class, "Subtract first sample from signal to avoid artifacts at the beginning of the signal");

		/**
		 *
		 */
		private Options() {
			addOptions();
		}
	}

	public final Options options = new Options();

	IIR _iir;
	Matrix<Float> _coefficients;
	float[] _firstSample;

	boolean _firstCall;

	public Butfilt()
	{
		_name = "SSJ_transformer_Butfilt";
	}

	protected Matrix<Float> getCoefficients(double sr)
	{
		double low = options.norm.getValue() ? options.low.getValue() : 2 * options.low.getValue() / sr;
		double high = options.norm.getValue() ? options.high.getValue() : 2 * options.high.getValue() / sr;

		return initCoefficients(options.type.getValue(), options.order.getValue(), low, high);
	}

	protected Matrix<Float> initCoefficients(Type type, int order, double low, double high)
	{
		Matrix<Float> coefficients = null;

		switch (type)
		{
			case LOW:
				coefficients = FilterTools.getInstance().getLPButter(order, low);
				break;
			case HIGH:
				coefficients = FilterTools.getInstance().getHPButter(order, high);
				break;
			case BAND:
				coefficients = FilterTools.getInstance().getBPButter(order, low, high);
				break;
		}

		return coefficients;
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		_coefficients = getCoefficients(stream_in[0].sr);

		_iir = new IIR();
		_iir.setCoefficients(_coefficients);
		_iir.enter(stream_in, stream_out);

		_firstCall = true;
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		if (_firstCall)
		{
			if (options.zero.getValue())
			{
				_firstSample = new float[stream_in[0].dim];

				System.arraycopy(stream_in[0].ptrF(), 0, _firstSample, 0, stream_in[0].ptrF().length);
			}

			_firstCall = false;
		}

		if (_firstSample != null)
		{
			float[] ptr = stream_in[0].ptrF();
			int ptrIndex = 0;

			for (int i = 0; i < stream_in[0].num; i++) // TODO SSI: info.frame_num?
			{
				for (int j = 0; j < stream_in[0].dim; j++)
				{
					ptr[ptrIndex++] -= _firstSample[j];
				}
			}
		}

		_iir.transform(stream_in, stream_out);

		if (_firstSample != null)
		{
			float[] ptr = stream_out.ptrF();
			int ptrIndex = 0;

			for (int i = 0; i < stream_in[0].num; i++) // TODO SSI: info.frame_num?
			{
				for (int j = 0; j < stream_in[0].dim; j++)
				{
					ptr[ptrIndex++] += _firstSample[j];
				}
			}
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return stream_in[0].dim;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(Cons.Type.FLOAT); // Float
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
	public void defineOutputClasses(Stream[] stream_in, Stream stream_out)
	{
		stream_out.dataclass = new String[stream_in[0].dim];
		System.arraycopy(stream_in[0].dataclass, 0, stream_out.dataclass, 0, stream_in[0].dataclass.length);
	}
}
