/*
 * MvgNorm.java
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
 *
 * Normalizes input stream using moving/sliding minim and/or maximum for the chosen window.
 */
public class MvgNorm extends Transformer
{
	public enum Norm
	{
		AVG_VAR,
		MIN_MAX,
		SUB_AVG,
		SUB_MIN
	}

	public enum Method
	{
		MOVING,
		SLIDING
	}

	public class Options extends OptionList
	{
		public final Option<Norm> norm = new Option<>("norm", Norm.AVG_VAR, Norm.class, "");
		public final Option<Float> rangeA = new Option<>("rangeA", 0.f, Float.class, "");
		public final Option<Float> rangeB = new Option<>("rangeB", 1.f, Float.class, "");
		public final Option<Float> windowSize = new Option<>("windowSize", 10.f, Float.class, "");
		public final Option<Method> method = new Option<>("method", Method.MOVING, Method.class, "");
		public final Option<Integer> numberOfBlocks = new Option<>("numberOfBlocks", 10, Integer.class, "");

		/**
		 *
		 */
		private Options() {
			addOptions();
		}
	}

	public final Options options = new Options();

	float       _rangeA;
	float       _rangeD;
	Norm        _norm;
	Transformer _mvg;
	Stream      _dataTmp;

	public MvgNorm()
	{
		_name = "MvgNorm";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		_rangeA = options.rangeA.get();
		_rangeD = options.rangeB.get() - options.rangeA.get();
		_norm = options.norm.get();

		switch (_norm)
		{
			case AVG_VAR:
			{
				MvgAvgVar mvgAvgVar = new MvgAvgVar();
				mvgAvgVar.options.format.set(MvgAvgVar.Format.AVG_AND_VAR);
				mvgAvgVar.options.method.set(options.method.get() == Method.MOVING ? MvgAvgVar.Method.MOVING : MvgAvgVar.Method.SLIDING);
				mvgAvgVar.options.window.set((double) options.windowSize.get());

				_mvg = mvgAvgVar;
				break;
			}
			case MIN_MAX:
			{
				MvgMinMax mvgMinMax = new MvgMinMax();
				mvgMinMax.options.format.set(MvgMinMax.Format.ALL);
				mvgMinMax.options.method.set(options.method.get() == Method.MOVING ? MvgMinMax.Method.MOVING : MvgMinMax.Method.SLIDING);
				mvgMinMax.options.windowSize.set(options.windowSize.get());

				_mvg = mvgMinMax;
				break;
			}
			case SUB_AVG:
			{
				MvgAvgVar mvgAvgVar = new MvgAvgVar();
				mvgAvgVar.options.format.set(MvgAvgVar.Format.AVERAGE);
				mvgAvgVar.options.method.set(options.method.get() == Method.MOVING ? MvgAvgVar.Method.MOVING : MvgAvgVar.Method.SLIDING);
				mvgAvgVar.options.window.set((double) options.windowSize.get());

				_mvg = mvgAvgVar;
				break;
			}
			case SUB_MIN:
			{
				MvgMinMax mvgMinMax = new MvgMinMax();
				mvgMinMax.options.format.set(MvgMinMax.Format.MIN);
				mvgMinMax.options.method.set(options.method.get() == Method.MOVING ? MvgMinMax.Method.MOVING : MvgMinMax.Method.SLIDING);
				mvgMinMax.options.windowSize.set(options.windowSize.get());
				mvgMinMax.options.numberOfBlocks.set(options.numberOfBlocks.get());

				_mvg = mvgMinMax;
				break;
			}
		}

		_dataTmp = Stream.create(stream_in[0].num, _mvg.getSampleDimension(stream_in), Util.calcSampleRate(_mvg, stream_in[0]), _mvg.getSampleType(stream_in));

		_mvg.enter(stream_in, _dataTmp);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		int sampleDimension = stream_in[0].dim;
		int sampleNumber = stream_in[0].num;

		_mvg.transform(stream_in, _dataTmp);

		float[] srcPtr = stream_in[0].ptrF();
		float[] dstPtr = stream_out.ptrF();
		float[] tmpPtr = _dataTmp.ptrF();

		int srcPtrIndex = 0;
		int dstPtrIndex = 0;
		int tmpPtrIndex = 0;

		switch (_norm)
		{
			case AVG_VAR:
			{
				float x, y, avgVal, varVal;

				for (int i = 0; i < sampleNumber; i++)
				{
					for (int j = 0; j < sampleDimension; j++)
					{
						x = srcPtr[srcPtrIndex++];
						avgVal = tmpPtr[tmpPtrIndex++];
						varVal = tmpPtr[tmpPtrIndex++];
						y = (float) ((x - avgVal) / Math.sqrt(varVal));

						dstPtr[dstPtrIndex++] = y;
					}
				}

				break;
			}
			case MIN_MAX:
			{
				float x, y, minVal, maxVal, difVal;

				for (int i = 0; i < sampleNumber; i++)
				{
					for (int j = 0; j < sampleDimension; j++)
					{
						x = srcPtr[srcPtrIndex++];
						minVal = tmpPtr[tmpPtrIndex++];
						maxVal = tmpPtr[tmpPtrIndex++];
						difVal = maxVal - minVal;

						if (difVal != 0)
						{
							y = (x - minVal) / difVal;
						}
						else
						{
							y = 0;
						}

						y = _rangeA + y * _rangeD;

						dstPtr[dstPtrIndex++] = y;
					}
				}

				break;
			}
			case SUB_AVG:
			{
				float x, y, avgVal;

				for (int i = 0; i < sampleNumber; i++)
				{
					for (int j = 0; j < sampleDimension; j++)
					{
						x = srcPtr[srcPtrIndex++];
						avgVal = tmpPtr[tmpPtrIndex++];
						y = x - avgVal;

						dstPtr[dstPtrIndex++] = y;
					}
				}

				break;
			}
			case SUB_MIN:
			{
				float x, y, minVal;

				for (int i = 0; i < sampleNumber; i++)
				{
					for (int j = 0; j < sampleDimension; j++)
					{
						x = srcPtr[srcPtrIndex++];
						minVal = tmpPtr[tmpPtrIndex++];
						y = x - minVal;

						dstPtr[dstPtrIndex++] = y;
					}
				}

				break;
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
