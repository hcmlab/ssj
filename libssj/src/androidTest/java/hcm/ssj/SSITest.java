/*
 * SSITest.java
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

import android.os.Environment;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.mobileSSI.SSI;
import hcm.ssj.mobileSSI.SSITransformer;
import hcm.ssj.signal.AvgVar;
import hcm.ssj.signal.Median;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.MinMax;
import hcm.ssj.signal.Progress;
import hcm.ssj.test.Logger;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SSITest
{
    @Test
    public void testTransformer() throws Exception
    {
        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        //sensor
        AndroidSensor sensor = new AndroidSensor();
        AndroidSensorChannel channel = new AndroidSensorChannel();
        channel.options.sensorType.set(SensorType.ACCELEROMETER);
        frame.addSensor(sensor, channel);

        SSITransformer transf = new SSITransformer();
        transf.options.name.set(SSI.TransformerName.Butfilt);
        transf.options.ssioptions.set(new String[]{"low->0.01"});
        frame.addTransformer(transf, channel, 1);

        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transf, 1, 0);

        //start framework
        frame.start();
        //run test
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

    @Test
    public void testClassifierT() throws Exception
    {
        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        //sensor
        AndroidSensor sensor = new AndroidSensor();
        AndroidSensorChannel acc = new AndroidSensorChannel();
        acc.options.sensorType.set(SensorType.ACCELEROMETER);
        frame.addSensor(sensor, acc);

        AndroidSensorChannel gyr = new AndroidSensorChannel();
        gyr.options.sensorType.set(SensorType.GYROSCOPE);
        frame.addSensor(sensor, gyr);

        AndroidSensorChannel mag = new AndroidSensorChannel();
        mag.options.sensorType.set(SensorType.MAGNETIC_FIELD);
        frame.addSensor(sensor, mag);

        Progress prog = new Progress();
        frame.addTransformer(prog, new Provider[]{acc, gyr, mag}, 1.0, 0);

        AvgVar avg = new AvgVar();
        avg.options.avg.set(true);
        avg.options.var.set(true);
        frame.addTransformer(avg, prog, 1.0, 0);

        MinMax minmax = new MinMax();
        frame.addTransformer(minmax, prog, 1.0, 0);

        Median med = new Median();
        frame.addTransformer(med, prog, 1.0, 0);

        Merge merge = new Merge();
        frame.addTransformer(merge, new Provider[]{avg, med, minmax}, 1.0, 0);

        SSITransformer transf = new SSITransformer();
        transf.options.name.set(SSI.TransformerName.ClassifierT);
        transf.options.ssioptions.set(new String[]{"trainer->" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/SSJ/Creator/res/activity.NaiveBayes.trainer"});
        frame.addTransformer(transf, merge, 1);

        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transf, 1, 0);

        //start framework
        frame.start();
        //run test
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
