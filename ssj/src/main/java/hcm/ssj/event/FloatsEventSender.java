/*
 * FloatsEventSender.java
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

package hcm.ssj.event;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Event;
import hcm.ssj.core.stream.Stream;

public class FloatsEventSender extends Consumer
{
    public class Options
    {
        public String sender;
        public String event = "event";

        public boolean mean = true; //send mean values
    }
    public Options options = new Options();

    public FloatsEventSender()
    {
        _name = "SSJ_consumer_FloatsEventSender";
        options.sender = _name;
    }

    @Override
    public void enter(Stream[] stream_in)
    {
        if (stream_in[0].type != Cons.Type.FLOAT) {
            throw new RuntimeException("type "+ stream_in[0].type +" not supported");
        }
    }

    @Override
    protected void consume(Stream[] stream_in)
    {
        float ptr[] = stream_in[0].ptrF();

        Event ev = new Event();
        ev.name = options.event;
        ev.sender = options.sender;
        ev.time = (int)(1000 * stream_in[0].time + 0.5);
        ev.dur = (int)(1000 * (stream_in[0].num / stream_in[0].sr) + 0.5);
        ev.state = Event.State.COMPLETED;

        ev.msg = "";
        if (options.mean) {
            float sum;
            for (int j = 0; j < stream_in[0].dim; j++)
            {
                sum = 0;
                for (int i = 0; i < stream_in[0].num; i++)
                    sum += ptr[i * stream_in[0].dim + j];

                ev.msg += String.valueOf(sum / stream_in[0].num) + " ";
            }
        }
        else {
            for (int i = 0; i < stream_in[0].num; i++) {
                for (int j = 0; j < stream_in[0].dim; j++)
                    ev.msg += String.valueOf(ptr[i * stream_in[0].dim + j]) + " ";
            }
        }

        _evchannel_out.pushEvent(ev);
    }

    @Override
    public void flush(Stream[] stream_in)
    {}
}
