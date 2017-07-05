/*
 * MvgMinMax.java
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

import java.util.EnumSet;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 06.08.2015.
 *
 * Computes moving/sliding minim and/or maximum of the input stream for the chosen window.
 */
public class MvgMinMax extends Transformer
{
	public enum Method
	{
		MOVING,
		SLIDING
	}

	public enum Format
	{
		MIN,
		MAX,
		ALL
	}

	public class Options extends OptionList
	{
		public final Option<Float> windowSize = new Option<>("windowSize", 10.f, Float.class, "");
		public final Option<Method> method = new Option<>("method", Method.MOVING, Method.class, "");
		public final Option<Format> format = new Option<>("format", Format.MIN, Format.class, "");
		public final Option<Integer> numberOfBlocks = new Option<>("numberOfBlocks", 10, Integer.class, "");

		/**
		 *
		 */
		private Options() {
			addOptions();
		}
	}

	public final Options options = new Options();

	Implementation _impl;

	public MvgMinMax()
	{
		_name = "MvgMinMax";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		if (options.method.get() == Method.MOVING)
		{
			_impl = new Moving(options);
		}
		else
		{
			_impl = new Sliding(options);
		}

		_impl.enter(stream_in[0], stream_out);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		_impl.transform(stream_in[0], stream_out);
	}

