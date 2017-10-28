/*
 * Sensor.java
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

import java.util.ArrayList;

/**
 * Handles connection to sensor device
 */
public abstract class Sensor extends Component {

    private boolean _isConnected = false;
    protected Pipeline _frame;
    protected ArrayList<SensorChannel> _provider = new ArrayList<>();

    public Sensor()
    {
        _frame = Pipeline.getInstance();
    }

    void addChannel(SensorChannel p) throws SSJException {
        _provider.add(p);
    }

    public ArrayList<SensorChannel> getProviders()
    {
        return _provider;
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("SSJ_" + _name);

        //if user did not specify a custom priority, use low priority
        android.os.Process.setThreadPriority( (threadPriority == Cons.THREAD_PRIORITY_NORMAL) ? Cons.THREAD_PRIORIIY_LOW : threadPriority );
        _isConnected = false;

        while(!_terminate)
        {
            if(!_isConnected && !_frame.isStopping())
            {
                try
                {
                    _isConnected = connect();
                    if (!_isConnected) {
                        waitCheckConnect();
                        continue;
                    }

                    synchronized (this)
                    {
                        this.notifyAll();
                    }
                }
                catch (SSJFatalException e) {
                    _frame.error(this.getComponentName(), "failed to connect to sensor", e);
                    _safeToKill = true;
                    return;
                } catch (Exception e) {
                    _frame.error(this.getComponentName(), "failed to connect to sensor", e);
                }
            }

            try {
                update();
            } catch(SSJFatalException e) {
                _frame.error(this.getComponentName(), "exception in sensor update", e);
                _safeToKill = true;
                return;
            } catch(Exception e) {
                _frame.error(this.getComponentName(), "exception in sensor update", e);
            }

            _isConnected = checkConnection();
        }

        try {
            disconnect();
        } catch(Exception e) {
            _frame.error(this.getComponentName(), "failed to disconnect from sensor", e);
        }
        _isConnected = false;
        _safeToKill = true;
    }

    /**
     * early initialization specific to implementation (called by framework on instantiation)
     */
    protected void init() throws SSJException {}

    /**
     * initialization specific to sensor implementation (called by local thread after framework start)
     */
    protected abstract boolean connect() throws SSJFatalException;
    protected boolean checkConnection() {return true;}

    private void waitCheckConnect()
    {
        try{
            Thread.sleep(Cons.SLEEP_ON_COMPONENT_IDLE);
        }
        catch(InterruptedException e) {
            Log.w("thread interrupt");
        }
    }

    /**
     * called once per frame, can be overwritten
     */
    protected void update() throws SSJFatalException
    {
        waitCheckConnect();
    }

    /**
     * called once before termination
     */
    protected abstract void disconnect() throws SSJFatalException;

    public boolean isConnected()
    {
        return _isConnected;
    }

    public void waitForConnection()
    {
            while (!isConnected() && !_terminate)
            {
                try {
                    synchronized (this)
                    {
                        this.wait();
                    }
                } catch (InterruptedException e) {}
            }
    }

    @Override
    public void clear()
    {
        _provider.clear();
        super.reset();
    }
}
