/*
 * OverallActivation.java
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

package hcm.ssj.body;

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Computes the expressivity feature Overall Activation as defined by Hartmann et al. 2005 and Baur et al. 2015
 * Created by Johnny on 05.03.2015.
 */
public class OverallActivation extends Transformer {

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

    public OverallActivation()
    {
        _name = "OverallActivation";
    }

    float _displacement[];

    @Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        Stream acc = null;
        for(Stream s : stream_in) {
			if (s.findDataClass("AccX") >= 0)
			{
				acc = s;
			}
        }
        if(acc == null || acc.dim != 3) {
            Log.w("non-standard input stream");
        }

        _displacement = new float[stream_in[0].dim];
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        float[] out = stream_out.ptrF();
        out[0] = computeOA(stream_in[0]);
    }

    public float computeOA(Stream stream)
    {
        float ptr[] = stream.ptrF();
        Arrays.fill(_displacement, 0);

		/*
		 * compute displacement for each dimension
		 */
        float a;
        for (int i = 0; i < stream.dim; ++i)
        {
            float velOld = 0;
            float velNew = 0;

            for (int j = 0; j < stream.num; ++j)
            {
                a = (Math.abs(ptr[j * stream.dim + i]) < 0.1) ? 0 : ptr[j * stream.dim + i];

                // v1 = a * t + v0
                velNew = (float) (a * stream.step) + velOld;
                if(velNew < 0) velNew = 0; //ignore negative velocities -> this can happen at the start of a frame

                // d = v0 * t + 0.5 * a * t²  or  d = v0 * t + 0.5 * (v1 - v0) * t
                _displacement[i] += velOld * stream.step + 0.5 * (velNew - velOld) * stream.step;

                // Update v0
                velOld = velNew;
            }
        }

		/*
		 * compute displacement by summing up the displacement of all dimensions
		 */
        float displacement = 0;
        for (int i = 0; i < stream.dim; ++i)
        {
            displacement += _displacement[i];
        }

        return displacement;
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {}

    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        return 1;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return 1;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        if(stream_in[0].bytes != 4)
            Log.e("Unsupported input stream type");

        return 4;
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        if(stream_in[0].type != Cons.Type.FLOAT)
            Log.e("Unsupported input stream type");

        return Cons.Type.FLOAT;
    }

    @Override
    public void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[stream_out.dim];
        stream_out.desc[0] = "Activation";
    }

}
