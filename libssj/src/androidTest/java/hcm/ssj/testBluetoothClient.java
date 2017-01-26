/*
 * testBluetoothClient.java
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
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.ioput.BluetoothConnection;
import hcm.ssj.ioput.BluetoothEventWriter;
import hcm.ssj.ioput.BluetoothWriter;
import hcm.ssj.test.Logger;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class testBluetoothClient extends ApplicationTestCase<Application> {

    String _name = "BLClient";

    public testBluetoothClient() {
        super(Application.class);
    }

    public void test() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.set(10.0f);

        AndroidSensor sensor = new AndroidSensor();
        sensor.options.sensorType.set(SensorType.ACCELEROMETER);

        AndroidSensorChannel acc = new AndroidSensorChannel();
        acc.options.sampleRate.set(50);
        frame.addSensor(sensor,acc);

        AndroidSensor sensor2 = new AndroidSensor();
        sensor2.options.sensorType.set(SensorType.GRAVITY);
        frame.addSensor(sensor2);
        AndroidSensorChannel gyr = new AndroidSensorChannel();
        gyr.options.sampleRate.set(50);
        frame.addSensor(sensor2,gyr);

        Logger dummy = new Logger();
        dummy.options.reduceNum.set(true);
        frame.addConsumer(dummy, acc, 1.0, 0);

        Logger dummy2 = new Logger();
        dummy2.options.reduceNum.set(true);
        frame.addConsumer(dummy2, gyr, 1.0, 0);

        BluetoothWriter blw = new BluetoothWriter();
        blw.options.connectionType.set(BluetoothConnection.Type.CLIENT);
        blw.options.serverName.set("HCM-Johnny-Phone");
        blw.options.connectionName.set("stream");
        frame.addConsumer(blw, new Provider[]{acc, gyr}, 1.0, 0);

        FloatsEventSender fes = new FloatsEventSender();
        frame.addConsumer(fes, acc, 1.0, 0);
        EventChannel ch = fes.getEventChannelOut();

        BluetoothEventWriter blew = new BluetoothEventWriter();
        blew.options.connectionType.set(BluetoothConnection.Type.CLIENT);
        blew.options.serverName.set("HCM-Johnny-Phone");
        blew.options.connectionName.set("event");
        frame.registerEventListener(blew, ch);

        try {
            frame.Start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + 1 * 60 * 1000)
                    break;

                Thread.sleep(1);
            }

            frame.Stop();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Log.i("test finished");
    }
}