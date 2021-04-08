/*
 * MobileSSIConsumer.java
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

package hcm.ssj.mobileSSI;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

import static java.lang.System.loadLibrary;

/**
 * File writer for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class MobileSSIConsumer extends Consumer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     *
     */
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

    public MobileSSIConsumer()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * @param stream_in Stream[]
	 */
    @Override
    public final void enter(Stream[] stream_in) throws SSJFatalException
    {
        if (stream_in.length > 1 || stream_in.length < 1)
        {
            Log.e("stream count not supported");
            return;
        }
        if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
        {
            Log.e("stream type not supported");
            return;
        }
        start(stream_in[0]);
    }

    /**
     * @param stream Stream
     */
    private void start(Stream stream)
    {

    }

    /**
     * @param stream_in Stream[]
	 * @param trigger event trigger
     */
    @Override
    protected final void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        float[] floats = stream_in[0].ptrF();
            stream_in[0].ptrF()[0]=0.5f;
        //ssi_ssj_sensor
        pushData(stream_in[0], getId());
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[]) throws SSJFatalException
    {

    }

    @Override
	public final void init(Stream stream_in[]) throws SSJException
    {
        int t=0;
		if (stream_in[0].type == Cons.Type.FLOAT)
		{
			t = 9;
		}
        setStreamOptions(getId(), stream_in[0].num, stream_in[0].dim, t, stream_in[0].sr);
    }


    public void setId(int _id)
    {
        id=_id;
    }
    public int getId()
    {return id;}

static
{
    loadLibrary("ssissjSensor");
}
    public native void setSensor(Object sensor, Stream[] stream, int id);
    public native void pushData(Stream stream, int id);
    public native void setStreamOptions(int id, int num, int dim, int type, double sr);

    private int id=0;



}
