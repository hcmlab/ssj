/*
 * EventLogger.java
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

package hcm.ssj.test;

import java.util.Arrays;

import hcm.ssj.core.EventChannel;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.OptionList;

/**
 * Outputs all incoming events using logcat
 */
public class EventLogger extends EventHandler
{
    public class Options extends OptionList
    {
        /**
         *
         */
        private Options() {addOptions();}
    }
    public final Options options = new Options();

    public EventLogger()
    {
        _name = "EventLogger";
        _doWakeLock = true;
        Log.d("Instantiated EventLogger "+this.hashCode());
    }

    int _lastBehavEventID;

    @Override
	public void enter() throws SSJFatalException
    {
        _lastBehavEventID = -1;

		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new RuntimeException("no input channels");
		}
    }

    @Override
    protected void process() throws SSJFatalException
    {
        for(EventChannel ch : _evchannel_in)
        {
            Event ev = ch.getEvent(_lastBehavEventID + 1, true);
            if (ev == null)
            {
                return;
            }

            _lastBehavEventID = ev.id;

            String msg = "";
            switch(ev.type)
            {
                case BYTE:
                    msg = Arrays.toString(ev.ptrB());
                    break;
                case CHAR:
                    msg = ev.ptrStr();
                    break;
                case STRING:
                    msg = ev.ptrStr();
                    break;
                case SHORT:
                    msg = Arrays.toString(ev.ptrShort());
                    break;
                case INT:
                    msg = Arrays.toString(ev.ptrI());
                    break;
                case LONG:
                    msg = Arrays.toString(ev.ptrL());
                    break;
                case FLOAT:
                    msg = Arrays.toString(ev.ptrF());
                    break;
                case DOUBLE:
                    msg = Arrays.toString(ev.ptrD());
                    break;
                case BOOL:
                    msg = Arrays.toString(ev.ptrBool());
                    break;
            }

            Log.i(ev.sender + "_" + ev.name + "_" + ev.id + " (" + ev.state.toString() + ", " + ev.time + ", " + ev.dur + ") : " + msg);
        }
    }


    @Override
    public void flush() throws SSJFatalException
    {}
}
