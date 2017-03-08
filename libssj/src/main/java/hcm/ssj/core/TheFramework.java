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

import android.os.SystemClock;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import hcm.ssj.BuildConfig;
import hcm.ssj.R;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.file.LoggingConstants;

/**
 * Created by Johnny on 05.03.2015.
 */
public class TheFramework
{

    public class Options extends OptionList
    {
        public final Option<Integer> countdown = new Option<>("countdown", 3, Integer.class, "");
        public final Option<Float> bufferSize = new Option<>("bufferSize", 2.f, Float.class, "");
        public final Option<Float> waitThreadKill = new Option<>("waitThreadKill", 30f, Float.class, "How long to wait for threads to finish on pipeline shutdown");
        public final Option<Float> waitSensorConnect = new Option<>("waitSensorConnect", 5.f, Float.class, "How long to wait for a sensor to connect");

        public final Option<String> master = new Option<>("master", null, String.class, "enter IP address of master pipeline (leave empty if this is the master)");

        public final Option<Integer> startSyncPort = new Option<>("startSyncPort", 0, Integer.class, "set port for synchronizing pipeline start over network (0 = disabled)"); //55100
        public final Option<Integer> clockSyncPort = new Option<>("clockSyncPort", 0, Integer.class, "set port for synchronizing pipeline clock over network (0 = disabled)"); //55101
        public final Option<Float> clockSyncInterval = new Option<>("clockSyncInterval", 1.0f, Float.class, "define time between clock sync attempts");

        public final Option<Boolean> log = new Option<>("log", false, Boolean.class, "write system log to file");
        public final Option<String> logpath = new Option<>("logpath", LoggingConstants.SSJ_EXTERNAL_STORAGE + File.separator + "[time]", String.class, "location of log file");
        public final Option<Log.Level> loglevel = new Option<>("loglevel", Log.Level.VERBOSE, Log.Level.class, "show all logs >= level");
        public final Option<Double> logtimeout = new Option<>("logtimeout", 5.0, Double.class, "ignore repeated entries < timeout");

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

    private long _startTime = 0; //virtual clock
    private long _startTimeSystem = 0; //real clock
    private long _createTime = 0; //real clock
    private long _timeOffset = 0;
    private ClockSync _clockSync;

    ThreadPool _threadPool;
    ExceptionHandler _exceptionHandler = null;

    //components
    protected HashSet<Component> _components = new HashSet<>();

    //buffers
    protected ArrayList<TimeBuffer> _buffer = new ArrayList<>();

    protected static TheFramework _instance = null;

    private TheFramework()
    {
        //configure logger
        Log.getInstance().setFramework(this);
        resetCreateTime();

        int coreThreads = Runtime.getRuntime().availableProcessors();
        _threadPool = new ThreadPool(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        Log.i(SSJApplication.getAppContext().getString(R.string.name_long) + " v" + getVersion());
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

    public void start()
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

            if (options.startSyncPort.get() != 0)
            {
                if (options.master.get() == null)
                    ClockSync.sendStartSignal(options.startSyncPort.get());
                else
                    ClockSync.listenForStartSignal(options.startSyncPort.get());
            }

            _startTimeSystem = System.currentTimeMillis();
            _startTime = SystemClock.elapsedRealtime();
            _isRunning = true;
            Log.i("pipeline started");

            //start clock sync
            if (options.clockSyncPort.get() != 0) {
                _clockSync = new ClockSync(options.master.get() == null, InetAddress.getByName(options.master.get()), options.clockSyncPort.get(), (int)(options.clockSyncInterval.get() * 1000));
                _threadPool.execute(_clockSync);
            }
        }
        catch (Exception e)
        {
            crash("framework start", "error starting pipeline", e);
        }
    }

