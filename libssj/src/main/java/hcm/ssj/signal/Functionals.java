/*
 * AvgVar.java
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
 * A variety of metrics. <br>
 * Created by Frank Gaibler on 10.01.2017.
 */
public class Functionals extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");
        public final Option<Boolean> mean = new Option<>("mean", true, Boolean.class, "Calculate mean of each frame");
        public final Option<Boolean> energy = new Option<>("energy", true, Boolean.class, "Calculate energy of each frame");
        public final Option<Boolean> std = new Option<>("std", true, Boolean.class, "Calculate standard deviation of each frame");
        public final Option<Boolean> min = new Option<>("min", true, Boolean.class, "Calculate minimum of each frame");
        public final Option<Boolean> max = new Option<>("max", true, Boolean.class, "Calculate maximum of each frame");
        public final Option<Boolean> range = new Option<>("range", true, Boolean.class, "Calculate range of each frame");
        public final Option<Boolean> minPos = new Option<>("minPos", true, Boolean.class, "Calculate sample position of the minimum of each frame");
        public final Option<Boolean> maxPos = new Option<>("maxPos", true, Boolean.class, "Calculate sample position of the maximum of each frame");
        public final Option<Boolean> zeros = new Option<>("zeros", true, Boolean.class, "Calculate zeros of each frame");
        public final Option<Boolean> peaks = new Option<>("peaks", true, Boolean.class, "Calculate peaks of each frame");
        public final Option<Boolean> len = new Option<>("len", true, Boolean.class, "Calculate length of each frame");
        public final Option<Integer> delta = new Option<>("delta", 2, Integer.class, "zero/peaks search offset");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    //helper variables
    private int _delta;
    private float[] _mean_val;
    private float[] _energy_val;
    private float[] _std_val;
    private float[] _min_val;
    private float[] _max_val;
    private int[] _min_pos;
    private int[] _max_pos;
    private int[] _zeros;
    private int[] _peaks;
    private float[] _left_val;
    private float[] _mid_val;

    /**
     *
     */
    public Functionals()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        if (stream_in.length != 1 || stream_in[0].dim < 1 || stream_in[0].type != Cons.Type.FLOAT)
        {
            Log.e("invalid input stream");
            return;
        }
        _delta = options.delta.get();
        int sample_dimension = stream_in[0].dim;
        _mean_val = new float[sample_dimension];
        _energy_val = new float[sample_dimension];
        _std_val = new float[sample_dimension];
        _min_val = new float[sample_dimension];
        _max_val = new float[sample_dimension];
        _min_pos = new int[sample_dimension];
        _max_pos = new int[sample_dimension];
        _zeros = new int[sample_dimension];
        _peaks = new int[sample_dimension];
        _left_val = new float[sample_dimension];
        _mid_val = new float[sample_dimension];
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void flush(Stream[] stream_in, Stream stream_out)
    {
        super.flush(stream_in, stream_out);
        _mean_val = null;
        _energy_val = null;
        _std_val = null;
        _min_val = null;
        _max_val = null;
        _min_pos = null;
        _max_pos = null;
        _zeros = null;
        _peaks = null;
        _left_val = null;
        _mid_val = null;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        boolean first_call = true;
        int sample_number = stream_in[0].num, sample_dimension = stream_in[0].dim, c_in = 0, c_out = 0;
        float[] ptr_in = stream_in[0].ptrF(), ptr_out = stream_out.ptrF();
        float _val;
        for (int i = 0; i < sample_dimension; i++)
        {
            _val = ptr_in[c_in++];
            _mean_val[i] = _val;
            _energy_val[i] = _val * _val;
            _min_val[i] = _val;
            _max_val[i] = _val;
            _min_pos[i] = 0;
            _max_pos[i] = 0;
            _zeros[i] = 0;
            _peaks[i] = 0;
            _mid_val[i] = _val;
        }
        for (int i = 1; i < sample_number; i++)
        {
            for (int j = 0; j < sample_dimension; j++)
            {
                _val = ptr_in[c_in++];
                _mean_val[j] += _val;
                _energy_val[j] += _val * _val;
                if (_val < _min_val[j])
                {
                    _min_val[j] = _val;
                    _min_pos[j] = i;
                } else if (_val > _max_val[j])
                {
                    _max_val[j] = _val;
                    _max_pos[j] = i;
                }
                if ((i % _delta) == 0)
                {
                    if (first_call)
                    {
                        first_call = false;
                    } else
                    {
                        if ((_left_val[j] > 0 && _mid_val[j] < 0) || (_left_val[j] < 0 && _mid_val[j] > 0))
                        {
                            _zeros[j]++;
                        }
                        if (_left_val[j] < _mid_val[j] && _mid_val[j] > _val)
                        {
                            _peaks[j]++;
                        }
                    }
                    _left_val[j] = _mid_val[j];
                    _mid_val[j] = _val;
                }
            }
        }
        for (int i = 0; i < sample_dimension; i++)
        {
            _mean_val[i] /= sample_number;
            _energy_val[i] /= sample_number;
            _std_val[i] = (float) Math.sqrt(Math.abs(_energy_val[i] - _mean_val[i] * _mean_val[i]));
        }
        if (options.mean.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _mean_val[i];
            }
        }
        if (options.energy.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _energy_val[i];
            }
        }
        if (options.std.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _std_val[i];
            }
        }
        if (options.min.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _min_val[i];
            }
        }
        if (options.max.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _max_val[i];
            }
        }
        if (options.range.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = _max_val[i] - _min_val[i];
            }
        }
        if (options.minPos.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = (float) (_min_pos[i]) / sample_number;
            }
        }
        if (options.maxPos.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = (float) (_max_pos[i]) / sample_number;
            }
        }
        if (options.zeros.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = (float) (_zeros[i]) / sample_number;
            }
        }
        if (options.peaks.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = (float) (_peaks[i]) / sample_number;
            }
        }
        if (options.len.get())
        {
            for (int i = 0; i < sample_dimension; i++)
            {
                ptr_out[c_out++] = (float) sample_number;
            }
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int dim = 0;
        if (options.mean.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.energy.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.std.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.min.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.max.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.range.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.minPos.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.maxPos.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.zeros.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.peaks.get())
        {
            dim += stream_in[0].dim;
        }
        if (options.len.get())
        {
            dim += stream_in[0].dim;
        }
        return dim;
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
        return 1;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.dataclass = new String[overallDimension];
        if (options.outputClass.get() != null && overallDimension == options.outputClass.get().length)
        {
            System.arraycopy(options.outputClass.get(), 0, stream_out.dataclass, 0, options.outputClass.get().length);
        } else
        {
            if (options.outputClass.get() != null && overallDimension != options.outputClass.get().length)
            {
                Log.w("invalid option outputClass length");
            }
            for (int i = 0; i < overallDimension; i++)
            {
                stream_out.dataclass[i] = "fun" + i;
            }
        }
    }
}