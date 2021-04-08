/*
 * Consumer.java
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

    private EventChannel _triggerChannel = null;

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
        Thread.currentThread().setName("SSJ_" + _name);

        if(!_isSetup) {
            _frame.error(_name, "not initialized", null);
            _safeToKill = true;
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

        //maintain update rate starting from now
        if(_triggerChannel == null)
            _timer.reset();

        while(!_terminate && _frame.isRunning())
        {
            try {
                if(_triggerChannel != null) {
                    ev = _triggerChannel.getEvent(eventID++, true);
                    if (ev == null || ev.dur == 0)
                        continue;
                }

                if(_doWakeLock) wakeLock.acquire();

                //grab data
                boolean ok = true;
                int pos, numSamples;
                for(int i = 0; i < _bufferID_in.length; i++)
                {
                    if(_triggerChannel != null)
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
                    consume(_stream_in, ev);
                }

                //maintain update rate
                if(ok && _triggerChannel == null)
                    _timer.sync();

            } catch(SSJFatalException e) {
                _frame.error(_name, "exception in loop", e);
                _safeToKill = true;
                return;
            } catch(Exception e) {
                _frame.error(_name, "exception in loop", e);
            } finally {
                if(_doWakeLock && wakeLock.isHeld()) wakeLock.release();
            }
        }

        try {
            flush(_stream_in);
        } catch(Exception e) {
            _frame.error(_name, "exception in flush", e);
        }

        _safeToKill = true;
    }

    /**
     * Initialization specific to sensor implementation (called by framework on instantiation)
     *
     * @param stream_in Input stream
     * @throws SSJException Exception
     */
    protected void init(Stream[] stream_in) throws SSJException {}

    /**
     * Initialization specific to sensor implementation (called by local thread after framework start)
     *
     * @param stream_in Input stream
     * @throws SSJFatalException Causes immediate pipeline termination
     */
    public void enter(Stream[] stream_in) throws SSJFatalException {}

    /**
     * Main processing method
     *
     * @param stream_in Input stream
     * @param trigger Event trigger
     * @throws SSJFatalException Exception
     */
    protected abstract void consume(Stream[] stream_in, Event trigger) throws SSJFatalException;

    /**
     * Called once prior to termination
     *
     * @param stream_in Input stream
     * @throws SSJFatalException Exception
     */
    public void flush(Stream[] stream_in) throws SSJFatalException {}

    public void setEventTrigger(EventChannel channel)
    {
        _triggerChannel = channel;
    }
    public EventChannel getEventTrigger()
    {
        return _triggerChannel;
    }

    /**
     * Initialization for continuous consumer
     *
     * @param sources Providers
     * @param frame Frame size
     * @param delta Delta size
     * @throws SSJException Exception
     */
    public void setup(Provider[] sources, double frame, double delta) throws SSJException
    {
        for (Provider source : sources)
        {
            if (!source.isSetup())
            {
                throw new SSJException("Components must be added in the correct order. Cannot add " + _name + " before its source " + source.getComponentName());
            }
        }

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
     * Initialization for event consumer
     *
     * @param sources Providers
     * @throws SSJException Exception
     */
    public void setup(Provider[] sources) throws SSJException
    {
        for (Provider source : sources)
        {
            if (!source.isSetup())
            {
                throw new SSJException("Components must be added in the correct order. Cannot add " + _name + " before its source " + source.getComponentName());
            }
        }

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

    @Override
    public void close()
    {
        if(_triggerChannel != null)
            _triggerChannel.close();

        super.close();
    }

    @Override
    public void reset()
    {
        if(_triggerChannel != null)
            _triggerChannel.reset();

        super.reset();
    }
}
