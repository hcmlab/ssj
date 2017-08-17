/*
 * MvgAvgVar.java
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

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Provides moving/sliding average and/or variance.
 *
 * Moving average: https://en.wikipedia.org/wiki/Moving_average#Simple_moving_average
 * Sliding average: https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average
 */
public class MvgAvgVar extends Transformer
{
	public enum Method
	{
		MOVING,
		SLIDING
	}

	public enum Format
	{
		AVERAGE,
		VARIANCE,
		AVG_AND_VAR
	}

	public class Options extends OptionList
	{
		public final Option<Double> window = new Option<>("window", 10., Double.class, "size of moving/sliding window in seconds");
		public final Option<Method> method = new Option<>("method", Method.MOVING, Method.class, "");
		public final Option<Format> format = new Option<>("format", Format.AVERAGE, Format.class, "");

		/**
		 *
		 */
		private Options() {
			addOptions();
		}
	}
	public final Options options = new Options();

	Implementation _impl;

	public MvgAvgVar()
	{
		_name = "MvgAvgVar";
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
		if (options.format.get() == Format.AVG_AND_VAR)
		{
			return stream_in[0].dim * 2;
		}

		return stream_in[0].dim;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return 4; //float
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
	public void describeOutput(Stream[] stream_in, Stream stream_out)
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

		int     _window_size_N;
		boolean _first_call;

		int   _history_size;
		int   _counter;
		float _history[];
		int   _hist_iter_0;
		int   _hist_iter_N;
		float _cumsum[];
		float _cumsum_2[];

		public Moving(Options options)
		{
			this.options = options;
		}

		@Override
		public void enter(Stream stream_in, Stream stream_out)
		{
			// calculate history size
			_window_size_N = (int) (options.window.get() * stream_in.sr + 0.5);
			_history_size = _window_size_N + 1;

			// allocate history array
			_history = new float[_history_size * stream_in.dim];
			_counter = _history_size;
			_hist_iter_0 = _history_size - 1;
			_hist_iter_N = 0;
			_cumsum = new float[stream_in.dim];
			_cumsum_2 = new float[stream_in.dim];

			// set first call to true
			_first_call = true;
		}

		@Override
		public void transform(Stream stream_in, Stream stream_out)
		{
			int sample_dimension = stream_in.dim;
			int sample_number = stream_in.num;

			float srcptr[] = stream_in.ptrF();
			float dstptr[] = stream_out.ptrF();

			int hist_iter, src_iter = 0, dst_iter = 0;
			float x_0, x_N, sum, sum_2;
			float var;

			// initialize history array
			if (_first_call)
			{
				hist_iter = 0;
				for (int i = 0; i < _history_size; ++i)
				{
					for (int j = 0; j < sample_dimension; ++j)
					{
						_history[hist_iter++] = srcptr[j];
					}
				}
				for (int j = 0; j < sample_dimension; ++j)
				{
					_cumsum[j] = _window_size_N * srcptr[j];
					_cumsum_2[j] = _window_size_N * srcptr[j] * srcptr[j];
				}
				_first_call = false;
			}

			for (int i = 0; i < sample_number; ++i)
			{

				// increment history;
				++_counter;
				_hist_iter_N += sample_dimension;
				_hist_iter_0 += sample_dimension;
				if (_counter > _history_size)
				{
					_counter = 1;
					_hist_iter_0 = 0;
				}
				if (_counter == _history_size)
				{
					_hist_iter_N = 0;
				}

				for (int j = 0; j < sample_dimension; ++j)
				{

					x_0 = srcptr[src_iter++];
					x_N = _history[_hist_iter_N + j];
					sum = _cumsum[j];
					sum_2 = _cumsum_2[j];

					// insert new sample
					_history[_hist_iter_0 + j] = x_0;

					// update sum
					sum = sum - x_N + x_0;
					sum_2 = sum_2 - x_N * x_N + x_0 * x_0;

					// calculate avg and var
					if (options.format.get() == Format.AVERAGE || options.format.get() == Format.AVG_AND_VAR)
					{
						dstptr[dst_iter++] = sum / _window_size_N;
					}
					if (options.format.get() == Format.VARIANCE || options.format.get() == Format.AVG_AND_VAR)
					{
						var = (_window_size_N * sum_2 - sum * sum) / (_window_size_N * (_window_size_N - 1));
						dstptr[dst_iter++] = var > 0 ? var : Float.MIN_VALUE;
					}

					_cumsum[j] = sum;
					_cumsum_2[j] = sum_2;
				}
			}
		}

		@Override
		public void flush(Stream stream_in, Stream stream_out)
		{

		}
	}

	class Sliding implements Implementation
	{
		Options options;

		float _alpha   = 0;
		float _1_alpha = 0;
		float _avg_hist[];
		float _var_hist[];
		boolean _first_call = true;

		public Sliding(Options options)
		{
			this.options = options;
		}

		@Override
		public void enter(Stream stream_in, Stream stream_out)
		{
			int sample_dimension = stream_in.dim;
			double sample_rate = stream_in.sr;

			// allocate history arrays
			_avg_hist = new float[sample_dimension];
			_var_hist = new float[sample_dimension];

			// allocate and initialize alpha array
			_alpha = (float) (1.0 - (2.0 * Math.sqrt(3.0)) / (options.window.get() * sample_rate));
			_1_alpha = 1 - _alpha;

			// set first call to true
			_first_call = true;
		}

		@Override
		public void transform(Stream stream_in, Stream stream_out)
		{
			int sample_dimension = stream_in.dim;
			int sample_number = stream_in.num;

			float srcptr[] = stream_in.ptrF();
			float dstptr[] = stream_out.ptrF();

			int avg_iter, var_iter, src_iter = 0, dst_iter = 0;
			float x, x_avg, avg, var;

			// initialize history array
			if (_first_call)
			{
				avg_iter = 0;
				var_iter = 0;
				for (int i = 0; i < sample_dimension; ++i)
				{
					_avg_hist[avg_iter++] = srcptr[i];
					_var_hist[var_iter++] = 0;
				}
				_first_call = false;
			}

			// do transformation
			switch (options.format.get())
			{

				case AVERAGE:
				{

					for (int i = 0; i < sample_number; ++i)
					{

						avg_iter = 0;

						for (int j = 0; j < sample_dimension; ++j)
						{

							x = srcptr[src_iter++];

							avg = _avg_hist[avg_iter];
							avg = _alpha * avg + _1_alpha * x;

							_avg_hist[avg_iter++] = avg;
							dstptr[dst_iter++] = avg;
						}
					}

					break;
				}

				case VARIANCE:
				case AVG_AND_VAR:
				{

					boolean store_all = options.format.get() == Format.AVG_AND_VAR;

					for (int i = 0; i < sample_number; ++i)
					{

						avg_iter = 0;
						var_iter = 0;

						for (int j = 0; j < sample_dimension; ++j)
						{

							x = srcptr[src_iter++];

							avg = _avg_hist[avg_iter];
							var = _var_hist[var_iter];

							avg = _alpha * avg + _1_alpha * x;
							x_avg = x - avg;
							var = _alpha * var + _1_alpha * x_avg * x_avg;
							var = var > 0 ? var : Float.MIN_VALUE;

							_avg_hist[avg_iter++] = avg;
							_var_hist[var_iter++] = var;

							if (store_all)
							{
								dstptr[dst_iter++] = avg;
							}

							dstptr[dst_iter++] = var;
						}
					}

					break;
				}
			}
		}

		@Override
		public void flush(Stream stream_in, Stream stream_out)
		{

		}
	}
}
