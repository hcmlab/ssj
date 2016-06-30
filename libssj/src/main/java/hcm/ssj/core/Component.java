/*
 * Component.java
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

package hcm.ssj.core;

import java.util.ArrayList;

/**
 * Created by Johnny on 05.03.2015.
 */
public abstract class Component implements Runnable
{
    protected String _name = "SSJ_Component";

    protected boolean _terminate = false;
    protected boolean _safeToKill = false;
    protected boolean _isSetup = false;

    protected ArrayList<EventChannel> _evchannel_in = null;
    protected EventChannel _evchannel_out = null;

    public int threadPriority = Cons.THREAD_PRIORITY_NORMAL;

    public void close() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
        Log.i(_name + " shutting down");

        _terminate = true;

        if(_evchannel_in != null)
            for(EventChannel ch : _evchannel_in)
                ch.close();

        if(_evchannel_out != null) _evchannel_out.close();

        double time = frame.getTime();
        while(!_safeToKill)
        {
            Thread.sleep(Cons.SLEEP_ON_TERMINATE);

            if(frame.getTime() > time + frame.options.timeoutThread.getValue())
            {
                Log.w(_name + "force-killed thread");
                forcekill();
                break;
            }
        }
        Log.i(_name + "shut down completed");
    }

    public void forcekill()
    {
        Thread.currentThread().interrupt();
    }

    public String getComponentName()
    {
        return _name;
    }

    void addEventChannelIn(EventChannel channel)
    {
        if(_evchannel_in == null)
            _evchannel_in = new ArrayList<>();

        _evchannel_in.add(channel);
    }
    void setEventChannelOut(EventChannel channel)
    {
        _evchannel_out = channel;
    }

    public void reset()
    {
        _terminate = false;
        _safeToKill = false;
        
        if(_evchannel_in != null)
            for(EventChannel ch : _evchannel_in)
                ch.reset();

        if(_evchannel_out != null) _evchannel_out.reset();
    }
}
