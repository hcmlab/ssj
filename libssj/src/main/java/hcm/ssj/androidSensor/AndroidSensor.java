/*
 * AndroidSensor.java
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

package hcm.ssj.androidSensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import hcm.ssj.core.SSJApplication;

/**
 * Standard connection for android sensors.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class AndroidSensor extends hcm.ssj.core.Sensor
{
    /**
     * All options for the sensor
     */
    public class Options
    {
        /**
         * According to documentation, the sensor will usually sample values
         * at a higher rate than the one specified.<p>
         * The delay is declared in microseconds or as a constant value.<br>
         * Every value above 3 will be processed as microseconds.
         * <li>SENSOR_DELAY_FASTEST = 0 = 0µs</li>
         * <li>SENSOR_DELAY_GAME = 1 = 20000µs</li>
         * <li>SENSOR_DELAY_UI = 2 = 66667µs</li>
         * <li>SENSOR_DELAY_NORMAL = 3 = 200000µs</li>
         */
        public int sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
    }

    public Options options = new Options();
    private SensorManager mSensorManager;
    private Sensor mSensor;
    protected SensorListener listener;
    private SensorType sensorType;

    /**
     * @param sensorType SensorType
     */
    public AndroidSensor(SensorType sensorType)
    {
        this.sensorType = sensorType;
        _name = "SSJ_sensor_" + this.sensorType.getName();
        listener = new SensorListener(this.sensorType);
        mSensorManager = (SensorManager) SSJApplication.getAppContext().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(this.sensorType.getType());
        if (mSensor == null)
        {
            Log.e("SSJ_SensorConnection", this.sensorType.getName() + " not found on device");
        }
    }

    /**
     *
     */
    @Override
    protected void connect()
    {
        if (mSensor != null)
        {
            mSensorManager.registerListener(listener, mSensor, options.sensorDelay);
        }
    }

    /**
     *
     */
    @Override
    protected void disconnect()
    {
        if (mSensor != null)
        {
            mSensorManager.unregisterListener(listener);
        }
    }

    /**
     * @return SensorData
     */
    protected SensorData getData()
    {
        return listener.getData();
    }

    /**
     * @return SensorType
     */
    protected SensorType getSensorType()
    {
        return sensorType;
    }
}