	@Override
	public void flush(Stream[] stream_in, Stream stream_out)
	{
		_impl.flush(stream_in[0], stream_out);
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		int bits = 0;

		if (EnumSet.of(Format.MIN, Format.ALL).contains(options.format.get()))
		{
			bits++;
		}

		if (EnumSet.of(Format.MAX, Format.ALL).contains(options.format.get()))
		{
			bits++;
		}

		return stream_in[0].dim * bits;
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

	interface Implementation
	{
		void enter(Stream stream_in, Stream stream_out);

		void transform(Stream stream_in, Stream stream_out);

		void flush(Stream stream_in, Stream stream_out);
	}

	class Moving implements Implementation
	{
		Options options;

		int _windowSizeInSamples;
		int _blockLengthInSamples;

		float[] _history;
		float[] _minMax;

		int _currentSampleIndex;
		int _currentBlockIndex;

		public Moving(Options options)
		{
			this.options = options;
		}

		@Override
		public void enter(Stream stream_in, Stream stream_out)
		{
			int sampleDimension = stream_in.dim;
			double sampleRate = stream_in.sr;

			double exactWindowSizeInSamples = options.windowSize.get() * sampleRate;
			double exactBlockLengthInSamples = exactWindowSizeInSamples / options.numberOfBlocks.get();
			int newBlockLengthInSamples = (int) (exactBlockLengthInSamples + 0.5); // Round
			int newWindowSizeInSamples = newBlockLengthInSamples * options.numberOfBlocks.get();

			_windowSizeInSamples = newWindowSizeInSamples;
			_blockLengthInSamples = newBlockLengthInSamples;

			_history = new float[(options.numberOfBlocks.get() << 1) * sampleDimension];
			_minMax = new float[sampleDimension << 1];

			int historyIterator = 0;
			for (int i = 0; i < options.numberOfBlocks.get(); i++)
			{
				for (int j = 0; j < sampleDimension; j++)
				{
					_history[historyIterator++] = Float.MAX_VALUE;
					_history[historyIterator++] = -Float.MAX_VALUE;
				}
			}

			int minMaxIterator = 0;
			for (int i = 0; i < sampleDimension; i++)
			{
				_minMax[minMaxIterator++] = Float.MAX_VALUE;
				_minMax[minMaxIterator++] = -Float.MAX_VALUE;
			}

			_currentSampleIndex = _blockLengthInSamples - 1;
			_currentBlockIndex = options.numberOfBlocks.get() - 1;
		}

		@Override
		public void transform(Stream stream_in, Stream stream_out)
		{
			int sampleDimension = stream_in.dim;
			int sampleNumber = stream_in.num;

			float[] srcPtr = stream_in.ptrF();
			float[] dstPtr = stream_out.ptrF();

			int historyIndex = 0;
			int minMaxIndex = 0;
			int dstIndex = 0;

			for (int curRelSample = 0; curRelSample < sampleNumber; curRelSample++)
			{
				// Check if we have to move to next block
				if (++_currentSampleIndex >= _blockLengthInSamples)
				{
					_currentSampleIndex = 0;

					if (++_currentBlockIndex >= options.numberOfBlocks.get())
					{
						_currentBlockIndex = 0;
					}

					historyIndex = sampleDimension * (_currentBlockIndex << 1);

					for (int forEachDimension = 0; forEachDimension < sampleDimension; forEachDimension++)
					{
						_history[historyIndex++] = srcPtr[forEachDimension];
						_history[historyIndex++] = srcPtr[forEachDimension];
					}
				}

				// Update min/max in current block
				historyIndex = sampleDimension * (_currentBlockIndex << 1);

				for (int forEachDimension = 0; forEachDimension < sampleDimension; forEachDimension++)
				{
					if (_history[historyIndex] > srcPtr[forEachDimension])
					{
						_history[historyIndex] = srcPtr[forEachDimension];
					}

					historyIndex++;

					if (_history[historyIndex] < srcPtr[forEachDimension])
					{
						_history[historyIndex] = srcPtr[forEachDimension];
					}

					historyIndex++;
				}

				// Update min/max in all blocks
				minMaxIndex = 0;

				for (int i = 0; i < sampleDimension; i++)
				{
					_minMax[minMaxIndex++] = Float.MAX_VALUE;
					_minMax[minMaxIndex++] = -Float.MAX_VALUE;
				}

				historyIndex = 0;

				for (int forEachBlock = 0; forEachBlock < options.numberOfBlocks.get(); forEachBlock++)
				{
					minMaxIndex = 0;

					for (int forEachDimension = 0; forEachDimension < sampleDimension; forEachDimension++)
					{
						if (_minMax[minMaxIndex] > _history[historyIndex])
						{
							_minMax[minMaxIndex] = _history[historyIndex];
						}

						minMaxIndex++;
						historyIndex++;

						if (_minMax[minMaxIndex] < _history[historyIndex])
						{
							_minMax[minMaxIndex] = _history[historyIndex];
						}

						minMaxIndex++;
						historyIndex++;
					}
				}

				// Write back min/max
				minMaxIndex = 0;
				dstIndex = 0;

				for (int forEachDimension = 0; forEachDimension < sampleDimension; forEachDimension++)
				{
					if (EnumSet.of(Format.MIN, Format.ALL).contains(options.format.get()))
					{
						dstPtr[dstIndex++] = _minMax[minMaxIndex];
					}

					minMaxIndex++;

					if (EnumSet.of(Format.MAX, Format.ALL).contains(options.format.get()))
					{
						dstPtr[dstIndex++] = _minMax[minMaxIndex];
					}

					minMaxIndex++;
				}
			}
		}

		@Override
		public void flush(Stream stream_in, Stream stream_out)
		{
			_history = null;
			_minMax = null;
		}
	}

	class Sliding implements Implementation
	{
		Options options;

		float[] _minHistory;
		float[] _maxHistory;

		float _alpha;
		float _1_alpha;

		boolean _firstCall;

		public Sliding(Options options)
		{
			this.options = options;
		}

		@Override
		public void enter(Stream stream_in, Stream stream_out)
		{
			int sampleDimension = stream_in.dim;
			double sampleRate = stream_in.sr;

			// Allocate history arrays
			_minHistory = new float[sampleDimension];
			_maxHistory = new float[sampleDimension];

			// Allocate and initialize alpha array
			_alpha = (float) (1.0 - (2.0 * Math.sqrt(3.0)) / (options.windowSize.get() * sampleRate));
			_1_alpha = 1 - _alpha;

			// Set first call to true
			_firstCall = true;
		}

		@Override
		public void transform(Stream stream_in, Stream stream_out)
		{
			int sampleDimension = stream_in.dim;
			int sampleNumber = stream_in.num;

			float[] srcPtr = stream_in.ptrF();
			float[] dstPtr = stream_out.ptrF();

			float x;
			float minVal;
			float maxVal;

			int srcIndex = 0;
			int dstIndex = 0;

			// Initialize history array
			if (_firstCall)
			{
				for (int i = 0; i < sampleDimension; i++)
				{
					_minHistory[i] = srcPtr[i];
					_maxHistory[i] = srcPtr[i];
				}

				_firstCall = false;
			}

			for (int i = 0; i < sampleNumber; i++)
			{
				for (int j = 0; j < sampleDimension; j++)
				{
					x = srcPtr[srcIndex++];

					if (EnumSet.of(Format.MIN, Format.ALL).contains(options.format.get()))
					{
						minVal = _minHistory[j];
						minVal = Math.min(x, _alpha * minVal + _1_alpha * x);

						_minHistory[j] = minVal;
						dstPtr[dstIndex] = minVal;
					}


					if (EnumSet.of(Format.MAX, Format.ALL).contains(options.format.get()))
					{
						maxVal = _maxHistory[j];
						maxVal = Math.max(x, _alpha * maxVal + _1_alpha * x);

						_maxHistory[j] = maxVal;
						dstPtr[dstIndex] = maxVal;
					}
				}
			}
		}

		@Override
		public void flush(Stream stream_in, Stream stream_out)
		{
			_minHistory = null;
			_maxHistory = null;
		}
	}
}
