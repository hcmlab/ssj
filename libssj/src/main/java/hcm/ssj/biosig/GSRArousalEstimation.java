/*
 * GSRArousalEstimation.java
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
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.signal.Butfilt;
import hcm.ssj.signal.MvgAvgVar;
import hcm.ssj.signal.MvgNorm;

/**
 * Created by Michael Dietz on 06.08.2015.
 */
public class GSRArousalEstimation extends Transformer
{

	public class Options extends OptionList
	{
		public final Option<Double> windowSizeShortTerm = new Option<>("windowSizeShortTerm", 5., Double.class, "Size of short time window in seconds");
		public final Option<Double> windowSizeLongTerm = new Option<>("windowSizeLongTerm", 60., Double.class, "Size of long time window in seconds");

		/**
		 *
		 */
		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	MvgNorm _detrend;
	MvgNorm _detrendNormMinMax;

	MvgAvgVar _mvgAvgLongTerm;
	MvgAvgVar _mvgAvgShortTerm;
	MvgAvgVar _mvgAvgCombination;

	Butfilt               _butfiltLongTerm;
	GSRArousalCombination _combination;

	Stream _detrendStream;
	Stream _detrendNormMinMaxStream;
	Stream _mvgAvgLongTermStream;
	Stream _butfiltLongTermStream;
	Stream _mvgAvgShortTermStream;
	Stream _combinationStream;
	Stream _mvgAvgCombinationStream;

	Stream[] _detrendStreamArray;
	Stream[] _detrendNormMinMaxStreamArray;
	Stream[] _mvgAvgLongTermStreamArray;
	Stream[] _mvgAvgShortTermStreamArray;
	Stream[] _combinationStreamArray;
	Stream[] _combinationInput;

