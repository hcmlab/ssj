/*
 * EventLogger.java
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

package hcm.ssj.test;

import android.util.Log;

import hcm.ssj.core.Event;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.EventHandler;

/**
 * Outputs all incoming events using logcat
 */
public class EventLogger extends EventHandler
{
    public class Options
    {
    }
    public Options options = new Options();

    public EventLogger()
    {
        _name = "SSJ_econsumer_EventLogger";
    }

    @Override
    public void enter()
    {
        if(_evchannel_in == null || _evchannel_in.size() == 0)
            throw new RuntimeException("no input channels");
    }

    @Override
    protected void process()
    {
        for(EventChannel ch : _evchannel_in)
        {
            Event ev = ch.getLastEvent(false, true);
            if (ev == null)
                return;

            Log.i(_name, ev.sender + "_" + ev.name + "_" + ev.id + " (" + ev.state.toString() + ", " + ev.time + ", " + ev.dur + ") : " + ev.msg);
        }
    }


    @Override
    public void flush()
    {}
}
