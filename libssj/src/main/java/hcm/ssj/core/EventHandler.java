/*
 * EventHandler.java
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

package hcm.ssj.core;

import android.content.Context;
import android.os.PowerManager;

import hcm.ssj.core.event.Event;

import static hcm.ssj.core.Cons.SLEEP_ON_COMPONENT_IDLE;

/**
 * An EventHandler is a general component with no regulated inputs or outputs
 * Its only means of communication are events
 *
 * Created by Johnny on 30.03.2015.
 */
public abstract class EventHandler extends Component implements EventListener {

    protected Pipeline _frame;
    protected boolean _doWakeLock = false;

    public EventHandler()
    {
        _frame = Pipeline.getInstance();
    }

    @Override
    public void run()
    {
        if(_evchannel_in == null && _evchannel_out == null)
        {
            _frame.error(_name, "no event channel has been registered", null);
            return;
        }

        PowerManager mgr = (PowerManager)SSJApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, _name);

        //register listener
        if(_evchannel_in != null && _evchannel_in.size() != 0)
            for(EventChannel ch : _evchannel_in)
                ch.addEventListener(this);

        try {
            enter();
        } catch(SSJFatalException e) {
            _frame.error(_name, "exception in enter", e);
            _safeToKill = true;
            return;
        } catch(Exception e) {
            _frame.error(_name, "exception in enter", e);
        }

        //wait for framework
        while (!_terminate && !_frame.isRunning()) {
            try {
                Thread.sleep(Cons.SLEEP_IN_LOOP);
            } catch (InterruptedException e) {
                Log.w("thread interrupt");
            }
        }

        while(!_terminate && _frame.isRunning())
        {
            try {
                if(_doWakeLock) wakeLock.acquire();
                process();
                if(_doWakeLock) wakeLock.release();
            } catch(SSJFatalException e) {
                _frame.error(_name, "exception in loop", e);
                _safeToKill = true;
                return;
            } catch(Exception e) {
                _frame.error(_name, "exception in loop", e);
            }
        }

        try {
            flush();
        } catch(Exception e) {
            _frame.error(_name, "exception in flush", e);
        }

        _safeToKill = true;
    }

    /**
     * initialization specific to sensor implementation
     */
    protected void enter() throws SSJFatalException {};

    /**
     * thread processing method, alternative to notify(), called in loop
     */
    protected void process() throws SSJFatalException
    {
        try {
            Thread.sleep(SLEEP_ON_COMPONENT_IDLE);
        } catch (InterruptedException e) {
            Log.w("thread interrupt");
        }
    }

    /**
     * alternative to process(), called once per received event
     */
    public void notify(Event event) {}

    /**
     * called once before termination
     */
    protected void flush() throws SSJFatalException {};
}
