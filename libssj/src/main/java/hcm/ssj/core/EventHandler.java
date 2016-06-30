/*
 * EventHandler.java
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

/**
 * An EventHandler is a general component with no regulated inputs or outputs
 * Its only means of communication are events
 *
 * Created by Johnny on 30.03.2015.
 */
public abstract class EventHandler extends Component {

    protected TheFramework _frame;
    public EventHandler()
    {
        _frame = TheFramework.getFramework();
    }

    @Override
    public void run()
    {
        if(_evchannel_in == null && _evchannel_out == null)
        {
            Log.e("no event channel has been registered");
            return;
        }

        try {
            enter();
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "exception in enter", e);
        }

        //wait for framework
        while (!_frame.isRunning()) {
            try {
                Thread.sleep(Cons.SLEEP_ON_IDLE);
            } catch (InterruptedException e) {}
        }

        while(!_terminate && _frame.isRunning())
        {
            try {
                process();
            } catch(Exception e) {
                _frame.crash(this.getClass().getSimpleName(), "exception in loop", e);
            }
        }

        try {
            flush();
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "exception in flush", e);
        }

        _safeToKill = true;
    }

    /**
     * initialization specific to sensor implementation
     */
    protected abstract void enter();

    /**
     * main processing method
     */
    protected abstract void process();

    /**
     * called once before termination
     */
    protected abstract void flush();
}