    public SensorChannel addSensor(Sensor s, SensorChannel c) throws SSJException
    {
        s.addChannel(c);
        c.setSensor(s);

        s.init();
        c.init();

        int dim = c.getSampleDimension();
        double sr = c.getSampleRate();
        int bytesPerValue = c.getSampleBytes();
        Cons.Type type = c.getSampleType();

        //add output buffer
        TimeBuffer buf = new TimeBuffer(options.bufferSize.get(), sr, dim, bytesPerValue, type, c);
        _buffer.add(buf);
        int buffer_id = _buffer.size() - 1;
        c.setBufferID(buffer_id);

        c.setup();

        _components.add(s);
        _components.add(c);

        return c;
    }

    public Provider addTransformer(Transformer t, Provider source, double frame, double delta) throws SSJException
    {
        Provider[] sources = {source};
        return addTransformer(t, sources, frame, delta);
    }

    public Provider addTransformer(Transformer t, Provider[] sources, double frame, double delta) throws SSJException
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

    public void addConsumer(Consumer c, Provider source, double frame, double delta) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, frame, delta);
    }

    public void addConsumer(Consumer c, Provider[] sources, double frame, double delta) throws SSJException {
        c.setup(sources, frame, delta);
        _components.add(c);
    }

    public void addConsumer(Consumer c, Provider source, EventChannel channel) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, channel);
    }

    public void addConsumer(Consumer c, Provider[] sources, EventChannel channel) throws SSJException {
        c.setup(sources);
        c.addEventChannelIn(channel);
        _components.add(c);
    }

    public void registerEventListener(Component c, Component source)
    {
        registerEventListener(c, source.getEventChannelOut());
    }

    public void registerEventListener(Component c, EventChannel channel)
    {
        _components.add(c);
        c.addEventChannelIn(channel);
    }

    public void registerEventListener(Component c, EventChannel[] channels)
    {
        _components.add(c);
        for(EventChannel ch : channels)
            c.addEventChannelIn(ch);
    }

    public EventChannel registerEventProvider(Component c)
    {
        _components.add(c);
        return c.getEventChannelOut();
    }

    /**
     * Adds an unspecific component to the framework.
     * No initialization is performed and no buffers are allocated.
     *
     * @param c the component to be added
     * @deprecated use registerEventProvider() or registerEventListener() instead
     */
    @Deprecated
    public void addComponent(Component c)
    {
        _components.add(c);
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

    public void stop()
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
                    Log.e("closing " + c.getComponentName() + " failed", e);
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
            writeLogFile();
            _isStopping = false;
        }
    }

    /**
     * Invalidates framework instance and clear all local content
     */
    public void release()
    {
        if (isRunning())
        {
            Log.w("Cannot release. Framework still active.");
            return;
        }

        clear();
        _instance = null;
    }

    /**
     * Clears all local references but does not invalidate instance
     */
    public void clear()
    {
        if (isRunning())
        {
            Log.w("Cannot clear. Framework still active.");
            return;
        }

        for (Component c : _components)
            c.clear();

        _components.clear();
        _buffer.clear();
        Log.getInstance().clear();
        _startTime = 0;
    }

    public void resetCreateTime()
    {
        _createTime = System.currentTimeMillis();
    }

    private void writeLogFile()
    {
        if (options.log.get())
        {
            Log.getInstance().saveToFile(options.logpath.parseWildcards());
        }
    }

    public void crash(String location, String message, Throwable e)
    {
        _isRunning = false;

        Log.e(location, message, e);
        writeLogFile();

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

        _buffer.get(bufferID).sync(getTime());
    }

    /**
     * @return elapsed time since start of the pipeline
     */
    public double getTime()
    {
        return getTimeMs() / 1000.0;
    }

    /**
     * @return elapsed time since start of the pipeline
     */
    public long getTimeMs()
    {
        if (_startTime == 0)
            return 0;

        return SystemClock.elapsedRealtime() - _startTime + _timeOffset;
    }

    void adjustTime(long offset)
    {
        _timeOffset += offset;
    }

    /**
     * @return system time at which the pipeline started
     */
    public long getStartTimeMs()
    {
        return _startTimeSystem;
    }

    /**
     * @return system time at which the framework was created
     */
    public long getCreateTimeMs()
    {
        return _createTime;
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
