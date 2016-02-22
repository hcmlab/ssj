/*
 * BluetoothProvider.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.ioput;

import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothProvider extends SensorProvider
{
    public class Options {
        public int bytes = 0;
        public int dim = 0;
        public double sr = 0;
        public int num = 1; //values >1 make buffer error handling less efficient
        public Cons.Type type = Cons.Type.UNDEF;
        public String[] outputClass = null;
    }

    public Options options = new Options();

    protected DataInputStream _in;
    private byte[] _data;

    public BluetoothProvider()
    {
        _name = "SSJ_consumer_BluetoothReader_Data";
    }

    @Override
    public void enter(Stream stream_out) {

        try
        {
            _in = new DataInputStream(((BluetoothReader)_sensor).conn.getSocket().getInputStream());
        }
        catch (IOException e)
        {
            Log.e(_name, "cannot connect to server", e);
        }

        if(options.sr == 0 || options.bytes == 0 || options.dim == 0 || options.type == Cons.Type.UNDEF)
            Log.e(_name, "input channel not configured");

        _data = new byte[stream_out.tot];
    }

    @Override
    protected void process(Stream stream_out)
    {
        try
        {
            //we check whether there is any data as reads are blocking for bluetooth
            if(_in.available() == 0)
                return;

            _in.readFully(_data);
            Util.arraycopy(_data, 0, stream_out.ptr(), 0, _data.length);
        }
        catch (IOException e)
        {
            Log.w(_name, "unable to read from data stream", e);
        }
    }

    @Override
    public int getSampleDimension()
    {
        return options.dim;
    }

    @Override
    public double getSampleRate()
    {
        return options.sr;
    }

    @Override
    public int getSampleBytes()
    {
        return options.bytes;
    }

    @Override
    public int getSampleNumber()
    {
        return options.num;
    }

    @Override
    public Cons.Type getSampleType()
    {
        return options.type;
    }

    @Override
    public void defineOutputClasses(Stream stream_out)
    {
        stream_out.dataclass = new String[stream_out.dim];
        if(options.outputClass == null || stream_out.dim != options.outputClass.length)
        {
            Log.w(_name, "incomplete definition of output classes");
            for(int i = 0; i < stream_out.dataclass.length; i++)
                stream_out.dataclass[i] = "BluetoothData";
        }
        else
        {
            System.arraycopy(options.outputClass, 0, stream_out.dataclass, 0, options.outputClass.length);
        }
    }
}
