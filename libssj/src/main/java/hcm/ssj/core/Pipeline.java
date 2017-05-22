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
 * Main class for creating and interfacing with SSJ pipelines.
 * Holds logic responsible for the setup and execution of pipelines.
 */
public class Pipeline
{

    public class Options extends OptionList
    {
		/** duration of pipeline start-up phase. Default: 3 */
        public final Option<Integer> countdown = new Option<>("countdown", 3, Integer.class, "duration of pipeline start-up phase");
		/** size of all inter-component buffers (in seconds). Default: 2.0 */
        public final Option<Float> bufferSize = new Option<>("bufferSize", 2.f, Float.class, "size of all inter-component buffers (in seconds)");
		/** How long to wait for threads to finish on pipeline shutdown. Default: 30.0 */
        public final Option<Float> waitThreadKill = new Option<>("waitThreadKill", 30f, Float.class, "How long to wait for threads to finish on pipeline shutdown");
		/** How long to wait for a sensor to connect. Default: 5.0 */
        public final Option<Float> waitSensorConnect = new Option<>("waitSensorConnect", 5.f, Float.class, "How long to wait for a sensor to connect");
		/** enter IP address of master pipeline (leave empty if this is the master). Default: null */
        public final Option<String> master = new Option<>("master", null, String.class, "enter IP address of master pipeline (leave empty if this is the master)");
		/** set port for synchronizing pipeline start over network (0 = disabled). Default: 0 */
        public final Option<Integer> startSyncPort = new Option<>("startSyncPort", 0, Integer.class, "set port for synchronizing pipeline start over network (0 = disabled)"); //55100
		/** set port for synchronizing pipeline clock over network (0 = disabled). Default: 0 */
        public final Option<Integer> clockSyncPort = new Option<>("clockSyncPort", 0, Integer.class, "set port for synchronizing pipeline clock over network (0 = disabled)"); //55101
		/** define time between clock sync attempts. Default: 1.0 */
        public final Option<Float> clockSyncInterval = new Option<>("clockSyncInterval", 1.0f, Float.class, "define time between clock sync attempts");
		/** write system log to file. Default: false */
        public final Option<Boolean> log = new Option<>("log", false, Boolean.class, "write system log to file");
		/** location of log file. Default: /sdcard/SSJ/[time] */
        public final Option<String> logpath = new Option<>("logpath", LoggingConstants.SSJ_EXTERNAL_STORAGE + File.separator + "[time]", String.class, "location of log file");
		/** show all logs >= level. Default: VERBOSE */
        public final Option<Log.Level> loglevel = new Option<>("loglevel", Log.Level.VERBOSE, Log.Level.class, "show all logs >= level");
		/** ignore repeated entries < timeout. Default: 5.0 */
        public final Option<Double> logtimeout = new Option<>("logtimeout", 5.0, Double.class, "ignore repeated entries < timeout");

        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    protected String name = "SSJ_Framework";
    protected boolean isRunning = false;
    protected boolean isStopping = false;

    private long startTime = 0; //virtual clock
    private long startTimeSystem = 0; //real clock
    private long createTime = 0; //real clock
    private long timeOffset = 0;
    private ClockSync clockSync;

    ThreadPool threadPool;
    ExceptionHandler exceptionHandler = null;

    private HashSet<Component> components = new HashSet<>();
	private ArrayList<TimeBuffer> buffers = new ArrayList<>();

    protected static Pipeline instance = null;

    private Pipeline()
    {
        //configure logger
        Log.getInstance().setFramework(this);
        resetCreateTime();

        int coreThreads = Runtime.getRuntime().availableProcessors();
        threadPool = new ThreadPool(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        Log.i(SSJApplication.getAppContext().getString(R.string.name_long) + " v" + getVersion());
    }

    public static Pipeline getInstance()
    {
        if (instance == null)
            instance = new Pipeline();

        return instance;
    }

    public static boolean isInstanced()
    {
        return instance != null;
    }

    public void start()
    {
        try
        {
            Log.i("starting pipeline" + '\n' +
                  "\tSSJ v" + getVersion() + '\n' +
                  "\tlocal time: " + Util.getTimestamp(System.currentTimeMillis()));

            Log.i("preparing buffers");
            for (TimeBuffer b : buffers)
                b.reset();

            for (Component c : components)
            {
                Log.i("starting " + c.getComponentName());
                c.reset();
                threadPool.execute(c);
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

            startTimeSystem = System.currentTimeMillis();
            startTime = SystemClock.elapsedRealtime();
            isRunning = true;
            Log.i("pipeline started");

            //start clock sync
            if (options.clockSyncPort.get() != 0) {
                clockSync = new ClockSync(options.master.get() == null, InetAddress.getByName(options.master.get()), options.clockSyncPort.get(), (int)(options.clockSyncInterval.get() * 1000));
                threadPool.execute(clockSync);
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
        buffers.add(buf);
        int buffer_id = buffers.size() - 1;
        c.setBufferID(buffer_id);

        c.setup();

        components.add(s);
        components.add(c);

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
        buffers.add(buf);
        int buffer_id = buffers.size() - 1;
        t.setBufferID(buffer_id);

        components.add(t);
        return t;
    }

    public void addConsumer(Consumer c, Provider source, double frame, double delta) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, frame, delta);
    }

    public void addConsumer(Consumer c, Provider[] sources, double frame, double delta) throws SSJException {
        c.setup(sources, frame, delta);
        components.add(c);
    }

    public void addConsumer(Consumer c, Provider source, EventChannel channel) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, channel);
    }

