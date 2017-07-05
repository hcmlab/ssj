/*
 * IIR.java
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
import hcm.ssj.core.stream.Stream;

/**
 * Provides second-order infinite impulse response (IIR) filtering.
 *
 * Created by Michael Dietz on 11.08.2015.
 */
public class IIR extends Transformer
{
	int _sections;
	Matrix<Float> _coefficients;
	Matrix<Float> _history;

	public IIR()
	{
		_name = "IIR";
	}

	public void setCoefficients(Matrix<Float> coefficients)
	{
		_coefficients = null;
		_sections = coefficients.getRows();

		// filt = [b_11, b_12, b_13, 1, a_12, a_13;
		//         b_21, b_22, b_23, 1, a_22, a_23;
		//                      ...
		//         b_n1, b_n2, b_n3, 1, a_n2, a_n3];
		//
		// so all we need to store are b_x1, b_x2, b_x3 and a_x2, a_x3
		// also, we store them in the order a_x2, a_x3, b_x1, b_x2, b_x3
		// since this is the order in which we'll access them later

		_coefficients = new Matrix<>(_sections, 5);

		for (int i = 0; i < _sections; i++)
		{
			_coefficients.setData(i, 0, coefficients.getData(i, 4));
			_coefficients.setData(i, 1, coefficients.getData(i, 5));
			_coefficients.setData(i, 2, coefficients.getData(i, 0));
			_coefficients.setData(i, 3, coefficients.getData(i, 1));
			_coefficients.setData(i, 4, coefficients.getData(i, 2));
		}
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		int sampleDimension = stream_in[0].dim;

		_history = new Matrix<>(_sections * sampleDimension, 2);
		_history.fillValue(0.0f);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		int sampleDimension = stream_in[0].dim;
		int sampleNumber = stream_in[0].num;

		float[] srcPtr = stream_in[0].ptrF();
		float[] dstPtr = stream_out.ptrF();

		float hist1;
		float hist2;
		float newHist;

		int srcIndex = 0;
		int dstIndex = 0;

		int histPtrIndex = 0;
		int coefsPtrIndex = 0;
		int coefsTmpPtrIndex = 0;
		int histPtrTmpIndex = 0;
		int histPtrTmp1Index = 0;
		int histPtrTmp2Index = 0;

		for (int i = 0; i < sampleNumber; i++)
		{
			histPtrTmpIndex = histPtrIndex;

			for (int j = 0; j < sampleDimension; j++)
			{
				dstPtr[dstIndex] = srcPtr[srcIndex];
				coefsTmpPtrIndex = coefsPtrIndex;
				histPtrTmp1Index = histPtrTmpIndex;
				histPtrTmp2Index = histPtrTmp1Index + 1;

				for (int k = 0; k < _sections; k++)
				{
					hist1 = _history.getData(histPtrTmp1Index);
					hist2 = _history.getData(histPtrTmp2Index);

					dstPtr[dstIndex] -= hist1 * _coefficients.getData(coefsTmpPtrIndex++); // a_x2
					newHist = dstPtr[dstIndex] - hist2 * _coefficients.getData(coefsTmpPtrIndex++); // a_x3
					dstPtr[dstIndex] = newHist * _coefficients.getData(coefsTmpPtrIndex++); // b_x1
					dstPtr[dstIndex] += hist1 * _coefficients.getData(coefsTmpPtrIndex++); // b_x2
					dstPtr[dstIndex] += hist2 * _coefficients.getData(coefsTmpPtrIndex++); // b_x3

					_history.setData(histPtrTmp2Index++, hist1);
					_history.setData(histPtrTmp1Index++, newHist);

					histPtrTmp2Index++;
					histPtrTmp1Index++;
				}

				srcIndex++;
				dstIndex++;

				histPtrTmpIndex += (_sections << 1);
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
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_in[0].dim];
		System.arraycopy(stream_in[0].desc, 0, stream_out.desc, 0, stream_in[0].desc.length);
	}
}
