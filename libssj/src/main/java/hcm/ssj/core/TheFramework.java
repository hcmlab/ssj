/*
 * TheFramework.java
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import hcm.ssj.BuildConfig;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class TheFramework
{

    public class Options extends OptionList
    {
        public final Option<Integer> countdown = new Option<>("countdown", 3, Integer.class, "");
        public final Option<Float> bufferSize = new Option<>("bufferSize", 2.f, Float.class, "");
        public final Option<Float> timeoutThread = new Option<>("timeoutThread", 5.f, Float.class, "");
        public final Option<Boolean> netSync = new Option<>("netSync", false, Boolean.class, "");
        public final Option<Boolean> netSyncListen = new Option<>("netSyncListen", false, Boolean.class, "set true if this is not the server pipe");
        public final Option<Integer> netSyncPort = new Option<>("netSyncPort", 55100, Integer.class, "");
        public final Option<String> logfile = new Option<>("logfile", null, String.class, "");
        public final Option<Log.Level> loglevel = new Option<>("loglevel", Log.Level.VERBOSE, Log.Level.class, "show all logs >= level");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    protected String _name = "SSJ_Framework";
    protected boolean _isRunning = false;
    protected boolean _isStopping = false;
    protected Timer _timer = null;
    protected long _startTime = 0;

    DatagramSocket _syncSocket;

    ThreadPool _threadPool;
    ExceptionHandler _exceptionHandler = null;

    //components
    protected ArrayList<Component> _components = new ArrayList<>();

    //buffers
    protected ArrayList<TimeBuffer> _buffer = new ArrayList<>();

    protected static TheFramework _instance = null;

    private TheFramework()
    {
        //configure logger
        Log.getInstance().setFramework(this);

        int coreThreads = Runtime.getRuntime().availableProcessors();
        _threadPool = new ThreadPool(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        Log.i("\nSocial Signal Interpretation for Java/Android v" + getVersion() + "\n\n");
    }

    public static TheFramework getFramework()
    {
        if (_instance == null)
            _instance = new TheFramework();

        return _instance;
    }

    public static boolean isInstanced()
    {
        return _instance != null;
    }

    public void Start()
    {
        try
        {
            Log.i("starting pipeline (SSJ v" + getVersion() + ")");

            Log.i("preparing buffers");
            for (TimeBuffer b : _buffer)
                b.reset();

            for (Component c : _components)
            {
                Log.i("starting " + c.getComponentName());
                c.reset();
                _threadPool.execute(c);
            }

            for (int i = 0; i < options.countdown.get(); i++)
            {
                Log.i("starting pipeline in " + (options.countdown.get() - i));
                Thread.sleep(1000);
            }

            if (options.netSync.get())
            {
                try
                {
                    if (options.netSyncListen.get())
                    {
                        _syncSocket = new DatagramSocket(options.netSyncPort.get());
                        _syncSocket.setReuseAddress(true);

                        Log.i("waiting for master pipeline (port = " + options.netSyncPort + ")");
                        while (true)
                        {
                            byte[] data = new byte[32];
                            DatagramPacket packet = new DatagramPacket(data, 32);
                            _syncSocket.receive(packet);
                            Log.d("received packet from " + packet.getAddress().toString());

                            //check data
                            String str =  new String(packet.getData(), "ASCII");
                            if (str.startsWith("SSI:STRT:RUN")) //SSI format for compatibility
                            {
                                Log.d("packet identified as start ping");
                                break;
                            }
                            Log.d("packet not recognized");
                        }
                    } else
                    {
                        _syncSocket = new DatagramSocket(null);
                        _syncSocket.setReuseAddress(true);
                        _syncSocket.setBroadcast(true);

                        String msg = "SSI:STRT:RUN1"; //send in SSI format for compatibility
                        byte[] data = msg.getBytes("ASCII");
                        DatagramPacket packet = new DatagramPacket(data, data.length, Util.getBroadcastAddress(), options.netSyncPort.get());
                        _syncSocket.send(packet);

                        Log.i("sync ping sent on port " + options.netSyncPort.get());
                    }
                } catch (IOException e)
                {
                    Log.e("network sync failed", e);
                }
            }

            _startTime = System.currentTimeMillis();
            _timer = new Timer();
            _isRunning = true;
            Log.i("pipeline started");

        } catch (Exception e)
        {
            crash("framework start", "error starting pipeline", e);
        }
    }

    public void addSensor(Sensor s)
    {
        s.init();
        _components.add(s);
    }

    /**
     * Used by the sensor to initialize each provider
     *
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
        TimeBuffer buf = new TimeBuffer(options.bufferSize.get(), sr, dim, bytesPerValue, type, p);
        _buffer.add(buf);
        int buffer_id = _buffer.size() - 1;
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
        TimeBuffer buf = new TimeBuffer(options.bufferSize.get(), sr, dim, bytesPerValue, type, t);
        _buffer.add(buf);
        int buffer_id = _buffer.size() - 1;
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
     *
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
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        _buffer.get(buffer_id).push(data, numBytes);
    }

    public void pushZeroes(int buffer_id)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        TimeBuffer buf = _buffer.get(buffer_id);

        double frame_time = getTime();
        double buffer_time = buf.getLastWrittenSampleTime();

        if (buffer_time < frame_time)
        {
            int bytes = (int) ((frame_time - buffer_time) * buf.getSampleRate()) * buf.getBytesPerSample();

            if (bytes > 0)
                buf.pushZeroes(bytes);
        }
    }

    public void pushZeroes(int buffer_id, int num)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        _buffer.get(buffer_id).pushZeroes(num);
    }

    public boolean getData(int buffer_id, Object data, double start_time, double duration)
    {
        if (!_isRunning)
        {
            return false;
        }

        if (buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w("cannot read from buffer " + buffer_id + ". Buffer does not exist.");

        TimeBuffer buf = _buffer.get(buffer_id);
        int res = buf.get(data, start_time, duration);

        switch (res)
        {
            case TimeBuffer.STATUS_INPUT_ARRAY_TOO_SMALL:
                Log.w(buf.getOwner().getComponentName(), "input buffer too small");
                return false;
            case TimeBuffer.STATUS_DATA_EXCEEDS_BUFFER_SIZE:
                Log.w(buf.getOwner().getComponentName(), "data exceeds buffers size");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_YET:
                Log.w(buf.getOwner().getComponentName(), "data not in buffer yer");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_ANYMORE:
                Log.w(buf.getOwner().getComponentName(), "data not in buffer anymore");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_SMALL:
                Log.w(buf.getOwner().getComponentName(), "requested duration too small");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_LARGE:
                Log.w(buf.getOwner().getComponentName(), "requested duration too large");
                return false;
            case TimeBuffer.STATUS_ERROR:
                if (_isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown error occurred");
                return false;
        }

        return true;
    }

    public boolean getData(int buffer_id, Object data, int startSample, int numSamples)
    {
        if (!_isRunning)
        {
            return false;
        }

        if (buffer_id < 0 || buffer_id >= _buffer.size())
            Log.w("Invalid buffer");

        TimeBuffer buf = _buffer.get(buffer_id);
        int res = buf.get(data, startSample, numSamples);

        switch (res)
        {
            case TimeBuffer.STATUS_INPUT_ARRAY_TOO_SMALL:
                Log.w(buf.getOwner().getComponentName(), "input buffer too small");
                return false;
            case TimeBuffer.STATUS_DATA_EXCEEDS_BUFFER_SIZE:
                Log.w(buf.getOwner().getComponentName(), "data exceeds buffers size");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_YET:
                Log.w(buf.getOwner().getComponentName(), "data not in buffer yet");
                return false;
            case TimeBuffer.STATUS_DATA_NOT_IN_BUFFER_ANYMORE:
                Log.w(buf.getOwner().getComponentName(), "data range (" + startSample + "," + (startSample + numSamples) + ") not in buffer anymore");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_SMALL:
                Log.w(buf.getOwner().getComponentName(), "requested duration too small");
                return false;
            case TimeBuffer.STATUS_DURATION_TOO_LARGE:
                Log.w(buf.getOwner().getComponentName(), "requested duration too large");
                return false;
            case TimeBuffer.STATUS_UNKNOWN_DATA:
                Log.w(buf.getOwner().getComponentName(), "requested data is unknown, probably caused by a delayed sensor start");
                return false;
            case TimeBuffer.STATUS_ERROR:
                if (_isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown buffer error occurred");
                return false;
        }

        return true;
    }

    public void Stop()
    {
        if (_isStopping)
            return;

        _isStopping = true;
        _isRunning = false;

        Log.i("shutting down ...");
        try
        {
            Log.i("closing buffer");
            for (TimeBuffer b : _buffer)
                b.close();

            Log.i("closing components");
            for (Component c : _components)
            {
                Log.i("closing " + c.getComponentName());
                //try to close everything individually to free each sensor
                try
                {
                    c.close();
                } catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("closing " + c.getComponentName() + " failed");
                }
            }

            Log.i("waiting for components to terminate");
            _threadPool.awaitTermination(Cons.WAIT_THREAD_TERMINATION, TimeUnit.MICROSECONDS);

            Log.i("shut down completed");
        } catch (Exception e)
        {
            Log.e("Exception in closing framework", e);

            if (_exceptionHandler != null)
                _exceptionHandler.handle("TheFramework.stop()", "Exception in closing framework", e);
            else
                throw new RuntimeException(e);
        } finally
        {
            log();
            _isStopping = false;
        }
    }

    public void invalidateFramework()
    {
        if (isRunning())
        {
            Log.w("Cannot invalidateFramework. Framework still active.");
            return;
        }

        _components.clear();
        _buffer.clear();
        Log.getInstance().invalidate();

        _instance = null;
    }

    public void clear()
    {
        if (isRunning())
        {
            Log.w("Cannot invalidateFramework. Framework still active.");
            return;
        }

        _components.clear();
        _buffer.clear();
        Log.getInstance().clear();
    }

    private void log()
    {
        if (options.logfile.get() != null)
        {
            Log.getInstance().saveToFile(options.logfile.get());
            Log.getInstance().clear();
        }
    }

    public void crash(String location, String message, Throwable e)
    {
        _isRunning = false;

        Log.e(location, message, e);
        log();

        if (_exceptionHandler != null)
        {
            _exceptionHandler.handle(location, message, e);
        } else
        {
            throw new RuntimeException(e);
        }
    }

    public void sync(int bufferID)
    {
        if (!isRunning())
            return;

        _buffer.get(bufferID).sync(_timer.getElapsed());
    }

    public double getTime()
    {
        if (_timer == null)
            return 0;

        return _timer.getElapsed();
    }

    public long getTimeMs()
    {
        if (_timer == null)
            return 0;

        return _timer.getElapsedMs();
    }

    public long getStartTimeMs()
    {
        return _startTime;
    }

    public boolean isRunning()
    {
        return _isRunning;
    }

    public String getVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    public void setExceptionHandler(ExceptionHandler h)
    {
        _exceptionHandler = h;
    }
}
