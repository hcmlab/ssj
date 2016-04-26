/*
 * EventChannel.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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
import java.util.LinkedList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class EventChannel
{

    protected String _name = "SSJ_EventChannel";

    private ArrayList<EventListener> _listeners = new ArrayList<>();
    private LinkedList<Event>        _events    = new LinkedList<>();
    private int                      _event_id  = 0;

    final private Object _lock = new Object();
    protected boolean _terminate = false;

    protected TheFramework _frame;

    EventChannel()
    {
        _frame = TheFramework.getFramework();
    }
    
    public void reset()
    {
        _terminate = false;
        _event_id = 0;
    }

    public void addEventListener(EventListener listener)
    {
        _listeners.add(listener);
    }

    public Event getLastEvent(boolean peek, boolean blocking) {

        Event ev = null;

        synchronized (_lock) {
            while (!_terminate && _events.size() == 0)
            {
                if(blocking)
                {
                    try { _lock.wait(); }
                    catch (InterruptedException e) {}
                }
                else
                    return null;
            }

            if(_terminate)
                return null;

            ev = _events.getLast();

            if (!peek)
                _events.removeLast();
        }

        return ev;
    }

    public Event getEvent(int eventID, boolean blocking) {

        synchronized (_lock) {
            while (!_terminate && (_events.size() == 0 || eventID > _events.getLast().id))
            {
                if(blocking)
                {
                    try { _lock.wait(); }
                    catch (InterruptedException e) {}
                }
                else
                    return null;
            }

            if(_terminate)
                return null;

            if(eventID == _events.getFirst().id)
                return _events.getFirst();

            if(eventID < _events.getFirst().id)
            {
                Log.w("event "+ eventID +" no longer in queue");
                return _events.getFirst(); //if event is no longer in queue, return oldest event
            }

            //search for event
            for (Event ev : _events)
            {
                if (ev.id == eventID)
                    return ev;
            }
        }
        return null;
    }

    public void pushEvent(final Event ev)
    {
        synchronized (_lock) {
            //give event a local-unique ID
            ev.id = _event_id++;

            _events.addLast(ev);

//                Log.d("E_" + ev.id + "_" + ev.sender + ": name = " +ev.name+  " state = " + ev.state.toString() + " time = " + ev.time + " dur = " + ev.dur + " msg = " + ev.msg);

            if(_events.size() > Cons.MAX_NUM_EVENTS_PER_CHANNEL)
                _events.removeFirst();

            // Notify event listeners
            for (final EventListener listener : _listeners)
            {
                _frame._threadPool.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        listener.notify(ev);
                    }
                });
            }

            _lock.notifyAll();
        }
    }

    public void pushEvent(String msg) {
        pushEvent(new Event(msg));
    }

    public void pushEvent(byte[] msg, int pos, int len) {
        pushEvent(new Event(msg, pos, len));
    }


    public void close()
    {
        Log.i("shutting down");

        _terminate = true;

        synchronized (_lock)
        {
            _lock.notifyAll();
        }

        Log.i("shut down complete");
    }
}
