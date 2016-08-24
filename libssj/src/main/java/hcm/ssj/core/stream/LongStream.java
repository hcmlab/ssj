/*
 * LongStream.java
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

package hcm.ssj.core.stream;

import hcm.ssj.core.Cons;

/**
 * Created by Johnny on 11.06.2015.
 */
public class LongStream extends Stream
{
    private long[] _ptr;

    public LongStream(int num, int dim, double sr)
    {
        this.num = num;
        this.dim = dim;
        this.bytes = 8;
        this.type = Cons.Type.LONG;
        this.sr = sr;
        this.step = 1.0 / sr;

        tot = num * dim * bytes;
        _ptr = new long[num * dim];
    }

    @Override
    public long[] ptr()
{
    return _ptr;
}
    public long[] ptrL()
    {
        return _ptr;
    }

    public void adjust(int num)
    {
        if(num < this.num)
        {
            this.num = num;
            this.tot = num * dim * bytes;
        }
        else
        {
            this.num = num;
            this.tot = num * dim * bytes;
            _ptr = new long[num * dim];
        }
    }

    public LongStream select(int[] new_dims)
    {
        if(dim == new_dims.length)
            return this;

        LongStream slice = new LongStream(num, new_dims.length, sr);
        slice.source = source;

        long[] src = this.ptr();
        long[] dst = slice.ptr();
        int srcPos = 0, dstPos = 0;
        while(srcPos < num * dim)
        {
            for(int i = 0; i < new_dims.length; i++)
                dst[dstPos++] = src[srcPos + new_dims[i]];

            srcPos += dim;
        }

        return slice;
    }

    public LongStream select(int new_dim)
    {
        if(dim == 1)
            return this;

        LongStream slice = new LongStream(num, 1, sr);
        slice.source = source;

        long[] src = this.ptr();
        long[] dst = slice.ptr();
        int srcPos = 0, dstPos = 0;
        while(srcPos < num * dim)
        {
            dst[dstPos++] = src[srcPos + new_dim];
            srcPos += dim;
        }

        return slice;
    }

    @Override
    public Stream clone()
    {
        LongStream copy = new LongStream(num, dim, sr);
        System.arraycopy(_ptr, 0, copy._ptr, 0, _ptr.length);

        return copy;
    }
}
