/*
 * SensorType.java
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

/**
 * Android sensor configurations.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public enum SensorType
{
    ACCELEROMETER("Accelerometer", Sensor.TYPE_ACCELEROMETER, new String[]{"AccX", "AccY", "AccZ"}),
    AMBIENT_TEMPERATURE("AmbientTemperature", Sensor.TYPE_AMBIENT_TEMPERATURE, new String[]{"ATemp °C"}),
    GAME_ROTATION_VECTOR("GameRotationVector", Sensor.TYPE_GAME_ROTATION_VECTOR, new String[]{"GameRotVX", "GameRotVY", "GameRotVZ"}),
    GEOMAGNETIC_ROTATION_VECTOR("GeomagneticRotationVector", Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, new String[]{"GeoRotVX", "GeoRotVY", "GeoRotVZ"}),
    GRAVITY("Gravity", Sensor.TYPE_GRAVITY, new String[]{"GrvX", "GrvY", "GrvZ"}),
    GYROSCOPE("Gyroscope", Sensor.TYPE_GYROSCOPE, new String[]{"GyrX", "GyrY", "GyrZ"}),
    GYROSCOPE_UNCALIBRATED("GyroscopeUncalibrated", Sensor.TYPE_GYROSCOPE_UNCALIBRATED, new String[]{"UGyrX", "UGyrY", "UGyrZ", "UGyrDriftX", "UGyrDriftY", "UGyrDriftZ"}),
    LIGHT("Light", Sensor.TYPE_LIGHT, new String[]{"lx"}),
    LINEAR_ACCELERATION("LinearAcceleration", Sensor.TYPE_LINEAR_ACCELERATION, new String[]{"LAccX", "LAccY", "LAccZ"}),
    MAGNETIC_FIELD("MagneticField", Sensor.TYPE_MAGNETIC_FIELD, new String[]{"MgnFiX", "MgnFiY", "MgnFiZ"}),
    MAGNETIC_FIELD_UNCALIBRATED("MagneticFieldUncalibrated", Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, new String[]{"UMgnFiX", "UMgnFiY", "UMgnFiZ", "UMgnFiBiasX", "UMgnFiBiasY", "UMgnFiBiasZ"}),
    ORIENTATION("Orientation", Sensor.TYPE_ORIENTATION, new String[]{"OriAzi", "OriPitch", "OriRoll"}),
    PRESSURE("Pressure", Sensor.TYPE_PRESSURE, new String[]{"Pressure"}),
    PROXIMITY("Proximity", Sensor.TYPE_PROXIMITY, new String[]{"Proximity"}),
    RELATIVE_HUMIDITY("RelativeHumidity", Sensor.TYPE_RELATIVE_HUMIDITY, new String[]{"Humidity"}),
    ROTATION_VECTOR("RotationVector", Sensor.TYPE_ROTATION_VECTOR, new String[]{"RotVX", "RotVY", "RotVZ", "RotVSc"}),
    STEP_COUNTER("StepCounter", Sensor.TYPE_STEP_COUNTER, new String[]{"Steps"}),
    TEMPERATURE("Temperature", Sensor.TYPE_TEMPERATURE, new String[]{"Temp °C"}),
    HEART_RATE("HeartRate", Sensor.TYPE_HEART_RATE, new String[]{"BPM"});

    final private String name;
    final private int type;
    final private int dataSize;
    final private String[] output;

    /**
     * @param name   String
     * @param type   int
     * @param output String[]
     */
    SensorType(String name, int type, String[] output)
    {
        this.name = name;
        this.type = type;
        this.dataSize = output.length;
        this.output = output;
    }

    /**
     * @return String
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return int
     */
    public int getType()
    {
        return type;
    }

    /**
     * @return int
     */
    public int getDataSize()
    {
        return dataSize;
    }

    /**
     * @return String[]
     */
    public String[] getOutput()
    {
        return output;
    }

    /**
     * @param name String
     * @return SensorType
     */
    protected static SensorType getSensorType(String name)
    {
        try
        {
            return SensorType.valueOf(name);
        } catch (IllegalArgumentException ex)
        {
            for (SensorType sensorType : SensorType.values())
            {
                if (sensorType.getName().equalsIgnoreCase(name))
                {
                    return sensorType;
                }
            }
        }
        return null;
    }
}
