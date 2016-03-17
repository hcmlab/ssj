/*
 * AndroidSensorTest.java
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

package hcm.ssj;

import android.app.Application;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorProvider;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.androidSensor.transformer.AvgVar;
import hcm.ssj.androidSensor.transformer.Count;
import hcm.ssj.androidSensor.transformer.Distance;
import hcm.ssj.androidSensor.transformer.Median;
import hcm.ssj.androidSensor.transformer.MinMax;
import hcm.ssj.androidSensor.transformer.Progress;
import hcm.ssj.core.Log;
import hcm.ssj.core.TheFramework;
import hcm.ssj.test.Logger;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class AndroidSensorTest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 1000 * 10;//2 * 60 * 1000;

    /**
     *
     */
    public AndroidSensorTest()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testSensors() throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.i("maxMemory: " + Long.toString(maxMemory));


        //test for every sensor type
//        for (SensorType type : SensorType.values())
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize = 10.0f;
            //sensor
            AndroidSensor sensor = new AndroidSensor(SensorType.LIGHT);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, sensorProvider, 1, 0);
            //start framework
            frame.Start();
            //run test
            long end = System.currentTimeMillis() + TEST_LENGTH;
            try
            {
                while (System.currentTimeMillis() < end)
                {
                    Thread.sleep(1);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            frame.Stop();
            frame.clear();
        }
    }

    /**
     * @throws Exception
     */
    public void testMinMax() throws Exception
    {
        //test for a few setups
        boolean[][] options = {
                {true, true},
                {true, false},
                {false, true}
        };
        for (boolean[] option : options)
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize = 10.0f;
            //create providers
            SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
            AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
            for (int i = 0; i < sensorTypes.length; i++)
            {
                //sensor
                AndroidSensor sensor = new AndroidSensor(sensorTypes[i]);
                frame.addSensor(sensor);
                //provider
                AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
                sensor.addProvider(sensorProvider);
                sensorProviders[i] = sensorProvider;

            }
            //transformer
            MinMax transformer = new MinMax();
            transformer.options.min = option[0];
            transformer.options.max = option[1];
            frame.addTransformer(transformer, sensorProviders, 1, 0);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, transformer, 1, 0);
            //start framework
            frame.Start();
            //run test
            long end = System.currentTimeMillis() + TEST_LENGTH;
            try
            {
                while (System.currentTimeMillis() < end)
                {
                    Thread.sleep(1);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            frame.Stop();
            frame.clear();
        }
    }

    /**
     * @throws Exception
     */
    public void testAvgVar() throws Exception
    {
        //test for a few setups
        boolean[][] options = {
                {true, true},
                {true, false},
                {false, true}
        };
        for (boolean[] option : options)
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize = 10.0f;
            //create providers
            SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
            AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
            for (int i = 0; i < sensorTypes.length; i++)
            {
                //sensor
                AndroidSensor sensor = new AndroidSensor(sensorTypes[i]);
                frame.addSensor(sensor);
                //provider
                AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
                sensor.addProvider(sensorProvider);
                sensorProviders[i] = sensorProvider;

            }
            //transformer
            AvgVar transformer = new AvgVar();
            transformer.options.avg = option[0];
            transformer.options.var = option[1];
            frame.addTransformer(transformer, sensorProviders, 1, 0);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, transformer, 1, 0);
            //start framework
            frame.Start();
            //run test
            long end = System.currentTimeMillis() + TEST_LENGTH;
            try
            {
                while (System.currentTimeMillis() < end)
                {
                    Thread.sleep(1);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            frame.Stop();
            frame.clear();
        }
    }

    /**
     * @throws Exception
     */
    public void testProgressDistanceCount() throws Exception
    {
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize = 10.0f;
        //create providers
        SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
        AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++)
        {
            //sensor
            AndroidSensor sensor = new AndroidSensor(sensorTypes[i]);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            sensorProviders[i] = sensorProvider;

        }
        //transformer
        Progress transformer1 = new Progress();
        frame.addTransformer(transformer1, sensorProviders, 1, 0);
        Distance transformer2 = new Distance();
        frame.addTransformer(transformer2, transformer1, 1, 0);
        Count transformer3 = new Count();
        frame.addTransformer(transformer3, transformer2, 1, 0);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transformer3, 1, 0);
        //start framework
        frame.Start();
        //run test
        long end = System.currentTimeMillis() + TEST_LENGTH;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        frame.Stop();
        frame.clear();
    }

    /**
     * @throws Exception
     */
    public void testMedian() throws Exception
    {
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize = 10.0f;
        //create providers
        SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
        AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++)
        {
            //sensor
            AndroidSensor sensor = new AndroidSensor(sensorTypes[i]);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            sensorProviders[i] = sensorProvider;

        }
        //transformer
        Median transformer = new Median();
        frame.addTransformer(transformer, sensorProviders, 1, 0);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transformer, 1, 0);
        //start framework
        frame.Start();
        //run test
        long end = System.currentTimeMillis() + TEST_LENGTH;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        frame.Stop();
        frame.clear();
    }
}