	public GSRArousalEstimation()
	{
		_name = "SSJ_transformer_GSRArousalEstimation";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		// Detrend: remove y offset
		_detrend = new MvgNorm();
		_detrend.options.norm.setValue(MvgNorm.Norm.SUB_MIN);
		_detrend.options.method.setValue(MvgNorm.Method.SLIDING);
		_detrend.options.windowSize.setValue(60.f);

		_detrendStream = Stream.create(_detrend.getSampleNumber(stream_in[0].num), _detrend.getSampleDimension(stream_in), _detrend.getSampleBytes(stream_in), Util.calcSampleRate(_detrend, stream_in[0]), _detrend.getSampleType(stream_in));
		_detrendStreamArray = new Stream[]{_detrendStream};
		_detrend.enter(stream_in, _detrendStream);

		// Detrend min max: convert to 0..1 range
		_detrendNormMinMax = new MvgNorm();
		_detrendNormMinMax.options.norm.setValue(MvgNorm.Norm.MIN_MAX);
		_detrendNormMinMax.options.rangeA.setValue(0.f);
		_detrendNormMinMax.options.rangeB.setValue(1.f);
		_detrendNormMinMax.options.method.setValue(MvgNorm.Method.SLIDING);
		_detrendNormMinMax.options.windowSize.setValue(60.f);

		_detrendNormMinMaxStream = Stream.create(_detrendNormMinMax.getSampleNumber(_detrendStream.num), _detrendNormMinMax.getSampleDimension(_detrendStreamArray), _detrendNormMinMax.getSampleBytes(_detrendStreamArray), Util.calcSampleRate(_detrendNormMinMax, _detrendStream), _detrendNormMinMax.getSampleType(_detrendStreamArray));
		_detrendNormMinMaxStreamArray = new Stream[]{_detrendNormMinMaxStream};
		_detrendNormMinMax.enter(_detrendStreamArray, _detrendNormMinMaxStream);

		// Moving average long term
		_mvgAvgLongTerm = new MvgAvgVar();
		_mvgAvgLongTerm.options.format.setValue(MvgAvgVar.Format.AVERAGE);
		_mvgAvgLongTerm.options.method.setValue(MvgAvgVar.Method.SLIDING);
		_mvgAvgLongTerm.options.window.setValue(90.);

		_mvgAvgLongTermStream = Stream.create(_mvgAvgLongTerm.getSampleNumber(_detrendNormMinMaxStream.num), _mvgAvgLongTerm.getSampleDimension(_detrendNormMinMaxStreamArray), _mvgAvgLongTerm.getSampleBytes(_detrendNormMinMaxStreamArray), Util.calcSampleRate(_mvgAvgLongTerm, _detrendNormMinMaxStream), _mvgAvgLongTerm.getSampleType(_detrendNormMinMaxStreamArray));
		_mvgAvgLongTermStreamArray = new Stream[]{_mvgAvgLongTermStream};
		_mvgAvgLongTerm.enter(_detrendNormMinMaxStreamArray, _mvgAvgLongTermStream);

		_butfiltLongTerm = new Butfilt();
		_butfiltLongTerm.options.zero.setValue(true);
		_butfiltLongTerm.options.norm.setValue(false);
		_butfiltLongTerm.options.low.setValue(0.1);
		_butfiltLongTerm.options.order.setValue(3);
		_butfiltLongTerm.options.type.setValue(Butfilt.Type.LOW);

		_butfiltLongTermStream = Stream.create(_butfiltLongTerm.getSampleNumber(_mvgAvgLongTermStream.num), _butfiltLongTerm.getSampleDimension(_mvgAvgLongTermStreamArray), _butfiltLongTerm.getSampleBytes(_mvgAvgLongTermStreamArray), Util.calcSampleRate(_butfiltLongTerm, _mvgAvgLongTermStream), _butfiltLongTerm.getSampleType(_mvgAvgLongTermStreamArray));
		_butfiltLongTerm.enter(_mvgAvgLongTermStreamArray, _butfiltLongTermStream);

		// Moving average short term
		_mvgAvgShortTerm = new MvgAvgVar();
		_mvgAvgShortTerm.options.format.setValue(MvgAvgVar.Format.AVERAGE);
		_mvgAvgShortTerm.options.method.setValue(MvgAvgVar.Method.SLIDING);
		_mvgAvgShortTerm.options.window.setValue(5.);

		_mvgAvgShortTermStream = Stream.create(_mvgAvgShortTerm.getSampleNumber(_detrendNormMinMaxStream.num), _mvgAvgShortTerm.getSampleDimension(_detrendNormMinMaxStreamArray), _mvgAvgShortTerm.getSampleBytes(_detrendNormMinMaxStreamArray), Util.calcSampleRate(_mvgAvgShortTerm, _detrendNormMinMaxStream), _mvgAvgShortTerm.getSampleType(_detrendNormMinMaxStreamArray));
		_mvgAvgShortTermStreamArray = new Stream[]{_mvgAvgShortTermStream};
		_mvgAvgShortTerm.enter(_detrendNormMinMaxStreamArray, _mvgAvgShortTermStream);

		// GSR Arousal Combination
		_combination = new GSRArousalCombination();

		_combinationStream = Stream.create(_combination.getSampleNumber(_mvgAvgShortTermStream.num), _combination.getSampleDimension(_mvgAvgShortTermStreamArray), _combination.getSampleBytes(_mvgAvgShortTermStreamArray), Util.calcSampleRate(_combination, _mvgAvgShortTermStream), _combination.getSampleType(_mvgAvgShortTermStreamArray));
		_combinationStreamArray = new Stream[]{_combinationStream};
		_combinationInput = new Stream[]{_butfiltLongTermStream, _mvgAvgShortTermStream};
		_combination.enter(_combinationInput, _combinationStream);

		// Moving average combination
		_mvgAvgCombination = new MvgAvgVar();
		_mvgAvgCombination.options.format.setValue(MvgAvgVar.Format.AVERAGE);
		_mvgAvgCombination.options.method.setValue(MvgAvgVar.Method.SLIDING);
		_mvgAvgCombination.options.window.setValue(30.);

		_mvgAvgCombinationStream = Stream.create(_mvgAvgCombination.getSampleNumber(_combinationStream.num), _mvgAvgCombination.getSampleDimension(_combinationStreamArray), _mvgAvgCombination.getSampleBytes(_combinationStreamArray), Util.calcSampleRate(_mvgAvgCombination, _combinationStream), _mvgAvgCombination.getSampleType(_combinationStreamArray));
		_mvgAvgCombination.enter(_combinationStreamArray, _mvgAvgCombinationStream);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		int n = stream_in[0].num;
		double sr = stream_in[0].sr;

		// Adjust stream size
		_detrendStream.adjust(n);
		_detrendNormMinMaxStream.adjust(n);
		_mvgAvgLongTermStream.adjust(n);
		_butfiltLongTermStream.adjust(n);
		_mvgAvgShortTermStream.adjust(n);
		_combinationStream.adjust(n);
		_mvgAvgCombinationStream.adjust(n);

		_detrend.transform(stream_in, _detrendStream);
		_detrendNormMinMax.transform(_detrendStreamArray, _detrendNormMinMaxStream);

		// Copy stream
		Stream detrendNormMinMaxStreamCopy = _detrendNormMinMaxStream.clone();
		Stream[] detrendNormMinMaxStreamCopyArray = new Stream[]{detrendNormMinMaxStreamCopy};

		_mvgAvgLongTerm.transform(_detrendNormMinMaxStreamArray, _mvgAvgLongTermStream);
		_butfiltLongTerm.transform(_mvgAvgLongTermStreamArray, _butfiltLongTermStream);
		_mvgAvgShortTerm.transform(detrendNormMinMaxStreamCopyArray, _mvgAvgShortTermStream);

		_combination.transform(_combinationInput, _combinationStream);
		_mvgAvgCombination.transform(_combinationStreamArray , _mvgAvgCombinationStream);

		float[] ptrResult = _mvgAvgCombinationStream.ptrF();
		float[] ptrOut = stream_out.ptrF();

		for (int nSamp = 0; nSamp < stream_in[0].num; nSamp++)
		{
			ptrOut[nSamp] = ptrResult[nSamp];
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
