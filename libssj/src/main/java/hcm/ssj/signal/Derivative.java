/*
 * Derivative.java
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
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer to calculate derivative of signal data.<br>
 * Created by Ionut Damian on 01.02.2017. Based on Derivative plugin from SSI.
 */
public class Derivative extends Transformer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<Boolean> zero = new Option<>("zero", true, Boolean.class, "pass along the untransformed stream");
        public final Option<Boolean> first = new Option<>("first", true, Boolean.class, "compute first derivative");
        public final Option<Boolean> second = new Option<>("second", true, Boolean.class, "compute second derivative");
        public final Option<Boolean> third = new Option<>("third", true, Boolean.class, "compute third derivative");
        public final Option<Boolean> fourth = new Option<>("fourth", true, Boolean.class, "compute fourth derivative");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    private float[] history;
    private int depth = 0;
    private boolean first_call;

    private boolean[] store_value = new boolean[5];

    /**
     *
     */
    public Derivative()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in  Stream[]
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        if (options.fourth.get()) store_value[4] = true;
        if (options.third.get()) store_value[3] = true;
        if (options.second.get()) store_value[2] = true;
        if (options.first.get()) store_value[1] = true;
        if (options.zero.get()) store_value[0] = true;

		for (int i = 0; i <= 4; i++)
		{
			if (store_value[i]) depth = i;
		}
        depth++;

        history = new float[(depth - 1) * stream_in[0].dim];
        first_call = true;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        int sample_dimension = stream_in[0].dim;
        int sample_number = stream_in[0].num;

        float src[] = stream_in[0].ptrF();
        float dst[] = stream_out.ptrF();
        float tmp, tmp2;

        // initialize history during first call
        if (first_call) {
            for (int i = 0; i < sample_dimension; ++i) {
                for (int j = 0; j < depth-1; j++) {
                    history[i*(depth-1) + j] = j == 0 ? src[i] : 0;
                }
            }
            first_call = false;
        }

        // calculate derivative
        int srccnt = 0;
        int dstcnt = 0;
        for (int i = 0; i < sample_number; i++) {
            int histcnt = 0;
            for (int j = 0; j < sample_dimension; j++) {
                tmp = src[srccnt++];
                if (store_value[0]) {
                    dst[dstcnt++] = tmp;
                }
                for (int k = 1; k < depth; k++) {
                    tmp2 = tmp;
                    tmp -= history[histcnt];
                    history[histcnt++] = tmp2;
                    if (store_value[k]) {
                        dst[dstcnt++] = tmp;
                    }
                }
            }
        }
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        history = null;
        store_value = null;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int dim = 0;

        if (options.fourth.get()) dim++;
        if (options.third.get()) dim++;
        if (options.second.get()) dim++;
        if (options.first.get()) dim++;
        if (options.zero.get()) dim++;

        return dim * stream_in[0].dim;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return sampleNumber_in;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];

        for (int i = 0; i < stream_in[0].dim; i++)
        {
            int j = 0;
            if (options.zero.get()) stream_out.desc[j++] = stream_in[0].desc[i];
            if (options.first.get()) stream_out.desc[j] = stream_in[0].desc[i] + ".d" + j++;
            if (options.second.get()) stream_out.desc[j] = stream_in[0].desc[i] + ".d" + j++;
            if (options.third.get()) stream_out.desc[j] = stream_in[0].desc[i] + ".d" + j++;
            if (options.fourth.get()) stream_out.desc[j] = stream_in[0].desc[i] + ".d" + j;
        }
    }
}
