/*
 * Sensor.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

/**
 * Handles connection to sensor device
 */
public abstract class Sensor extends Component {

    protected boolean _isConnected = false;
    protected TheFramework _frame;

    public Sensor()
    {
        _frame = TheFramework.getFramework();
    }

    public SensorProvider addProvider(SensorProvider p)
    {
        _frame.addSensorProvider(this, p);
        return p;
    }

    @Override
    public void run()
    {
        //if user did not specify a custom priority, use low priority
        android.os.Process.setThreadPriority( (threadPriority == Cons.THREAD_PRIORITY_NORMAL) ? Cons.THREAD_PRIORIIY_LOW : threadPriority );

        try {
            connect();
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "failed to connect to sensor", e);
        }
        _isConnected = true;

        synchronized (this)
        {
            this.notifyAll();
        }

        while(!_terminate)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        try {
            disconnect();
        } catch(Exception e) {
            _frame.crash(this.getClass().getSimpleName(), "failed to disconnect from sensor", e);
        }
        _isConnected = false;
        _safeToKill = true;
    }

    /**
     * initialization specific to sensor implementation (called by local thread after framework start)
     */
    protected abstract void connect();

    /**
     * called once before termination
     */
    protected abstract void disconnect();

    public boolean isConnected()
    {
        return _isConnected;
    }

    public void waitForConnection()
    {
        synchronized (this)
        {
            while (!isConnected() && !_terminate)
            {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

}
