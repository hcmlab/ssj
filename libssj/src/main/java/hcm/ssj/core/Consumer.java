/*
 * Consumer.java
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

import java.util.Arrays;

import hcm.ssj.core.event.Event;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public abstract class Consumer extends Component {

    private Stream[] _stream_in;
    private int[] _readPos = null;
    private int[] _bufferID_in;

    private int[] _num_frame;
    private int[] _num_delta;

    private boolean _triggeredByEvent = false;

    private Timer _timer;

    protected Pipeline _frame;
    protected boolean _doWakeLock = true;

    public Consumer()
    {
        _frame = Pipeline.getInstance();
    }

    @Override
    public void run()
    {
        if(!_isSetup) {
            Log.e("not initialized");
            return;
        }

        android.os.Process.setThreadPriority(threadPriority);
        PowerManager mgr = (PowerManager)SSJApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, _name);

        Event ev = null;
        int eventID = 0;

        //clear data
        if(_readPos != null)
            Arrays.fill(_readPos, 0);
        for(int i = 0; i < _stream_in.length; i++)
            _stream_in[i].reset();

        try {
            enter(_stream_in);
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "exception in enter", e);
        }

        //wait for framework
        while (!_terminate && !_frame.isRunning()) {
            try {
                Thread.sleep(Cons.SLEEP_IN_LOOP);
            } catch (InterruptedException e) {
                Log.w("thread interrupt");
            }
        }

        //maintain update rate starting from now
        if(!_triggeredByEvent)
            _timer.reset();

        while(!_terminate && _frame.isRunning())
        {
            try {
                if(_triggeredByEvent) {
                    ev = _evchannel_in.get(0).getEvent(eventID++, true);
                    if (ev == null || ev.dur == 0)
                        continue;
                }

                if(_doWakeLock) wakeLock.acquire();

                //grab data
                boolean ok = true;
                int pos, numSamples;
                for(int i = 0; i < _bufferID_in.length; i++)
                {
                    if(_triggeredByEvent)
                    {
                        pos = (int) ((ev.time / 1000.0) * _stream_in[i].sr + 0.5);
                        numSamples = ((int) (((ev.time + ev.dur) / 1000.0) * _stream_in[i].sr + 0.5)) - pos;

                        // check if local buffer is large enough and make it larger if necessary
                        _stream_in[i].adjust(numSamples);
                    }
                    else
                    {
                        pos = _readPos[i];
                        _readPos[i] += _num_frame[i];
                    }

                    ok &= _frame.getData(_bufferID_in[i], _stream_in[i].ptr(), pos, _stream_in[i].num);
                    if (ok)
                        _stream_in[i].time = (double) pos / _stream_in[i].sr;
                }

                //if we received data from all sources, process it
                if(ok) {
                    if(_triggeredByEvent)
                    {
                        prepareForTriggerByEvent(ev);
                    }
                    consume(_stream_in);
                }

                if(_doWakeLock) wakeLock.release();

                //maintain update rate
                if(ok && !_triggeredByEvent)
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
     * initialization specific to sensor implementation (called by framework on instantiation)
     */
    protected void init(Stream stream_in[]) throws SSJException {}

    /**
     * initialization specific to sensor implementation (called by local thread after framework start)
     */
    public void enter(Stream stream_in[]) {}

    /**
     * main processing method
     */
    protected abstract void consume(Stream stream_in[]);

    /**
     * called once prior to termination
     */
    public void flush(Stream stream_in[]) {}

    public void setTriggeredByEvent(boolean value)
    {
        _triggeredByEvent = value;
    }

    /**
     * Called immediately before the consume method in case of an event trigger
     * @param ev the event which triggers the consume
     */
    protected void prepareForTriggerByEvent(Event ev) {}

    /**
     * initialization for continuous consumer
     */
    public void setup(Provider[] sources, double frame, double delta) throws SSJException
    {
        try
        {
            _bufferID_in = new int[sources.length];
            _readPos = new int[sources.length];
            _stream_in = new Stream[sources.length];
            _num_frame = new int[sources.length];
            _num_delta = new int[sources.length];

            //compute window sizes
            for(int i = 0; i < sources.length; i++) {
                _num_frame[i] = (int)(frame * sources[i].getOutputStream().sr + 0.5);
                _num_delta[i] = (int)(delta * sources[i].getOutputStream().sr + 0.5);
            }
            frame = (double)_num_frame[0] / sources[0].getOutputStream().sr;
            delta = (double)_num_delta[0] / sources[0].getOutputStream().sr;

            //allocate local input buffer
            for(int i = 0; i < sources.length; i++) {
                _bufferID_in[i] = sources[i].getBufferID();
                _stream_in[i] = Stream.create(sources[i], _num_frame[i], _num_delta[i]);
            }

            //give implementation a chance to react to window size
            init(_stream_in);

            // configure update rate
            _timer = new Timer(frame);
            _timer.setStartOffset(delta);
        }
        catch(Exception e)
        {
            throw new SSJException("error configuring component", e);
        }

        _isSetup = true;
    }

    /**
     * initialization for event consumer
     */
    public void setup(Provider[] sources) throws SSJException {
        try {
            _bufferID_in = new int[sources.length];
            _stream_in = new Stream[sources.length];

            for(int i = 0; i < sources.length; i++) {
                _bufferID_in[i] = sources[i].getBufferID();

                //allocate local input buffer and make it one second large too avoid memory allocation at runtime
                _stream_in[i] = Stream.create(sources[i], (int)sources[i].getOutputStream().sr);
            }

            init(_stream_in);

        } catch(Exception e) {
            throw new SSJException("error configuring component", e);
        }

        _isSetup = true;
    }
}
