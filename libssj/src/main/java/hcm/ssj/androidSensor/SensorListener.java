/*
 * SensorListener.java
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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.Arrays;

/**
 * Standard listener for android sensors.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
class SensorListener implements SensorEventListener
{
    private SensorType sensorType;
    private SensorData data;

    /**
     * @param sensorType SensorType
     */
    public SensorListener(SensorType sensorType)
    {
        this.sensorType = sensorType;

        float[] val = new float[this.sensorType.getDataSize()];
        Arrays.fill(val, 0);
        data = new SensorData(val);
    }

    /**
     * @param event SensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        // test for proper event
        if (event.sensor.getType() == sensorType.getType())
        {
            synchronized (this) {
                // set values
                if (event.values != null)
                    data = new SensorData(event.values);
            }
        }
    }

    /**
     * @param sensor   Sensor
     * @param accuracy int
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    /**
     * @return SensorData
     */
    public SensorData getData()
    {
        SensorData d;
        synchronized (this) {
            d = data;
        }
        return d;
    }
}
