/*
 * Transformer.java
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

import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public abstract class Transformer extends Provider {

    private Stream[] _stream_in;
    private int[] _bufferID_in;

    private int[] _readPos;
    private int[] _num_frame;
    private int[] _num_delta;

    private Timer _timer;

    protected Pipeline _frame;

    public Transformer()
    {
        _frame = Pipeline.getInstance();
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("SSJ_" + _name);

        if(!_isSetup) {
            _frame.error(this.getComponentName(), "not initialized", null);
            return;
        }

        android.os.Process.setThreadPriority(threadPriority);
        PowerManager mgr = (PowerManager)SSJApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, _name);

        //clear data
        Arrays.fill(_readPos, 0);
        for(int i = 0; i < _stream_in.length; i++)
            _stream_in[i].reset();

        try {
            enter(_stream_in, _stream_out);
        } catch(SSJFatalException e) {
            _frame.error(this.getComponentName(), "exception in enter", e);
            _safeToKill = true;
            return;
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "exception in enter", e);
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
        _timer.reset();

        while(!_terminate && _frame.isRunning())
        {
            try {
                wakeLock.acquire();

                //grab data
                boolean ok = true;
                for(int i = 0; i < _bufferID_in.length; i++)
                {
                    ok &= _frame.getData(_bufferID_in[i], _stream_in[i].ptr(), _readPos[i],
                                         _stream_in[i].num);
                    if(ok)
                        _stream_in[i].time = (double)_readPos[i] / _stream_in[i].sr;

                    _readPos[i] += _num_frame[i];
                }

                //if we received data from all sources, process it
                if(ok) {
                    transform(_stream_in, _stream_out);
                    _frame.pushData(_bufferID, _stream_out.ptr(), _stream_out.tot);
                }

                wakeLock.release();

                if(ok) {
                    //maintain update rate
                    _timer.sync();
                }
            } catch(SSJFatalException e) {
                _frame.error(this.getComponentName(), "exception in loop", e);
                _safeToKill = true;
                return;
            } catch(Exception e) {
                _frame.error(this.getComponentName(), "exception in loop", e);
            } finally {
                wakeLock.release();
            }
        }

        try {
            flush(_stream_in, _stream_out);
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "exception in flush", e);
        }
        _safeToKill = true;
    }

    /**
     * early initialization specific to implementation (called by framework on instantiation)
     */
    public void init(double frame, double delta) throws SSJException {}

    /**
     * initialization specific to sensor implementation (called by local thread after framework start)
     */
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException {}

    /**
     * main processing method
     */
    public abstract void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException;

    /**
     * called once prior to termination
     */
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException {}

    /**
     * general transformer initialization
     */
    public final void setup(Provider[] sources, double frame, double delta) throws SSJException
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
            _readPos = new int[sources.length];
            _num_frame = new int[sources.length];
            _num_delta = new int[sources.length];

            //compute window sizes
            for (int i = 0; i < sources.length; i++) {
                _num_frame[i] = (int) (frame * sources[i].getOutputStream().sr + 0.5);
                _num_delta[i] = (int) (delta * sources[i].getOutputStream().sr + 0.5);
            }
            frame = (double) _num_frame[0] / sources[0].getOutputStream().sr;
            delta = (double) _num_delta[0] / sources[0].getOutputStream().sr;

            if (frame == 0)
                throw new SSJException("frame size too small");

            //give implementation a chance to react to window size
            init(frame, delta);

            //allocate local input buffer
            for (int i = 0; i < sources.length; i++) {
                _bufferID_in[i] = sources[i].getBufferID();
                _stream_in[i] = Stream.create(sources[i], _num_frame[i], _num_delta[i]);
            }

            // figure out properties of output signal based on first input stream
            int bytes_out = getSampleBytes(_stream_in);
            int dim_out = getSampleDimension(_stream_in);
            Cons.Type type_out = getSampleType(_stream_in);

            int num_out = getSampleNumber(_num_frame[0]);
            double sr_out = (double) num_out / frame;

            if(num_out > 1 && delta != 0)
                Log.w("Non-feature transformer called with positive delta. Transformer may not support this.");

            _stream_out = Stream.create(num_out, dim_out, sr_out, type_out);

            describeOutput(_stream_in, _stream_out);

            // configure update rate
            _timer = new Timer(frame);
            _timer.setStartOffset(delta);
        }
        catch(Exception e)
        {
            throw new SSJException("error configuring component", e);
        }

        Log.i("Transformer " + _name + " (output)" + '\n' +
                "\tbytes=" +_stream_out.bytes+ '\n' +
                "\tdim=" +_stream_out.dim+ '\n' +
                "\ttype=" +_stream_out.type.toString() + '\n' +
                "\tnum=" +_stream_out.num+ '\n' +
                "\tsr=" +_stream_out.sr);

        _isSetup = true;
    }

    @Override
    public String[] getOutputDescription()
    {
        if(!_isSetup) {
            Log.e("not initialized");
            return null;
        }

        String[] desc = new String[_stream_out.desc.length];
        System.arraycopy(_stream_out.desc, 0, desc, 0, _stream_out.desc.length);

        return desc;
    }

    public abstract int getSampleDimension(Stream[] stream_in);
    public abstract int getSampleBytes(Stream[] stream_in);
    public abstract Cons.Type getSampleType(Stream[] stream_in);
    public abstract int getSampleNumber(int sampleNumber_in);

    protected abstract void describeOutput(Stream[] stream_in, Stream stream_out);
}
