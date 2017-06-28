/*
 * ImageStream.java
 * Copyright (c) 2017
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

import android.graphics.ImageFormat;
import android.media.Image;

import hcm.ssj.core.Cons;

/**
 * Created by Johnny on 11.06.2015.
 */
public class ImageStream extends ByteStream
{
    private int width;
    private int height;
    private int format;

    public ImageStream(int num, int dim, double sr)
    {
        super(num, dim, sr);
        this.type = Cons.Type.IMAGE;
        this.width = 0;
        this.height = 0;
        this.format = ImageFormat.NV21;
    }

    public ImageStream(int num, int dim, double sr, int width, int height, int format)
    {
        super(num, dim, sr);
        this.type = Cons.Type.IMAGE;
        this.width = width;
        this.height = height;
        this.format = format;
    }

    public ImageStream select(int[] new_dims)
    {
        if(dim == new_dims.length)
            return this;

        ImageStream slice = new ImageStream(num, new_dims.length, sr, width, height, format);
        slice.source = source;

        byte[] src = this.ptr();
        byte[] dst = slice.ptr();
        int srcPos = 0, dstPos = 0;
        while(srcPos < num * dim)
        {
            for(int i = 0; i < new_dims.length; i++)
                dst[dstPos++] = src[srcPos + new_dims[i]];

            srcPos += dim;
        }

        return slice;
    }

    public ImageStream select(int new_dim)
    {
        if(dim == 1)
            return this;

        ImageStream slice = new ImageStream(num, 1, sr, width, height, format);
        slice.source = source;

        byte[] src = this.ptr();
        byte[] dst = slice.ptr();
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
        ImageStream copy = new ImageStream(num, dim, sr, width, height, format);
        System.arraycopy(_ptr, 0, copy._ptr, 0, _ptr.length);

        return copy;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public void setFormat(int format)
    {
        this.format = format;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getFormat()
    {
        return format;
    }
}
