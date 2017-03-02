/*
 * testSockets.java
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

import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.Cons;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.TheFramework;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.ioput.SocketChannel;
import hcm.ssj.ioput.SocketEventWriter;
import hcm.ssj.ioput.SocketReader;
import hcm.ssj.praat.Intensity;
import hcm.ssj.test.EventLogger;
import hcm.ssj.test.Logger;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class testSockets extends ApplicationTestCase<Application> {
    public testSockets() {
        super(Application.class);
    }

    public void test() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
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
        sock.options.port = 34300;
        sock.options.ip = "192.168.0.101";
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

    public void test2() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
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