/*
 * Spectrogram.java
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

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

import static hcm.ssj.signal.FilterTools.WINDOW_TYPE;
import static hcm.ssj.signal.Matrix.MATRIX_DIMENSION;

/**
 * Created by Johnny on 07.04.2017.
 * Computes spectrogram of input stream
 *
 * Code adapted from SSI's Spectrogram.cpp
 */

public class Spectrogram extends Transformer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
	{
		public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");

		public final Option<Integer> nfft = new Option<>("nbanks", 512, Integer.class, "#fft coefficients");
		public final Option<Integer> nbanks = new Option<>("nbanks", 2, Integer.class, "#filter banks");
//		public final Option<Double> minfreq = new Option<> ("minfreq", 0.0, Double.class, "mininmum frequency");
//		public final Option<Double> maxfreq = new Option<> ("maxfreq", 0.0, Double.class, "maximum frequency (nyquist if 0)");
		public final Option<FilterTools.WINDOW_TYPE> wintype = new Option<> ("wintype", FilterTools.WINDOW_TYPE.HAMMING, FilterTools.WINDOW_TYPE.class, "window type");
		public final Option<Boolean> dolog = new Option<> ("dolog", true, Boolean.class, "apply logarithm");
		public final Option<Boolean> dopower = new Option<>("dopower", false, Boolean.class, "compute the PSD for every bank");
		public final Option<String> banks = new Option<>("banks", "0.040 0.150, 0.150 0.400", String.class, "string with filter banks that gets applied if no file was set (example: \"0.003 0.040\n0.040 0.150\n0.150 0.400\").");
		/**
		 *
		 */
		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private int _fft_size = 0;
	private int _rfft;
	private FilterTools.WINDOW_TYPE _win_type = FilterTools.WINDOW_TYPE.HAMMING;
	private int _win_size = 0;
	private Matrix<Float> _filterbank = null;
	private FloatFFT_1D _fft = null;
	private Matrix<Float> _fftmag = null;
	private Matrix<Float> _window = null;
	private boolean _apply_log = false;

	Matrix<Float> _matrix_in;
	Matrix<Float> _matrix_out;
	float _data_in[];
	float _data_out[];

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (_filterbank == null) {
			if (options.banks.get() != null)
			{
				readFilterbank(options.banks.get(), stream_in[0].sr);
			}
			else
			{
				Log.e("frequency banks not set");
			}
		}

		if (stream_in[0].num > options.nfft.get())
		{
			Log.w("nfft too small (" + options.nfft.get() + ") for input stream (num=" + stream_in[0].num + "), extra samples will get ignored");
		}

		_matrix_in = new Matrix<>(stream_in[0].num, 1);
		_matrix_out = new Matrix<>(1, _filterbank.getCols());
		_data_in = new float[_fft_size];
		Arrays.fill(_data_in, 0);
		_data_out = new float[_rfft];
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		for(int i = 0; i < stream_in[0].num; i++)
		{
			switch (stream_in[0].type)
			{
				case FLOAT:
					_matrix_in.setData(i, stream_in[0].ptrF()[i]);
					break;
				case DOUBLE:
					_matrix_in.setData(i, (float)stream_in[0].ptrD()[i]);
					break;
			}
		}

		//apply window
		if (_win_size != _matrix_in.getRows()) {
			_win_size = _matrix_in.getRows();
			if (_win_type != WINDOW_TYPE.RECTANGLE) {
				_window = FilterTools.getInstance().Window (_win_size, _win_type, MATRIX_DIMENSION.COL);
			}
		}

		if (_win_type != WINDOW_TYPE.RECTANGLE)
		{
			MatrixOps.getInstance().mult(_matrix_in, _window);
		}

		//copy data from matrix for fft
		//if nfft to large, fill with zeroes
		for (int i = 0; i < _data_in.length && i < _matrix_in.getSize(); i++)
		{
			_data_in[i] = _matrix_in.getData(i);
		}
		for (int i = _matrix_in.getSize(); i < _data_in.length; i++)
		{
			_data_in[i] = 0;
		}

		// Calculate FFT
		_fft.realForward(_data_in);

		// Format values like in SSI
		Util.joinFFT(_data_in, _data_out);

		for (int i = 0; i < _data_out.length; ++i)
		{
			if (options.dopower.get())
			{
				_data_out[i] = (float) Math.pow(_data_out[i], 2) / _data_out.length;
			}

			_fftmag.setData(i, _data_out[i]);
		}

		MatrixOps.getInstance().multM (_fftmag, _filterbank, _matrix_out);

		//compute log
		if (_apply_log) {
			MatrixOps.getInstance().log10 (_matrix_out);
		}

		float output[] = stream_out.ptrF();
		for (int i = 0; i < _matrix_out.getSize(); i++)
		{
			output[i] = _matrix_out.getData(i);
		}
	}

	@Override
	public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		_win_size = 0;
	}

	private void readFilterbank (String string, double sr)
	{
		int n_banks = 0;
		Matrix<Float> intervals;

		String[] banks = string.split("\\s*,\\s*");
		n_banks = banks.length;

		if (options.nbanks.get() != n_banks) {
			Log.e("#banks ("+n_banks+") in string '"+string+"' differs from #banks ("+options.nbanks+") in options");
		}

		intervals = new Matrix<>(n_banks, 2);
		int current_bank = 0;

		for (String bank : banks){
			String[] freq = bank.split("\\s+");
			intervals.setData(current_bank * 2, Float.valueOf(freq[0]));
			intervals.setData(current_bank * 2 + 1, Float.valueOf(freq[1]));
			current_bank++;
		}

		Matrix<Float> filterbank = FilterTools.getInstance().Filterbank(options.nfft.get(), sr, intervals, options.wintype.get());
		setFilterbank(filterbank, options.wintype.get(), options.dolog.get());
	}

	private void setFilterbank (Matrix<Float> filterbank,	FilterTools.WINDOW_TYPE win_type, boolean apply_log)
	{
		_fft = null;
		_fftmag = null;
		_filterbank = null;

		_fft_size = (filterbank.getCols() - 1) << 1;
		_rfft = (_fft_size >> 1) + 1;
		_win_type = win_type;
		_apply_log = apply_log;

		_filterbank = filterbank;
		_filterbank.transpose();

		_fft = new FloatFFT_1D (_fft_size);
		_fftmag = new Matrix<> (1, _rfft);
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		if(stream_in[0].dim > 1)
			Log.e("dimension > 1 not supported");

		if (_filterbank != null) {
			return _filterbank.getCols();
		} else {
			return options.nbanks.get();
		}
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(Cons.Type.FLOAT);
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if(stream_in[0].type != Cons.Type.FLOAT && stream_in[0].type != Cons.Type.DOUBLE)
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
		stream_out.desc = new String[]{"spectrogram"};
	}
}
