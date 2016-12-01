/*
 * AndroidSensor.java
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

package hcm.ssj.androidSensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Standard connection for android sensors.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class AndroidSensor extends hcm.ssj.core.Sensor
{
    /**
     * All options for the sensor
     */
    public class Options extends OptionList
    {
        /**
         * According to documentation, the sensor will usually sample values
         * at a higher rate than the one specified.
         * The delay is declared in microseconds or as a constant value.
         * Every value above 3 will be processed as microseconds.
         * SENSOR_DELAY_FASTEST = 0 = 0µs
         * SENSOR_DELAY_GAME = 1 = 20000µs
         * SENSOR_DELAY_UI = 2 = 66667µs
         * SENSOR_DELAY_NORMAL = 3 = 200000µs
         */
        public final Option<Integer> sensorDelay = new Option<>("sensorDelay", SensorManager.SENSOR_DELAY_FASTEST, Integer.class, "see android documentation");
        public final Option<SensorType> sensorType = new Option<>("sensorType", SensorType.ACCELEROMETER, SensorType.class, "android sensor type");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private SensorManager mSensorManager;
    private Sensor mSensor;
    protected SensorListener listener;
    private SensorType sensorType;
    private final SensorType sensorTypeDefault = SensorType.ACCELEROMETER;
    private boolean initialized = false;

    /**
     *
     */
    public AndroidSensor()
    {
        super();
        _name = "Android";
    }

    /**
     *
     */
    protected void init()
    {
        if (!initialized)
        {
            initialized = true;
            if (options.sensorType.get() == null)
            {
                Log.w("sensor type not set, setting to default " + sensorTypeDefault.getName());
                options.sensorType.set(sensorTypeDefault);
            }
            sensorType = options.sensorType.get();
            _name = this.sensorType.getName();
            listener = new SensorListener(this.sensorType);
            mSensorManager = (SensorManager) SSJApplication.getAppContext().getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(this.sensorType.getType());
            if (mSensor == null)
            {
                Log.e(this.sensorType.getName() + " not found on device");
            }
        }
    }

    /**
     *
     */
    @Override
    protected boolean connect()
    {
        init();
        if (mSensor != null)
        {
            mSensorManager.registerListener(listener, mSensor, options.sensorDelay.get());
            return true;
        }
        return false;
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
        initialized = false;
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

