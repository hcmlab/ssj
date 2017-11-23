/*
 * Stream.java
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

package hcm.ssj.core.stream;

import java.io.Serializable;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Provider;

/**
 * Created by Johnny on 17.03.2015.
 */
public abstract class Stream implements Serializable
{
    public int dim;
    public int num;
    public int num_frame;
    public int num_delta;
    public int bytes;
    public int tot;
    public double sr;
    public double time;
    public double step;
    public Cons.Type type;
    public transient Provider source;
    public String[] desc = null;

    public static Stream create(int num, int dim, double sr, Cons.Type type)
    {
        switch(type)
        {
            case BYTE:
                return new ByteStream(num, dim, sr);
            case CHAR:
                return new CharStream(num, dim, sr);
            case SHORT:
                return new ShortStream(num, dim, sr);
            case INT:
                return new IntStream(num, dim, sr);
            case LONG:
                return new LongStream(num, dim, sr);
            case FLOAT:
                return new FloatStream(num, dim, sr);
            case DOUBLE:
                return new DoubleStream(num, dim, sr);
            case BOOL:
                return new BoolStream(num, dim, sr);
            case IMAGE:
                return new ImageStream(num, dim, sr);
            default:
                throw new UnsupportedOperationException("Stream type not supported");
        }
    }

    public static Stream create(Provider source, int num_frame, int num_delta)
    {
        Stream s = create(source, num_frame + num_delta);
        s.num_frame = num_frame;
        s.num_delta = num_delta;
        return s;
    }

    public static Stream create(Provider source, int num)
    {
        Stream s;
        switch(source.getOutputStream().type)
        {
            case IMAGE:
                ImageStream src = (ImageStream) source.getOutputStream();
                s = new ImageStream(num, src.dim, src.sr, src.width, src.height, src.format);
                break;
            default:
                s = create(num, source.getOutputStream().dim, source.getOutputStream().sr, source.getOutputStream().type);
        }

        s.source = source;
        s.desc = source.getOutputDescription();
        return s;
    }

    protected Stream()
    {
        dim = 0;
        bytes = 0;
        type = Cons.Type.UNDEF;
        sr = 0;
        step = 0;
        num = 0;
        num_frame = 0;
        num_delta = 0;
        tot = 0;
        time = 0;
    }

    protected Stream(int num, int dim, double sr)
    {
        this.dim = dim;
        this.num = num;
        this.num_frame = num;
        this.sr = sr;
        this.step = 1.0 / sr;

        this.num_delta = 0;
        this.time = 0;
        this.tot = 0;
        this.bytes = 0;
        this.type = Cons.Type.UNDEF;
    }

    public void setSource(Provider source)
    {
        this.source = source;
        this.desc = source.getOutputDescription();
    }

    public abstract Object ptr();
    public byte[] ptrB() { throw new UnsupportedOperationException(); }
    public char[] ptrC() { throw new UnsupportedOperationException(); }
    public short[] ptrS() { throw new UnsupportedOperationException(); }
    public int[] ptrI() { throw new UnsupportedOperationException(); }
    public long[] ptrL() { throw new UnsupportedOperationException(); }
    public float[] ptrF() { throw new UnsupportedOperationException(); }
    public double[] ptrD() { throw new UnsupportedOperationException(); }
    public boolean[] ptrBool() { throw new UnsupportedOperationException(); }

    public abstract void adjust(int num);
    public abstract Stream select(int[] new_dims);
    public abstract Stream select(int new_dim);
    public abstract Stream clone();

    /**
     * Extracts a subgroup of the stream. WARNING: may initialize large amounts of memory
     * @param from time in seconds pointing to the start of the substream
     * @param to time in seconds pointing to the end of the substream
     * @return new stream object
     */
    public abstract Stream substream(double from, double to);

    public int findDataClass(String name)
    {
        if(desc == null || desc.length == 0)
            return -1;

        for(int i = 0; i < desc.length; i++)
            if(desc[i].equalsIgnoreCase(name))
                return i;

        return -1;
    }

    public void reset()
    {
        time = 0;
    }
}
