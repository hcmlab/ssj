/*
 * Pipeline.java
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

import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import hcm.ssj.BuildConfig;
import hcm.ssj.R;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileDownloader;
import hcm.ssj.ml.Model;
import hcm.ssj.mobileSSI.SSI;

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
        /** How long to wait for a sensor to connect. Default: 30.0 */
        public final Option<Float> waitSensorConnect = new Option<>("waitSensorConnect", 30.f, Float.class, "How long to wait for a sensor to connect");
        /** Cross-device synchronization (requires network). Default: NONE */
        public final Option<SyncType> sync = new Option<>("sync", SyncType.NONE, SyncType.class, "Cross-device synchronization (requires network).");
        /** enter IP address of host pipeline for synchronization (leave empty if this is the host). Default: null */
        public final Option<String> syncHost = new Option<>("syncHost", null, String.class, "enter IP address of host pipeline for synchronization (leave empty if this is the host)");
        /** set port for synchronizing pipeline over network. Default: 55100 */
        public final Option<Integer> syncPort = new Option<>("syncPort", 0, Integer.class, "port for synchronizing pipeline over network");
        /** define time between clock sync attempts (requires CONTINUOUS sync). Default: 1.0 */
        public final Option<Float> syncInterval = new Option<>("syncInterval", 10.0f, Float.class, "define time between clock sync attempts (requires CONTINUOUS sync)");
        /** write system log to file. Default: false */
        public final Option<Boolean> log = new Option<>("log", false, Boolean.class, "write system log to file");
        /** location of log file. Default: /sdcard/SSJ/[time] */
        public final Option<String> logpath = new Option<>("logpath", FileCons.SSJ_EXTERNAL_STORAGE + File.separator + "[time]", String.class, "location of log file");
        /** show all logs greater or equal than level. Default: VERBOSE */
        public final Option<Log.Level> loglevel = new Option<>("loglevel", Log.Level.VERBOSE, Log.Level.class, "show all logs >= level");
        /** repeated log entries with a duration delta smaller than the timeout value are ignored. Default: 1.0 */
        public final Option<Double> logtimeout = new Option<>("logtimeout", 1.0, Double.class, "ignore repeated entries < timeout");
        /** Shut down pipeline if runtime error is encountered */
        public final Option<Boolean> terminateOnError = new Option<>("terminateOnError", false, Boolean.class, "Shut down pipeline if runtime error is encountered");

        private Options()
        {
            addOptions();
        }
    }

    public enum State
    {
        INACTIVE,
        STARTING,
        RUNNING,
        STOPPING
    }

    public enum SyncType
    {
        NONE,
        START_STOP,
        CONTINUOUS
    }

    public final Options options = new Options();

    protected String name = "SSJ_Framework";
    private State state;

    private long startTime = 0; //virtual clock
    private long startTimeSystem = 0; //real clock
    private long createTime = 0; //real clock
    private long timeOffset = 0;

    private NetworkSync sync = null;

    ThreadPool threadPool = null;
    ExceptionHandler exceptionHandler = null;

    private HashSet<Component> components = new HashSet<>();
    private ArrayList<TimeBuffer> buffers = new ArrayList<>();
    private List<PipelineStateListener> stateListeners = new ArrayList<>();

    private FileDownloader downloader;

    protected static Pipeline instance = null;

    private Pipeline()
    {
        setState(State.INACTIVE);

        //configure logger
        Log.getInstance().setFramework(this);
        resetCreateTime();

        Log.i(SSJApplication.getAppContext().getString(R.string.name_long) + " v" + getVersion());
    }

    /**
     * Retrieve the SSJ pipeline instance.
     * Only one SSJ pipeline instance per application is supported, yet it can contain multiple parallel branches.
     *
     * @return Pipeline the pipeline instance
     */
    public static Pipeline getInstance()
    {
        if (instance == null)
            instance = new Pipeline();

        return instance;
    }

    public OptionList getOptions()
    {
        return options;
    }

    /**
     * Sets the pipeline state and notifies state listeners
     * @param newState target state
     */
    private void setState(final State newState)
    {
        this.state = newState;

        for (final PipelineStateListener stateListener : stateListeners)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    stateListener.stateUpdated(newState);
                }
            }).start();
        }
    }

    /**
     * Adds a new pipeline state listener
     * @param listener listener
     */
    public void registerStateListener(PipelineStateListener listener)
    {
        stateListeners.add(listener);
    }

    /**
     * Removes a pipeline state listener
     * @param listener listener
     */
    public void unregisterStateListener(PipelineStateListener listener)
    {
        stateListeners.remove(listener);
    }

    /**
     * Starts the SSJ pipeline.
     * Automatically resets buffers and component states.
     */
    public void start()
    {
        setState(State.STARTING);

        try
        {
            Log.i("starting pipeline" + '\n' +
                  "\tSSJ v" + getVersion() + '\n' +
                  "\tlocal time: " + Util.getTimestamp(System.currentTimeMillis()));

            int coreThreads = Runtime.getRuntime().availableProcessors();
            threadPool = new ThreadPool(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

            //sync with other pipelines
            if (options.sync.get() != SyncType.NONE) {
                boolean isMaster = (options.syncHost.get() == null) || (options.syncHost.get().isEmpty());
                sync = new NetworkSync(options.sync.get(), isMaster, InetAddress.getByName(options.syncHost.get()), options.syncPort.get(), (int)(options.syncInterval.get() * 1000));
            }

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

            if (options.sync.get() != SyncType.NONE)
            {
                if (options.syncHost.get() == null)
                    NetworkSync.sendStartSignal(options.syncPort.get());
                else
                {
                    Log.i("waiting for start signal from host pipeline ...");
                    sync.waitForStartSignal();
                    if(state != State.STARTING) //cancel startup if something happened while waiting for sync
                        return;
                }
            }

            startTimeSystem = System.currentTimeMillis();
            startTime = SystemClock.elapsedRealtime();

            setState(State.RUNNING);
            Log.i("pipeline started");

            if (options.sync.get() != SyncType.NONE)
            {
                if (options.syncHost.get() != null)
                {
                    Runnable stopper = new Runnable() {
                        @Override
                        public void run()
                        {
                            sync.waitForStopSignal();
                            Log.i("received stop signal from host pipeline");
                            stop();
                        }
                    };
                    threadPool.execute(stopper);
                }
            }
        }
        catch (Exception e)
        {
            error(this.getClass().getSimpleName(), "error starting pipeline, shutting down", e);
            stop();
        }
    }

    /**
     * Adds a sensor with a corresponding channel to the pipeline and sets up the necessary output buffer.
     * Calls init method of sensor and channel before setting up buffer.
     *
     * @param s the Sensor to be added
     * @param c the corresponding channel
     * @return the same SensorChannel which was passed as parameter
     * @throws SSJException thrown is an error occurred when setting up the sensor
     */
    public SensorChannel addSensor(Sensor s, SensorChannel c) throws SSJException
    {
        if(components.contains(c))
        {
            Log.w("Component already added.");
            return c;
        }

        s.addChannel(c);
        c.setSensor(s);

        if(!components.contains(s))
        {
            components.add(s);
            s.init();
        }

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

        components.add(c);

        return c;
    }

    /**
     * Adds a transformer to the pipeline and sets up the necessary output buffer.
     * init method of transformer is called before setting up buffer.
     *
     * @param t the Transformer to be added
     * @param source the component which will provide data to the transformer
     * @return the Transformer which was passed as parameter
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public Provider addTransformer(Transformer t, Provider source) throws SSJException
    {
        Provider[] sources = {source};
        return addTransformer(t, sources, source.getOutputStream().num / source.getOutputStream().sr, 0);
    }

    /**
     * Adds a transformer to the pipeline and sets up the necessary output buffer.
     * init method of transformer is called before setting up buffer.
     *
     * @param t the Transformer to be added
     * @param source the component which will provide data to the transformer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @return the Transformer which was passed as parameter
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public Provider addTransformer(Transformer t, Provider source, double frame) throws SSJException
    {
        Provider[] sources = {source};
        return addTransformer(t, sources, frame, 0);
    }

    /**
     * Adds a transformer to the pipeline and sets up the necessary output buffer.
     * init method of transformer is called before setting up buffer.
     *
     * @param t the Transformer to be added
     * @param source the component which will provide data to the transformer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @param delta the amount of input data which overlaps with the previous window (in seconds). Provided in addition to the primary window ("frame").
     * @return the Transformer which was passed as parameter
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public Provider addTransformer(Transformer t, Provider source, double frame, double delta) throws SSJException
    {
        Provider[] sources = {source};
        return addTransformer(t, sources, frame, delta);
    }

    /**
     * Adds a transformer to the pipeline and sets up the necessary output buffer.
     * init method of transformer is called before setting up buffer.
     *
     * @param t the Transformer to be added
     * @param sources the components which will provide data to the transformer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @param delta the amount of input data which overlaps with the previous window (in seconds). Provided in addition to the primary window ("frame").
     * @return the Transformer which was passed as parameter
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public Provider addTransformer(Transformer t, Provider[] sources, double frame, double delta) throws SSJException
    {
        if(components.contains(t))
        {
            Log.w("Component already added.");
            return t;
        }

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

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param source the component which will provide data to the consumer
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider source) throws SSJException
    {
        Provider[] sources = {source};
        addConsumer(c, sources);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param sources the components which will provide data to the consumer
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider[] sources) throws SSJException
    {
        addConsumer(c, sources, sources[0].getOutputStream().num / sources[0].getOutputStream().sr, 0);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param source the component which will provide data to the consumer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider source, double frame) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, frame, 0);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param source the component which will provide data to the consumer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @param delta the amount of input data which overlaps with the previous window (in seconds). Provided in addition to the primary window ("frame").
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider source, double frame, double delta) throws SSJException {
        Provider[] sources = {source};
        addConsumer(c, sources, frame, delta);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param sources the components which will provide data to the consumer
     * @param frame the size of the data window which is provided every iteration to the transformer (in seconds)
     * @param delta the amount of input data which overlaps with the previous window (in seconds). Provided in addition to the primary window ("frame").
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider[] sources, double frame, double delta) throws SSJException
    {
        if(components.contains(c))
        {
            Log.w("Component already added.");
            return;
        }

        c.setup(sources, frame, delta);
        components.add(c);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param source the component which will provide data to the consumer
     * @param trigger an event channel which acts as a trigger. The consumer will only process data when an event is received.
     *                The data window to be processed is defined by the timing information of the event.
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider source, EventChannel trigger) throws SSJException
    {
        Provider[] sources = {source};
        addConsumer(c, sources, trigger);
    }

    /**
     * Adds a consumer to the pipeline.
     * init method of consumer is called after setting up internal input buffer.
     *
     * @param c the Consumer to be added
     * @param sources the components which will provide data to the consumer
     * @param trigger an event channel which acts as a trigger. The consumer will only process data when an event is received.
     *                The data window to be processed is defined by the timing information of the event.
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addConsumer(Consumer c, Provider[] sources, EventChannel trigger) throws SSJException
    {
        if(components.contains(c))
        {
            Log.w("Component already added.");
            return;
        }

        c.setEventTrigger(trigger);
        c.setup(sources);
        components.add(c);
    }

    /**
     * Adds a model to the pipeline.
     *
     * @param m the Model to be added
     * @throws SSJException thrown is an error occurred when setting up the component
     */
    public void addModel(Model m) throws SSJException {

        if(components.contains(m))
        {
            Log.w("Component already added.");
            return;
        }

        m.setup();
        components.add(m);
    }

    /**
     * Registers a component as a listener to another component's events.
     * Component is also added to the pipeline (if not already there)
     * This component will be notified every time the "source" component pushes an event into its channel
     *
     * @param c the listener
     * @param source the one being listened to
     */
    public void registerEventListener(Component c, Component source)
    {
        registerEventListener(c, source.getEventChannelOut());
    }

    /**
     * Registers a component as a listener to a specific event channel.
     * Component is also added to the pipeline (if not already there)
     * This component will be notified every time a new event is pushed into the channel.
     *
     * @param c the listener
     * @param channel the channel to be listened to
     */
    public void registerEventListener(Component c, EventChannel channel)
    {
        components.add(c);
        c.addEventChannelIn(channel);
    }

    /**
     * Register a component as a listener to multiple event channels.
     * Component is also added to the pipeline (if not already there)
     * This component will be notified every time a new event is pushed a channel.
     *
     * @param c the listener
     * @param channels the channel to be listened to
     */
    public void registerEventListener(Component c, EventChannel[] channels)
    {
        components.add(c);
        for(EventChannel ch : channels)
            c.addEventChannelIn(ch);
    }

    /**
     * Register a component as an event provider.
     * Component is also added to the pipeline (if not already there)
     *
     * @param c the event provider
     * @return the output channel of the component.
     */
    public EventChannel registerEventProvider(Component c)
    {
        components.add(c);
        return c.getEventChannelOut();
    }

    public void registerInFeedbackCollection(Feedback feedback, FeedbackCollection feedbackCollection, int level, FeedbackCollection.LevelBehaviour levelBehaviour)
    {
        components.add(feedback);

        if(feedback._evchannel_in != null)
            feedback._evchannel_in.clear();

        for(EventChannel eventChannel : feedbackCollection._evchannel_in)
        {
            registerEventListener(feedback, eventChannel);
        }

        feedbackCollection.addFeedback(feedback, level, levelBehaviour);
    }

    public void registerInFeedbackCollection(FeedbackCollection feedbackCollection, List<Map<Feedback,FeedbackCollection.LevelBehaviour>> feedbackList)
    {
        feedbackCollection.removeAllFeedbacks();

        for (int level = 0; level < feedbackList.size(); level++)
        {
            for(Map.Entry<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourEntry : feedbackList.get(level).entrySet())
            {
                registerInFeedbackCollection(feedbackLevelBehaviourEntry.getKey(),
                                                                   feedbackCollection,
                                                                   level,
                                                                   feedbackLevelBehaviourEntry.getValue());
            }
        }
    }

    void pushData(int buffer_id, Object data, int numBytes)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        buffers.get(buffer_id).push(data, numBytes);
    }

    void pushZeroes(int buffer_id)
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

    void pushZeroes(int buffer_id, int num)
    {
        if (!isRunning())
        {
            return;
        }

        if (buffer_id < 0 || buffer_id >= buffers.size())
            Log.w("cannot push to buffer " + buffer_id + ". Buffer does not exist.");

        buffers.get(buffer_id).pushZeroes(num);
    }

    boolean getData(int buffer_id, Object data, double start_time, double duration)
    {
        if (!isRunning())
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
                if (isRunning()) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown error occurred");
                return false;
        }

        return true;
    }

    boolean getData(int buffer_id, Object data, int startSample, int numSamples)
    {
        if (!isRunning())
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
                long bufEnd = buf.getPositionAbs() / buf.getBytesPerSample();
                long bufStart = bufEnd - buf.getCapacity() / buf.getBytesPerSample();
                Log.w(buf.getOwner().getComponentName(), "requested data range (" + (startSample - buf.getOffsetSamples()) + "-" + (startSample + numSamples - buf.getOffsetSamples()) + ") not in buffer (" + bufStart + "-" + bufEnd + ") anymore. Consumer/Transformer is probably too slow");
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
                if (isRunning()) //this means that either the framework shut down (in this case the behaviour is normal) or some other error occurred
                    Log.w(buf.getOwner().getComponentName(), "unknown buffer error occurred");
                return false;
        }

        return true;
    }

    /**
     * Stops the pipeline.
     * Closes all buffers and shuts down all components.
     * Also writes log file to sd card (if configured).
     */
    public void stop()
    {
        if (state == State.STOPPING || state == State.INACTIVE)
            return;

        setState(State.STOPPING);

        Log.i("stopping pipeline" + '\n' +
              "\tlocal time: " + Util.getTimestamp(System.currentTimeMillis()));
        try
        {
            if(sync != null)
            {
                if (options.syncHost.get() == null)
                    NetworkSync.sendStopSignal(options.syncPort.get());

                sync.release();
            }

            Log.i("closing buffer");
            for (TimeBuffer b : buffers)
                b.close();

            if(downloader != null)
            {
                Log.i("aborting downloads");
                downloader.terminate();
                downloader = null;
            }

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

            threadPool.shutdown();

            Log.i("waiting for components to terminate");
            if(!threadPool.awaitTermination(Cons.WAIT_THREAD_TERMINATION, TimeUnit.MILLISECONDS))
                threadPool.shutdownNow();

            Log.i("shut down completed");
        }
        catch (InterruptedException e)
        {
            threadPool.shutdownNow();
        }
        catch (Exception e)
        {
            Log.e("Exception in closing framework", e);

            if (exceptionHandler != null)
            {
                exceptionHandler.handle("TheFramework.stop()", "Exception in closing framework", e);
            }
            else
            {
                setState(State.INACTIVE);
                throw new RuntimeException(e);
            }
        } finally
        {
            writeLogFile();
            setState(State.INACTIVE);
        }
    }

    /**
     * Invalidates framework instance and clears all local content
     */
    public void release()
    {
        if (isRunning())
        {
            Log.w("Cannot release. Framework still active.");
            return;
        }

        clear();

        if(downloader != null)
        {
            downloader.terminate();
            downloader = null;
        }

        instance = null;
    }

    /**
     * Clears all buffers, components and internal pipeline state, but does not invalidate instance.
     */
    public void clear()
    {
        if (isRunning())
        {
            Log.w("Cannot clear. Framework still active.");
            return;
        }

        setState(State.INACTIVE);

        for (Component c : components)
            c.clear();

        components.clear();
        buffers.clear();
        stateListeners.clear();
        Log.getInstance().clear();
        startTime = 0;

        if(threadPool != null)
            threadPool.purge();

        SSI.clear();
    }

    /**
     * Executes a runnable using the pipeline's thread pool
     *
     * @param r runnable
     */
    public void executeRunnable(Runnable r)
    {
        threadPool.execute(r);
    }

    /**
     * Resets pipeline "create" timestamp
     */
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

    /**
     * Marks the occurence of an unrecoverable error, reports it and attempts to shut down the pipeline
     * @param location where the error occurred
     * @param message error message
     * @param e exception which caused the error, can be null
     */
    void error(String location, String message, Throwable e)
    {
        Log.e(location, message, e);
        writeLogFile();

        if (exceptionHandler != null)
        {
            exceptionHandler.handle(location, message, e);
        }

        if(options.terminateOnError.get() && isRunning())
            stop();
    }

    void sync(int bufferID)
    {
        if (!isRunning())
            return;

        buffers.get(bufferID).sync(getTime());
    }

    /**
     * Downloads multiple files to the sd card
     * @param fileNames array containing the names of the files to download
     * @param from remote path from which to download files
     * @param to path to download to
     * @param wait if true, function blocks until download is finished
     */
    public void download(String[] fileNames, String from, String to, boolean wait)
    {
        if(downloader == null || downloader.isTerminating())
            downloader = new FileDownloader();

        FileDownloader.Task t = null;
        for(String fileName : fileNames)
        {
            t = downloader.addToQueue(fileName, from, to);
            if(t == null)
                return;
        }

        if(!downloader.isAlive())
            downloader.start();

        if(wait) downloader.wait(t);
    }

    /**
     * Downloads a file to the sd card
     *
     * @param fileName Name of the file
     * @param from Remote path from which to download file
     * @param to Path to download to
     * @param wait If true, function blocks until download is finished
     * @throws IOException IO Exception
     */
    public void download(String fileName, String from, String to, boolean wait) throws IOException
    {
        if(fileName == null || fileName.isEmpty()
        || from == null || from.isEmpty()
        || to == null || to.isEmpty())
            throw new IOException("download source or destination is empty");

        if(downloader == null || downloader.isTerminating())
            downloader = new FileDownloader();

        FileDownloader.Task t = downloader.addToQueue(fileName, from, to);
        if(t == null)
            return;

        if(!downloader.isAlive())
            downloader.start();

        if(wait) downloader.wait(t);
    }

    /**
     * @return elapsed time since start of the pipeline (in seconds)
     */
    public double getTime()
    {
        return getTimeMs() / 1000.0;
    }

    /**
     * @return elapsed time since start of the pipeline (in milliseconds)
     */
    public long getTimeMs()
    {
        if (startTime == 0)
            return 0;

        return SystemClock.elapsedRealtime() - startTime + timeOffset;
    }

    void adjustTime(long offset)
    {
        Log.d("adjusting clock by " + offset + " ms");
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
     * @return system time at which the pipeline instance was created.
     * Timestamp can be modified using "resetCreateTime()"
     */
    public long getCreateTimeMs()
    {
        return createTime;
    }

    /**
     * @return true if SSJ has already been instanced, false otherwise
     */
    public static boolean isInstanced()
    {
        return instance != null;
    }

    /**
     * @return current state of the framework
     */
    public State getState()
    {
        return state;
    }

    /**
     * @return true if pipeline is running, false otherwise
     */
    public boolean isRunning()
    {
        return state == State.RUNNING;
    }

    /**
     * @return true if pipeline shut down has been initiated, false otherwise
     */
    public boolean isStopping()
    {
        return state == State.STOPPING;
    }

    /**
     * @return current SSJ version
     */
    public static String getVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Sets a handler for SSJ errors and crashes.
     * The handler is notified every time an SSJException happens at runtime.
     *
     * @param h a class which implements the ExceptionHandler interface
     */
    public void setExceptionHandler(ExceptionHandler h)
    {
        exceptionHandler = h;
    }
}
