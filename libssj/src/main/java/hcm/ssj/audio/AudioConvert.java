/*
 * AudioConvert.java
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

package hcm.ssj.audio;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Converts audio stream from float to short or short to float
 * Created by Johnny on 15.06.2015.
 */
public class AudioConvert extends Transformer {

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
    {
        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    public AudioConvert()
    {
        _name = "AudioConvert";
    }

    @Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        Stream audio = null;
        for(Stream s : stream_in) {
			if (s.findDataClass("Audio") >= 0)
			{
				audio = s;
			}
        }
        if(audio == null) {
            Log.w("invalid input stream");
            return;
        }
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        switch(stream_in[0].type)
        {
            case SHORT:
            {
                short[] data = stream_in[0].ptrS();
                float[] out = stream_out.ptrF();

                for (int i = 0; i < data.length; ++i)
                {
                    out[i] = data[i] / 32768.0f;
                }
            }
            break;
            case FLOAT:
            {
                float[] data = stream_in[0].ptrF();
                short[] out = stream_out.ptrS();

                for (int i = 0; i < data.length; ++i)
                {
                    out[i] = (short)(data[i] * 32768);
                }
            }
            break;
        }
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
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
        switch(stream_in[0].bytes)
        {
            case 4:
                return 2;
            case 2:
                return 4;
            default:
                Log.e("Unsupported input stream type");
                return 0;
        }
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        switch(stream_in[0].type)
        {
            case FLOAT:
                return Cons.Type.SHORT;
            case SHORT:
                return Cons.Type.FLOAT;
            default:
                Log.e("Unsupported input stream type");
                return Cons.Type.UNDEF;
        }
    }

    @Override
    public void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[stream_out.dim];

        for(int i = 0; i < stream_out.dim; ++i)
            stream_out.desc[i] = stream_in[0].desc[i];
    }
}
