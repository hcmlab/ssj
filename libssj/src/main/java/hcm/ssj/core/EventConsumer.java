/*
 * EventConsumer.java
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

import hcm.ssj.core.stream.Stream;

/**
 * An EventConsumer is similar to a normal consumer only that its timing is defined by events
 *
 * Created by Johnny on 30.03.2015.
 */
public abstract class EventConsumer extends Component {

    private Stream[] _stream_in;
    private int[] _bufferID_in;

    private Timer _timer;
    private boolean localUpdateRate = false;

    protected TheFramework _frame;

    public EventConsumer()
    {
        _frame = TheFramework.getFramework();
    }

    @Override
    public void run()
    {
        if(!_isSetup) {
            Log.e("not initialized");
            return;
        }

        if(_evchannel_in == null || _evchannel_in.size() != 1)
        {
            Log.e("invalid configuration of incoming event channels");
            return;
        }

        android.os.Process.setThreadPriority(threadPriority);

        try {
            enter(_stream_in);
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "exception in enter", e);
        }

        //wait for framework
        while (!_frame.isRunning()) {
            try {
                Thread.sleep(Cons.SLEEP_ON_IDLE);
            } catch (InterruptedException e) {}
        }

        //maintain update rate starting from now
        _timer.reset();

        int eventID = 0;

        while(!_terminate && _frame.isRunning())
        {
            try {
                //wait for event
                Event ev = _evchannel_in.get(0).getEvent(eventID++, !localUpdateRate);

                if (ev != null && ev.dur > 0)
                {
                    //grab data
                    boolean ok = true;
                    for (int i = 0; i < _bufferID_in.length; i++)
                    {
                        int pos = (int) ((ev.time / 1000.0) * _stream_in[i].sr + 0.5);
                        int pos_stop = (int) (((ev.time + ev.dur) / 1000.0) * _stream_in[i].sr + 0.5);
                        int numSamples = pos_stop - pos;

                        // check if local buffer is large enough and make it larger if necessary
                        _stream_in[i].adjust(numSamples);

                        ok &= _frame.getData(_bufferID_in[i], _stream_in[i].ptr(), pos, numSamples);
                        _stream_in[i].time = ev.time / 1000.0;
                    }

                    //if we received data from all sources, process it
                    if (ok)
                        consume(_stream_in);
                }

                //maintain update rate
                if (localUpdateRate)
                    _timer.sync();

            } catch(Exception e) {
                _frame.crash(this.getClass().getSimpleName(), "exception in loop", e);
            }
        }

        try {
            flush(_stream_in);
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "exception in flush", e);
        }

        _safeToKill = true;
    }

    /**
     * initialization specific to transformer implementation
     */
    protected abstract void enter(Stream stream_in[]);

    /**
     * main processing method
     */
    protected abstract void consume(Stream stream_in[]);

    /**
     * called once prior to termination
     */
    protected abstract void flush(Stream stream_in[]);

    /**
     * general transformer initialization
     */
    public void setup(Provider[] sources, double frame)
    {
        try {
            _bufferID_in = new int[sources.length];
            _stream_in = new Stream[sources.length];
            int num_frame[] = new int[sources.length];

            for(int i = 0; i < sources.length; i++) {
                _bufferID_in[i] = sources[i].getBufferID();

                num_frame[i] = (int)(frame * sources[i].getOutputStream().sr + 0.5);

                //allocate local input buffer
                _stream_in[i] = Stream.create(sources[i], num_frame[i]);
            }

            // configure update rate
            if(frame != 0)
            {
                frame = num_frame[0] * _stream_in[0].step;
                localUpdateRate = true;
            }
            _timer = new Timer(frame);

        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "error configuring component", e);
        }

        _isSetup = true;
    }



}
