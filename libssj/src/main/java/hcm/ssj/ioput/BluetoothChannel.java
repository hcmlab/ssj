/*
 * BluetoothChannel.java
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

package hcm.ssj.ioput;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothChannel extends SensorChannel
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
    {
        public final Option<Integer> channel_id = new Option<>("channel_id", 0, Integer.class, "the channel index as defined by the order in which the streams were sent");
        public final Option<Integer> bytes = new Option<>("bytes", 0, Integer.class, "");
        public final Option<Integer> dim = new Option<>("dim", 0, Integer.class, "");
        public final Option<Double> sr = new Option<>("sr", 0., Double.class, "");
        public final Option<Integer> num = new Option<>("num", 1, Integer.class, "values >1 make buffer error handling less efficient");
        public final Option<Cons.Type> type = new Option<>("type", Cons.Type.UNDEF, Cons.Type.class, "");
        public final Option<Integer> imageWidth = new Option<>("imageWidth", 0, Integer.class, "image width in case of image stream");
        public final Option<Integer> imageHeight = new Option<>("imageHeight", 0, Integer.class, "image height in case of image stream");
        public final Option<Cons.ImageFormat> imageFormat = new Option<>("imageFormat", Cons.ImageFormat.NV21, Cons.ImageFormat.class, "color format in case of image stream");
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();

    public BluetoothChannel()
    {
        _name = "BluetoothReader_Data";
    }

    @Override
	public void enter(Stream stream_out) throws SSJFatalException
	{

		if (options.sr.get() == 0 || options.bytes.get() == 0 || options.dim.get() == 0 || options.type.get() == Cons.Type.UNDEF)
		{
            throw new SSJFatalException("input channel not configured");
		}
    }

    @Override
    protected boolean process(Stream stream_out) throws SSJFatalException
    {
        if (!((BluetoothReader) _sensor).getConnection().isConnected())
        {
            return false;
        }

        byte[] data = ((BluetoothReader)_sensor).getData(options.channel_id.get());

        if(data == null || data.length != stream_out.tot)
        {
            Log.w("data mismatch");
            return false;
        }

        Util.arraycopy(data, 0, stream_out.ptr(), 0, stream_out.tot);
        return true;
    }

    @Override
    public int getSampleDimension()
    {
        return options.dim.get();
    }

    @Override
    public double getSampleRate()
    {
        return options.sr.get();
    }

    @Override
    public int getSampleBytes()
    {
        return options.bytes.get();
    }

    @Override
    public int getSampleNumber()
    {
        return options.num.get();
    }

    @Override
    public Cons.Type getSampleType()
    {
        return options.type.get();
    }

    @Override
    public void describeOutput(Stream stream_out)
    {
        stream_out.desc = new String[stream_out.dim];
        if(options.outputClass.get() == null || stream_out.dim != options.outputClass.get().length)
        {
            Log.w("incomplete definition of output classes");
            for(int i = 0; i < stream_out.desc.length; i++)
                stream_out.desc[i] = "BluetoothData" + options.channel_id.get();
        }
        else
        {
            System.arraycopy(options.outputClass.get(), 0, stream_out.desc, 0, options.outputClass.get().length);
        }

        if(getSampleType() == Cons.Type.IMAGE)
        {
            ((ImageStream) _stream_out).width = options.imageWidth.get();
            ((ImageStream) _stream_out).height = options.imageHeight.get();
            ((ImageStream) stream_out).format = options.imageFormat.get().val;
        }
    }
}
