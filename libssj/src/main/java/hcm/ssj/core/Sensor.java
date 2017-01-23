/*
 * Sensor.java
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

import java.util.ArrayList;

/**
 * Handles connection to sensor device
 */
public abstract class Sensor extends Component {

    private boolean _isConnected = false;
    protected TheFramework _frame;
    protected ArrayList<SensorChannel> _provider = new ArrayList<>();

    public Sensor()
    {
        _frame = TheFramework.getFramework();
    }

    SensorChannel addProvider(SensorChannel p) throws SSJException {
        _provider.add(p);
        return p;
    }

    public ArrayList<SensorChannel> getProviders()
    {
        return _provider;
    }

    @Override
    public void run()
    {
        //if user did not specify a custom priority, use low priority
        android.os.Process.setThreadPriority( (threadPriority == Cons.THREAD_PRIORITY_NORMAL) ? Cons.THREAD_PRIORIIY_LOW : threadPriority );
        _isConnected = false;

        while(!_terminate)
        {
            if(!_isConnected)
            {
                try
                {
                    _isConnected = connect();

                    if (!_isConnected && !_terminate)
                        throw new RuntimeException("unable to connect to device");

                    synchronized (this)
                    {
                        this.notifyAll();
                    }
                }
                catch (Exception e)
                {
                    _frame.crash(this.getComponentName(), "failed to connect to sensor", e);
                }
            }

            try {
                update();
            } catch(Exception e) {
                _frame.crash(this.getComponentName(), "exception in sensor update", e);
            }

            _isConnected = checkConnection();
        }

        try {
            disconnect();
        } catch(Exception e) {
            _frame.crash(this.getComponentName(), "failed to disconnect from sensor", e);
        }
        _isConnected = false;
        _safeToKill = true;
    }

    /**
     * early initialization specific to implementation (called by framework on instantiation)
     */
    protected void init() {}

    /**
     * initialization specific to sensor implementation (called by local thread after framework start)
     */
    protected abstract boolean connect();
    protected boolean checkConnection() {return true;}

    /**
     * called once per frame, can be overwritten
     */
    protected void update()
    {
        try{
            Thread.sleep(Cons.WAIT_SENSOR_CHECK_CONNECT);
        }
        catch(InterruptedException e) {}
    }

    /**
     * called once before termination
     */
    protected abstract void disconnect();

    public abstract int getChannelSize();

    public SensorChannel getChannel(int id) throws SSJException
    {
        SensorChannel provider = null;
        try {
            provider = (SensorChannel) getChannelClass(id).newInstance();
        } catch (InstantiationException e) {
            throw new SSJException(e);
        } catch (IllegalAccessException e) {
            throw new SSJException(e);
        }

        return provider;
    }

    public abstract Class getChannelClass(int id) throws SSJException;

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
