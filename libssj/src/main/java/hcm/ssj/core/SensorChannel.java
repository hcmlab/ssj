/*
 * SensorChannel.java
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

import hcm.ssj.core.stream.Stream;

/**
 * Component handling data stream of a sensor device.
 * Requires a valid sensor instance (to handle the connection to the physical device)
 */
public abstract class SensorChannel extends Provider {

    private float _watchInterval  = Cons.DFLT_WATCH_INTERVAL; //how often should the watchdog check if the sensor is providing data (in seconds)
    private float _syncInterval = Cons.DFLT_SYNC_INTERVAL; //how often should the watchdog sync the buffer with the framework (in seconds)

    protected Pipeline _frame;
    protected Timer _timer;

    protected Sensor _sensor;

    public SensorChannel()
    {
        _frame = Pipeline.getInstance();
    }

    void setSensor(Sensor s)
    {
        _sensor = s;
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("SSJ_" + _name);

        if(!_isSetup) {
            _frame.error(this.getComponentName(), "not initialized", null);
            _safeToKill = true;
            return;
        }

        //if user did not specify a custom priority, use high priority
        android.os.Process.setThreadPriority( (threadPriority == Cons.THREAD_PRIORITY_NORMAL) ? Cons.THREAD_PRIORIIY_HIGH : threadPriority );
        PowerManager mgr = (PowerManager)SSJApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, _name);

        WatchDog dog = new WatchDog(_bufferID, _watchInterval, _syncInterval);

        if(_sensor == null)
        {
            Log.w("provider has not been attached to any sensor");
        }
        else
        {
            // wait for sensor to connect
            while (!_sensor.isConnected() && !_terminate)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.w("thread interrupt");
                }
            }
        }

        try {
            enter(_stream_out);
        } catch(SSJFatalException e) {
            _frame.error(this.getComponentName(), "exception in enter", e);
            _safeToKill = true;
            return;
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "exception in enter", e);
        }

        _timer.reset();

        while(!_terminate)
        {
            try {
                wakeLock.acquire();
                if(process(_stream_out))
                {
                    _frame.pushData(_bufferID, _stream_out.ptr(), _stream_out.tot);
                    dog.checkIn();
                }
                _timer.sync();
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

        //dog must be closed as soon as sensor stops providing
        try {
            dog.close();
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "exception closing watch dog", e);
        }

        try {
            flush(_stream_out);
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "exception in flush", e);
        }

        _safeToKill = true;
    }

    /**
     * early initialization specific to implementation (called by framework on instantiation)
     */
    protected void init() throws SSJException {}

    /**
     * initialization specific to sensor implementation (called by local thread after framework start and after sensor connects)
     */
    public void enter(Stream stream_out) throws SSJFatalException {}

    /**
     * main processing method
     */
    protected abstract boolean process(Stream stream_out) throws SSJFatalException;

    /**
     * called once prior to termination
     */
    public void flush(Stream stream_out) throws SSJFatalException {}

    /**
     * general sensor initialization
     */
    public void setup() throws SSJException {
        try
        {
            // figure out properties of output signal
            int bytes_out = getSampleBytes();
            int dim_out = getSampleDimension();
            double sr_out = getSampleRate();
            Cons.Type type_out = getSampleType();
            int num_out = getSampleNumber();

            _stream_out = Stream.create(num_out, dim_out, sr_out, type_out);

            describeOutput(_stream_out);

            // configure update rate
            _timer = new Timer((double)num_out / sr_out);
        }
        catch(Exception e)
        {
            throw new SSJException("error configuring component", e);
        }

        Log.i("Sensor Provider " + _name + " (output)" + '\n' +
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

    protected abstract double getSampleRate();
    protected abstract int getSampleDimension();
    protected abstract Cons.Type getSampleType();

    protected int getSampleBytes()
    {
        return Util.sizeOf(getSampleType());
    }

    /*
     * By default, every sensor will push their data to the framework asap one sample at a time.
     * If a specific sensor cannot do this, this function can be overriden
     */
    protected int getSampleNumber()
    {
        return 1;
    }

    protected abstract void describeOutput(Stream stream_out);

    //how often should the watchdog check if the sensor is providing data (in seconds)
    public void setWatchInterval(float watchInterval)
    {
        _watchInterval = watchInterval;
    }

    //how often should the watchdog sync the buffer with the framework (in seconds)
    public void setSyncInterval(float syncInterval)
    {
        _syncInterval = syncInterval;
    }
}
