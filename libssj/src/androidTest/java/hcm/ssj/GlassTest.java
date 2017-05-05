/*
 * GlassTest.java
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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.core.Pipeline;
import hcm.ssj.glass.BlinkDetection;
import hcm.ssj.glass.InfraredChannel;
import hcm.ssj.glass.InfraredSensor;
import hcm.ssj.test.Logger;

/**
 * Tests all classes in the glass package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class GlassTest
{
    @Test
    public void testInfrared() throws Exception
    {
        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);
        //sensor
        InfraredSensor sensor = new InfraredSensor();

        //channel
        InfraredChannel sensorChannel = new InfraredChannel();
        frame.addSensor(sensor,sensorChannel);
        //transformer
        BlinkDetection transformer = new BlinkDetection();
        frame.addTransformer(transformer, sensorChannel, 1, 0);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, sensorChannel, 1, 0);
        //start framework
        frame.start();

        //run for two minutes
        long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
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
