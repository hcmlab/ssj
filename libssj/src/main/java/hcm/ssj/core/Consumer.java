/*
 * Consumer.java
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

package hcm.ssj.core;

import android.util.Log;

import java.util.Arrays;

import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public abstract class Consumer extends Component {

    private Stream[] _stream_in;
    private int[] _readPos ;
    private int[] _bufferID_in;

    private int[] _num_frame;
    private int[] _num_delta;

    private Timer _timer;

    protected TheFramework _frame;

    public Consumer()
    {
        _frame = TheFramework.getFramework();
    }

    @Override
    public void run()
    {
        if(!_isSetup) {
            Log.e(_name, "not initialized");
            return;
        }

        Arrays.fill(_readPos, 0);
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

        while(!_terminate && _frame.isRunning())
        {
            try {
                //grab data
                boolean ok = true;
                for(int i = 0; i < _bufferID_in.length; i++)
                {
                    ok &= _frame.getData(_bufferID_in[i], _stream_in[i].ptr(), _readPos[i],
                                         _stream_in[i].num);
                    _stream_in[i].time = (double)_readPos[i] / _stream_in[i].sr;
                    _readPos[i] += _num_frame[i];
                }

                //if we received data from all sources, process it
                if(ok) {
                    consume(_stream_in);

                    //maintain update rate
                    _timer.sync();
                }
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
    protected void init(Stream stream_in[]) {}

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

    /**
     * general transformer initialization
     */
    public void setup(Provider[] sources, double frame, double delta)
    {
        try {
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
                _stream_in[i] = Stream.create(sources[i], _num_frame[i] + _num_delta[i]);
            }

            //give implementation a chance to react to window size
            init(_stream_in);

            // configure update rate
            _timer = new Timer(frame);
            _timer.setStartOffset(delta);

        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "error configuring component", e);
        }

        _isSetup = true;
    }

}