    public void addConsumer(Consumer c, Provider[] sources, EventChannel channel) throws SSJException {
        c.setup(sources);
        c.addEventChannelIn(channel);
        components.add(c);
    }

    public void registerEventListener(Component c, Component source)
    {
        registerEventListener(c, source.getEventChannelOut());
    }

    public void registerEventListener(Component c, EventChannel channel)
    {
        components.add(c);
        c.addEventChannelIn(channel);
    }

    public void registerEventListener(Component c, EventChannel[] channels)
    {
        components.add(c);
        for(EventChannel ch : channels)
            c.addEventChannelIn(ch);
    }

    public EventChannel registerEventProvider(Component c)
    {
        components.add(c);
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
        components.add(c);
    }

    public void pushData(int buffer_id, Object data, int numBytes)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        buffers.get(buffer_id).push(data, numBytes);
    }

    public void pushZeroes(int buffer_id)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        TimeBuffer buf = buffers.get(buffer_id);

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

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        buffers.get(buffer_id).pushZeroes(num);
    }

    public boolean getData(int buffer_id, Object data, double start_time, double duration)
    {
        if (!isRunning)
        {
            return false;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot read from buffer " + buffer_id + ". Buffer does not exist.");

        TimeBuffer buf = buffers.get(buffer_id);
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
                if (isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown error occurred");
                return false;
        }

        return true;
    }

    public boolean getData(int buffer_id, Object data, int startSample, int numSamples)
    {
        if (!isRunning)
        {
            return false;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("Invalid buffer");

        TimeBuffer buf = buffers.get(buffer_id);
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
                if (isRunning) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown buffer error occurred");
                return false;
        }

        return true;
    }

    public void stop()
    {
        if (isStopping)
            return;

        isStopping = true;
        isRunning = false;

        Log.i("stopping pipeline" + '\n' +
              "\tlocal time: " + Util.getTimestamp(System.currentTimeMillis()));
        try
        {
            Log.i("closing buffer");
            for (TimeBuffer b : buffers)
                b.close();

            Log.i("closing components");
            for (Component c : components)
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
            threadPool.awaitTermination(Cons.WAIT_THREAD_TERMINATION, TimeUnit.MICROSECONDS);

            Log.i("shut down completed");
        } catch (Exception e)
        {
            Log.e("Exception in closing framework", e);

            if (exceptionHandler != null)
                exceptionHandler.handle("TheFramework.stop()", "Exception in closing framework", e);
            else
                throw new RuntimeException(e);
        } finally
        {
            writeLogFile();
            isStopping = false;
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
        instance = null;
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

        for (Component c : components)
            c.clear();

        components.clear();
        buffers.clear();
        Log.getInstance().clear();
        startTime = 0;
    }

    public void resetCreateTime()
    {
        createTime = System.currentTimeMillis();
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
        isRunning = false;

        Log.e(location, message, e);
        writeLogFile();

        if (exceptionHandler != null)
        {
            exceptionHandler.handle(location, message, e);
        } else
        {
            throw new RuntimeException(e);
        }
    }

    public void sync(int bufferID)
    {
        if (!isRunning())
            return;

        buffers.get(bufferID).sync(getTime());
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
        if (startTime == 0)
            return 0;

        return SystemClock.elapsedRealtime() - startTime + timeOffset;
    }

    void adjustTime(long offset)
    {
        timeOffset += offset;
    }

    /**
     * @return system time at which the pipeline started
     */
    public long getStartTimeMs()
    {
        return startTimeSystem;
    }

    /**
     * @return system time at which the framework was created
     */
    public long getCreateTimeMs()
    {
        return createTime;
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public boolean isStopping()
    {
        return isStopping;
    }

    public String getVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    public void setExceptionHandler(ExceptionHandler h)
    {
        exceptionHandler = h;
    }
}
