/*
 * IOputTest.java
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Intensity;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.Cons;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.ioput.BluetoothChannel;
import hcm.ssj.ioput.BluetoothConnection;
import hcm.ssj.ioput.BluetoothEventReader;
import hcm.ssj.ioput.BluetoothEventWriter;
import hcm.ssj.ioput.BluetoothReader;
import hcm.ssj.ioput.BluetoothWriter;
import hcm.ssj.ioput.SocketChannel;
import hcm.ssj.ioput.SocketEventWriter;
import hcm.ssj.ioput.SocketReader;
import hcm.ssj.test.EventLogger;
import hcm.ssj.test.Logger;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IOputTest {

    @Test
    public void testBLClient() throws Exception
    {
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        AndroidSensor sensor = new AndroidSensor();

        AndroidSensorChannel acc = new AndroidSensorChannel();
        acc.options.sensorType.set(SensorType.ACCELEROMETER);
        acc.options.sampleRate.set(50);
        frame.addSensor(sensor,acc);

        AndroidSensor sensor2 = new AndroidSensor();
        AndroidSensorChannel gyr = new AndroidSensorChannel();
        gyr.options.sensorType.set(SensorType.GRAVITY);
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
            frame.start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + TestHelper.DUR_TEST_LONG)
                    break;

                Thread.sleep(1);
            }

            frame.stop();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Log.i("test finished");
    }

    @Test
    public void testBLServer() throws Exception
    {
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        BluetoothReader blr = new BluetoothReader();
        blr.options.connectionType.set(BluetoothConnection.Type.SERVER);
        blr.options.connectionName.set("stream");

        BluetoothChannel acc = new BluetoothChannel();
        acc.options.channel_id.set(0);
        acc.options.dim.set(3);
        acc.options.bytes.set(4);
        acc.options.type.set(Cons.Type.FLOAT);
        acc.options.sr.set(50.);
        acc.options.num.set(50);
        frame.addSensor(blr,acc);

        BluetoothChannel gyr = new BluetoothChannel();
        gyr.options.channel_id.set(1);
        gyr.options.dim.set(3);
        gyr.options.bytes.set(4);
        gyr.options.type.set(Cons.Type.FLOAT);
        gyr.options.sr.set(50.);
        gyr.options.num.set(50);
        frame.addSensor(blr,gyr);

        Logger dummy = new Logger();
        dummy.options.reduceNum.set(true);
        frame.addConsumer(dummy, acc, 1.0, 0);

        Logger dummy2 = new Logger();
        dummy2.options.reduceNum.set(true);
        frame.addConsumer(dummy2, gyr, 1.0, 0);

        BluetoothEventReader bler = new BluetoothEventReader();
        bler.options.connectionType.set(BluetoothConnection.Type.SERVER);
        bler.options.connectionName.set("event");
        EventChannel ch = frame.registerEventProvider(bler);

        EventLogger evlog = new EventLogger();
        frame.registerEventListener(evlog, ch);

        try {
            frame.start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + TestHelper.DUR_TEST_LONG)
                    break;

                Thread.sleep(1);
            }

            frame.stop();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Log.i("test finished");
    }

    @Test
    public void testSocketEventWriter() throws Exception
    {
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        Microphone mic = new Microphone();
        AudioChannel audio = new AudioChannel();
        audio.options.sampleRate.set(16000);
        audio.options.scale.set(true);
        frame.addSensor(mic,audio);

        Intensity energy = new Intensity();
        frame.addTransformer(energy, audio, 1.0, 0);

        FloatsEventSender evs = new FloatsEventSender();
        evs.options.mean.set(true);
        frame.addConsumer(evs, energy, 1.0, 0);
        EventChannel channel = evs.getEventChannelOut();

        EventLogger log = new EventLogger();
        frame.registerEventListener(log, channel);

        SocketEventWriter sock = new SocketEventWriter();
        sock.options.port.set(34300);
        sock.options.ip.set("192.168.0.101");
        frame.registerEventListener(sock, channel);

        try {
            frame.start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + 1 * 10 * 1000)
                    break;

                Thread.sleep(1);
            }

            frame.stop();
            frame.clear();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testSocketReader() throws Exception
    {
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        SocketReader sock = new SocketReader();
        sock.options.port.set(7777);
        sock.options.ip.set("192.168.0.104");
        sock.options.type.set(Cons.SocketType.TCP);

        SocketChannel data = new SocketChannel();
        data.options.dim.set(2);
        data.options.bytes.set(4);
        data.options.type.set(Cons.Type.FLOAT);
        data.options.sr.set(50.);
        data.options.num.set(10);
        frame.addSensor(sock,data);

        Logger log = new Logger();
        frame.addConsumer(log, data, 0.2, 0);

        try {
            frame.start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + 10 * 60 * 1000)
                    break;

                Thread.sleep(1);
            }

            frame.stop();
            frame.clear();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}