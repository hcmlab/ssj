/*
 * testLinker.java
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

package hcm.ssj.creator;

import android.app.Application;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.TheFramework;
import hcm.ssj.creator.core.Pipeline;
import hcm.ssj.creator.core.SSJDescriptor;
import hcm.ssj.test.Logger;

/**
 * Created by Frank Gaibler on 10.03.2016.
 */
public class testLinker extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 2 * 5 * 1000;

    /**
     *
     */
    public testLinker()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testBuildAndLink() throws Exception
    {
        //scan content
        SSJDescriptor descriptor = SSJDescriptor.getInstance();
//        Builder.getInstance().scan(this.getContext());
        System.out.println(descriptor.sensors.get(0));
        System.out.println(descriptor.sensorChannels.get(0));
        System.out.println(descriptor.consumers.get(0));
        //
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.set(2.0f);
        Pipeline pipeline = Pipeline.getInstance();
        //select classes
        Sensor sensor = null;
        for (Class clazz : descriptor.sensors)
        {
            if (clazz.equals(AndroidSensor.class))
            {
                sensor = (Sensor) SSJDescriptor.instantiate(clazz);
                break;
            }
        }
        SensorChannel sensorChannel = null;
        if (sensor != null)
        {
            for (Class clazz : descriptor.sensorChannels)
            {
                if (clazz.equals(AndroidSensorChannel.class))
                {
                    sensorChannel = (SensorChannel) SSJDescriptor.instantiate(clazz);
                    break;
                }
            }
        }
        Consumer consumer = null;
        if (sensorChannel != null)
        {
            pipeline.add(sensor);
            pipeline.add(sensorChannel);
            pipeline.addProvider(sensor, sensorChannel);
            for (Class clazz : descriptor.consumers)
            {
                if (clazz.equals(Logger.class))
                {
                    consumer = (Consumer) SSJDescriptor.instantiate(clazz);
                    break;
                }
            }
            if (consumer != null)
            {
                pipeline.add(consumer);
                pipeline.addProvider(consumer, sensorChannel);
                pipeline.setFrameSize(consumer, 1);
                pipeline.setDelta(consumer, 0);
            }
        }
        pipeline.buildPipe();
        //start framework
        frame.start();
        //run for two minutes
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
        frame.stop();
        frame.release();
    }
}