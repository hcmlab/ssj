/*
 * MobileSSITest.java
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

package hcm.ssj;

import android.app.Application;
import android.content.res.AssetManager;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.TheFramework;
import hcm.ssj.mobileSSI.MobileSSIConsumer;
import hcm.ssj.test.Logger;

import static java.lang.System.loadLibrary;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class MobileSSITest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 1000 * 35;//2 * 60 * 1000;

    public native void startSSI(String path, AssetManager am, boolean extractFiles);
    public native void stopSSI();

    /**
     *
     */
    public MobileSSITest()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testSensors() throws Exception
    {

        loadLibrary("ssiframe");
        loadLibrary("ssievent");
        loadLibrary("ssiioput");
        loadLibrary("ssiandroidsensors");
        loadLibrary("ssimodel");
        loadLibrary("ssisignal");
        loadLibrary("ssissjSensor");
        loadLibrary("android_xmlpipe");


        //test for every sensor type
        SensorType acc = SensorType.GYROSCOPE;
        SensorType mag = SensorType.ACCELEROMETER;

        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.set(10.0f);
        //sensor
        AndroidSensor sensor = new AndroidSensor();
        sensor.options.sensorType.set(acc);
        AndroidSensor s2 = new AndroidSensor();
        s2.options.sensorType.set(mag);


        frame.addSensor(s2);

        //channel
        AndroidSensorChannel sensorChannel = new AndroidSensorChannel();
        AndroidSensorChannel sensorPmag = new AndroidSensorChannel();

        frame.addSensor(sensor,sensorChannel);
        frame.addSensor(s2,sensorPmag);

        //logger
        Logger log = new Logger();
        frame.addConsumer(log, sensorChannel, 1, 0);
        frame.addConsumer(log, sensorPmag, 1, 0);

        MobileSSIConsumer mobileSSI2 =new MobileSSIConsumer();
        MobileSSIConsumer mobileSSI = new MobileSSIConsumer();

        mobileSSI.setId(1);
        mobileSSI2.setId(2);

        frame.addConsumer(mobileSSI, sensorChannel, 1, 0);
        frame.addConsumer(mobileSSI2, sensorPmag, 1, 0);
        //start framework

        startSSI("/sdcard/android_xmlpipe", null, false);
        Thread.sleep(3100);
        frame.Start();

        mobileSSI.setSensor(sensorChannel, null, mobileSSI.getId());
        mobileSSI2.setSensor(sensorPmag, null, mobileSSI2.getId());
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
        stopSSI();
        frame.Stop();
        frame.release();


    }


}
