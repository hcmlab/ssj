/*
 * TheFramework.java
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Johnny on 05.03.2015.
 */
public class TheFramework {

    public class Options
    {
        public int countdown = 3;
        public float bufferSize = 2.0f;
        public float timeoutThread = 5.0f;

        public boolean netSync = false;
        public boolean netSyncListen = false; //set true if this is not the server pipe
        public int netSyncPort = 55100;
    }
    public Options options = new Options();

    protected String _name = "SSJ_Framework";
    protected boolean _isRunning = false;
    protected Timer _timer = new Timer();

    DatagramSocket _syncSocket;

    ThreadPoolExecutor _threadPool;

    //components
    protected ArrayList<Component> _components = new ArrayList<>();

    //buffers
    protected ArrayList<TimeBuffer> _buffer = new ArrayList<>();

    protected static TheFramework _instance = null;
    private TheFramework()
    {
        Log.i(_name, "===================================================");
        Log.i(_name, "Social Signal Interpretation for Java/Android, version "+ Cons.VERSION);

        int coreThreads = Runtime.getRuntime().availableProcessors();
        _threadPool = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    public static TheFramework getFramework()
    {
        if(_instance == null)
            _instance = new TheFramework();

        return _instance;
    }

    public static boolean isInstanced()
    {
        return _instance != null;
    }

    public void Start()
    {
        try {
            Log.i(_name, "starting pipeline");

            Log.i(_name, "preparing buffers");
            for (TimeBuffer b : _buffer)
                b.reset();

            for (Component c : _components) {
                Log.i(_name, "starting " + c.getComponentName());
                c.reset();
                _threadPool.execute(c);
            }

            for (int i = 0; i < options.countdown; i++) {
                Log.i(_name, "starting pipeline in " + (options.countdown - i));
                Thread.sleep(1000);
            }

            if(options.netSync)
            {
                try
                {
                    if(options.netSyncListen)
                    {
                        _syncSocket = new DatagramSocket(options.netSyncPort);
                        _syncSocket.setReuseAddress(true);

                        Log.i(_name, "waiting for master pipeline (port = "+ options.netSyncPort+")");
                        while(true)
                        {
                            byte[] data = new byte[4];
                            DatagramPacket packet = new DatagramPacket(data, 4);
                            _syncSocket.receive(packet);
                            Log.d(_name, "received packet from " + packet.getAddress().toString());

                            //check data
                            if(packet.getData()[0] == 'S' && packet.getData()[1] == 'S' && packet.getData()[2] == 'J' && packet.getData()[3] == 1)
                            {
                                Log.d(_name, "packet identified as start ping");
                                break;
                            }
                            Log.d(_name, "packet not recognized");
                        }
                    }
                    else
                    {
                        _syncSocket = new DatagramSocket(null);
                        _syncSocket.setReuseAddress(true);
                        _syncSocket.setBroadcast(true);

                        byte[] data = {'S', 'S', 'J', 1};
                        DatagramPacket packet = new DatagramPacket(data, 4, Util.getBroadcastAddress(), options.netSyncPort);
                        _syncSocket.send(packet);

                        Log.i(_name, "sync ping sent on port " + options.netSyncPort);
                    }
                }
                catch (IOException e)
                {
                    Log.e(_name, "network sync failed", e);
                    return;
                }
            }

            _timer = new Timer();
            _isRunning = true;
            Log.i(_name, "pipeline started");

        } catch(Exception e) {
            Log.e(_name, "error starting pipeline", e);
            throw new RuntimeException(e);
        }
    }

    public void addSensor(Sensor s)
    {
        _components.add(s);
    }

    /**
     * Used by the sensor to initialize each provider
     * @param s the sensor which owns the provider
     * @param p the provider to be added to the framework
     */
    void addSensorProvider(Sensor s, SensorProvider p)
    {
        p.setSensor(s);
        p.init();

        int dim = p.getSampleDimension();
        double sr = p.getSampleRate();
        int bytesPerValue = p.getSampleBytes();
        Cons.Type type = p.getSampleType();

        //add output buffer
        TimeBuffer buf = new TimeBuffer(options.bufferSize, sr, dim, bytesPerValue, type);
        _buffer.add(buf);
        int buffer_id = _buffer.size() -1;
        p.setBufferID(buffer_id);

        p.setup();
        _components.add(p);
    }

    public Provider addTransformer(Transformer t, Provider source, double frame, double delta)
    {
        Provider[] sources = {source};
        return addTransformer(t, sources, frame, delta);
    }

    public Provider addTransformer(Transformer t, Provider[] sources, double frame, double delta)
    {
        t.setup(sources, frame, delta);

        int dim = t.getOutputStream().dim;
        double sr = t.getOutputStream().sr;
        int bytesPerValue = t.getOutputStream().bytes;
        Cons.Type type = t.getOutputStream().type;

        //add output buffer
        TimeBuffer buf = new TimeBuffer(options.bufferSize, sr, dim, bytesPerValue, type);
        _buffer.add(buf);
        int buffer_id = _buffer.size() -1;
        t.setBufferID(buffer_id);

        _components.add(t);
        return t;
    }

    public void addConsumer(Consumer c, Provider source, double frame, double delta)
    {
        Provider[] sources = {source};
        addConsumer(c, sources, frame, delta);
    }

    public void addConsumer(Consumer c, Provider[] sources, double frame, double delta)
    {
        c.setup(sources, frame, delta);
        _components.add(c);
    }

    public void addEventConsumer(EventConsumer c, Provider source, double frameSize, EventChannel channel)
    {
        Provider[] sources = {source};
        addEventConsumer(c, sources, frameSize, channel);
    }

    public void addEventConsumer(EventConsumer c, Provider[] sources, double frameSize, EventChannel channel)
    {
        c.setup(sources, frameSize);
        c.addEventChannelIn(channel);
        _components.add(c);
    }

    public void addEventConsumer(EventConsumer c, Provider source, EventChannel channel)
    {
        Provider[] sources = {source};
        addEventConsumer(c, sources, channel);
    }

    public void addEventConsumer(EventConsumer c, Provider[] sources, EventChannel channel)
    {
        c.setup(sources, 0);
        c.addEventChannelIn(channel);
        _components.add(c);
    }

    /**
     * Adds an unspecific component to the framework.
     * No initialization is performed and no buffers are allocated.
     * @param c the component to be added
     */
    public void addComponent(Component c)
    {
        _components.add(c);
    }

    public EventChannel registerEventProvider(Component c)
    {
        EventChannel channel = new EventChannel();
        c.setEventChannelOut(channel);
        return channel;
    }
    public void registerEventListener(Component c, EventChannel channel)
    {
        c.addEventChannelIn(channel);
    }

    public void pushData(int buffer_id, Object data, int numBytes)
    {
        if(!isRunning()) {
            return;
        }

        if(buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w(_name, "cannot push to buffer "+buffer_id +". Buffer does not exist.");

        _buffer.get(buffer_id).push(data, numBytes);
    }

    public void pushZeroes(int buffer_id)
    {
        if(!isRunning()) {
            return;
        }

        if(buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w(_name, "cannot push to buffer "+buffer_id +". Buffer does not exist.");

        TimeBuffer buf = _buffer.get(buffer_id);

        double frame_time = getTime();
        double buffer_time = buf.getLastWrittenSampleTime();

        if (buffer_time < frame_time) {
            int bytes = (int)((frame_time - buffer_time) * buf.getSampleRate()) * buf.getBytesPerSample();

            if(bytes > 0)
                buf.pushZeroes(bytes);
        }
    }

    public void pushZeroes(int buffer_id, int num)
    {
        if(!isRunning()) {
            return;
        }

        if(buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w(_name, "cannot push to buffer "+buffer_id +". Buffer does not exist.");

        _buffer.get(buffer_id).pushZeroes(num);
    }

    public boolean getData(int buffer_id, Object data, double start_time, double duration)
    {
        if(!_isRunning) {
            return false;
        }

        if(buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w(_name, "cannot read from buffer "+buffer_id +". Buffer does not exist.");

        int res = _buffer.get(buffer_id).get(data, start_time, duration);

        switch(res)
        {
            case TimeBuffer.STATUS_INPUT_ARRAY_TOO_SMALL:
                Log.w(_name, "input buffer too small");
                return false;
            case TimeBuffer.STATUS_DATA_EXCEEDS_BUFFER_SIZE:
                Log.w(_name, "data exceeds buffers size");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_YET:
                Log.w(_name, "data not in buffer yer");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_ANYMORE:
                Log.w(_name, "data not in buffer anymore");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_SMALL:
                Log.w(_name, "requested duration too small");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_LARGE:
                Log.w(_name, "requested duration too large");
                return false;
            case TimeBuffer.STATUS_ERROR:
                if(_isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(_name, "unknown error occurred");
                return false;
        }

        return true;
    }

    public boolean getData(int buffer_id, Object data, int startSample, int numSamples)
    {
        if(!_isRunning) {
            return false;
        }

        if(buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w(_name, "Invalid buffer");

        int res = _buffer.get(buffer_id).get(data, startSample, numSamples);

        switch(res)
        {
            case TimeBuffer.STATUS_INPUT_ARRAY_TOO_SMALL:
                Log.w(_name, "input buffer too small");
                return false;
            case TimeBuffer.STATUS_DATA_EXCEEDS_BUFFER_SIZE:
                Log.w(_name, "data exceeds buffers size");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_YET:
                Log.w(_name, "data not in buffer yet");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_ANYMORE:
                Log.w(_name, "data range ("+startSample +","+ (startSample + numSamples) +") not in buffer anymore");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_SMALL:
                Log.w(_name, "requested duration too small");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_LARGE:
                Log.w(_name, "requested duration too large");
                return false;
            case TimeBuffer.STATUS_UNKNOWN_DATA:
                Log.w(_name, "requested data is unknown, probably caused by a delayed sensor start");
                return false;
            case TimeBuffer.STATUS_ERROR:
                if(_isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(_name, "unknown buffer error occurred");
                return false;
        }

        return true;
    }

    public void Stop()
    {
        if (!_isRunning)
        {
            Log.i(_name, "Cannot stop. Framework not active.");
            return;
        }

        _isRunning = false;

        Log.i(_name, "shutting down ...");
        try
        {
            Log.i(_name, "closing buffer");
            for (TimeBuffer b : _buffer)
                b.close();

            Log.i(_name, "closing components");
            for (Component c : _components)
            {
                Log.i(_name, "closing " + c.getComponentName());
                //try to close everything individually to free each sensor
                try
                {
                    c.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    Log.e(_name, "closing " + c.getComponentName() + " failed");
                }
            }

            Log.i(_name, "waiting for components to terminate");
            _threadPool.awaitTermination(Cons.WAIT_THREAD_TERMINATION, TimeUnit.MICROSECONDS);
        }
        catch (Exception e)
        {
            Log.e(_name, "Exception in closing framework", e);
            throw new RuntimeException(e);
        }
        Log.i(_name, "shut down completed");
    }

    public void clear()
    {
        if(isRunning())
        {
            Log.w(_name, "Cannot clear. Framework still active.");
            return;
        }

        _components.clear();
        _buffer.clear();
        _instance = null;
    }

    public void sync(int bufferID)
    {
        _buffer.get(bufferID).sync(_timer.getElapsed());
    }

    public double getTime()
    {
        return _timer.getElapsed();
    }

    public long getTimeMs()
    {
        return _timer.getElapsedMs();
    }

    public boolean isRunning()
    {
        return _isRunning;
    }
}
