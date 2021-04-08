/*
 * FFMPEGReaderChannel.java
 * Copyright (c) 2018
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

package hcm.ssj.ffmpeg;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 04.09.2017.
 */

public class FFMPEGReaderChannel extends SensorChannel
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
	 * All options for the provider
	 */
	public class Options extends OptionList
	{
		public final Option<Double> sampleRate = new Option<>("sampleRate", 15., Double.class, "sample rate");

		/**
		 *
		 */
		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private int sampleDimension = 0;
	private FFMPEGReader ffmpegReader = null;

	public FFMPEGReaderChannel()
	{
		super();
		_name = this.getClass().getSimpleName();
	}

	@Override
	protected void init() throws SSJException
	{
		ffmpegReader =  (FFMPEGReader) _sensor;
		sampleDimension = ffmpegReader.getBufferSize();
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		byte[] out = stream_out.ptrB();

		ffmpegReader.swapBuffer(out);

		return true;
	}

	/**
	 * @return double
	 */
	@Override
	public double getSampleRate()
	{
		return options.sampleRate.get();
	}

	/**
	 * @return int
	 */
	@Override
	final public int getSampleDimension()
	{
		return sampleDimension;
	}

	@Override
	protected Cons.Type getSampleType()
	{
		return Cons.Type.IMAGE;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[]{"video"};

		((ImageStream)_stream_out).width = ffmpegReader.options.width.get();
		((ImageStream)_stream_out).height = ffmpegReader.options.height.get();
		((ImageStream)_stream_out).format = Cons.ImageFormat.FLEX_RGB_888.val;
	}
}
