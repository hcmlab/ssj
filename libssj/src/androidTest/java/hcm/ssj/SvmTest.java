/*
 * AndroidSensorTest.java
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
import android.os.BatteryManager;
import android.os.Environment;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Transformer;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.ml.ClassifierT;
import hcm.ssj.msband.GSRChannel;
import hcm.ssj.msband.HeartRateChannel;
import hcm.ssj.msband.MSBand;
import hcm.ssj.msband.SkinTempChannel;
import hcm.ssj.signal.Butfilt;
import hcm.ssj.signal.Derivative;
import hcm.ssj.signal.FFTfeat;
import hcm.ssj.signal.Functionals;
import hcm.ssj.signal.Progress;
import hcm.ssj.test.Logger;

import static android.content.Context.BATTERY_SERVICE;

/**
 * Tests the SVM class.<br>
 * Created by Frank Gaibler on 13.01.2017.
 */
public class SvmTest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 2 * 60 * 1000;
    private final static SensorType[] SENSOR_TYPES = {SensorType.ACCELEROMETER, SensorType.GRAVITY, SensorType.GYROSCOPE, SensorType.LINEAR_ACCELERATION, SensorType.MAGNETIC_FIELD};
    private final static String FILE = "", PATH = "";

    /**
     *
     */
    public SvmTest()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testSvm() throws Exception
    {
        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(61.0f);
        Transformer[] transformers = new Transformer[SENSOR_TYPES.length * 2];
        //add all sensors
        for (int i = 0; i < SENSOR_TYPES.length; i++)
        {
            //sensor
            AndroidSensor sensor = new AndroidSensor();
            sensor.options.sensorType.set(SENSOR_TYPES[i]);

            //channel
            AndroidSensorChannel sensorChannel = new AndroidSensorChannel();
            frame.addSensor(sensor,sensorChannel);
            //transformers
            Progress progress = new Progress();
            frame.addTransformer(progress, sensorChannel, 5, 0);
            //
            Functionals distance = new Functionals();
            distance.options.mean.set(false);
            distance.options.energy.set(false);
            distance.options.std.set(false);
            distance.options.min.set(false);
            distance.options.max.set(false);
            distance.options.range.set(false);
            distance.options.minPos.set(false);
            distance.options.maxPos.set(false);
            distance.options.zeros.set(false);
            distance.options.peaks.set(false);
            distance.options.len.set(false);
            distance.options.path.set(true);
            frame.addTransformer(distance, sensorChannel, 30, 30);
            transformers[i] = distance;
            //
            Functionals functionals = new Functionals();
            functionals.options.mean.set(true);
            functionals.options.energy.set(true);
            functionals.options.std.set(true);
            functionals.options.min.set(true);
            functionals.options.max.set(true);
            functionals.options.range.set(true);
            functionals.options.minPos.set(false);
            functionals.options.maxPos.set(false);
            functionals.options.zeros.set(false);
            functionals.options.peaks.set(false);
            functionals.options.len.set(false);
            functionals.options.path.set(false);
            frame.addTransformer(functionals, progress, 30, 30);
            transformers[i + SENSOR_TYPES.length] = functionals;
        }
        ClassifierT classifier = new ClassifierT();
        classifier.options.trainerFile.set(FILE);
        classifier.options.trainerPath.set(PATH);
        frame.addTransformer(classifier, transformers, 30, 30);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, classifier, 30, 0);
        //start framework
        frame.start();
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
        frame.stop();
        frame.release();
    }

    public void testPerformance() throws Exception
    {
        double window = 5.0;

        // Setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);
        frame.options.countdown.set(3);
        frame.options.log.set(true);

        // Sensor
        FileReader file = new FileReader();
        file.options.filePath.set(Environment.getExternalStorageDirectory().getPath() + "/SSJ/data/1");
        file.options.fileName.set("audio.stream");
        FileReaderChannel channel = new FileReaderChannel();
        channel.options.chunk.set(0.032);
        frame.addSensor(file, channel);

        // Transformer
        FFTfeat fft = new FFTfeat();
        frame.addTransformer(fft, channel, 512.0 / channel.getSampleRate(), 0);

        Functionals func = new Functionals();
        frame.addTransformer(func, fft, window, 0);

        ClassifierT classifier = new ClassifierT();
        classifier.options.trainerPath.set("/sdcard/SSJ/data/model");
        classifier.options.trainerFile.set("johnny.trainer");
        frame.addTransformer(classifier, func, window, 0);

        Logger log = new Logger();
        frame.addConsumer(log, channel, window, 0);

        // start framework
        frame.start();

        BatteryManager bm = (BatteryManager) SSJApplication.getAppContext().getSystemService(BATTERY_SERVICE);
        long end = System.currentTimeMillis() + TEST_LENGTH;

        // Run test
        try
        {
//            while (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) > 5)
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // stop framework
        frame.stop();
        frame.clear();
    }

    public void testStressReco() throws Exception
    {
        double window = 5.0;

        // Setup
        Pipeline ssj = Pipeline.getInstance();
        ssj.options.bufferSize.set(10.0f);
        ssj.options.countdown.set(3);
        ssj.options.log.set(true);

        MSBand msBand = new MSBand();

        /**
         * Process HR
         */
        HeartRateChannel hr = new HeartRateChannel();
        ssj.addSensor(msBand, hr);

        Derivative hrd = new Derivative();
        hrd.options.zero.set(false);
        ssj.addTransformer(hrd, hr, 5, 5);

        Functionals hrd_func = new Functionals();
        ssj.addTransformer(hrd_func, hrd, 5, 5);

        /**
         * Process GSR
         */
        GSRChannel gsr = new GSRChannel();
        ssj.addSensor(msBand, gsr);

        Functionals gsr_func = new Functionals();
        ssj.addTransformer(gsr_func, gsr, 5, 5);

        Butfilt gsrf = new Butfilt();
        gsrf.options.type.set(Butfilt.Type.HIGH);
        gsrf.options.high.set(0.001);
        ssj.addTransformer(gsrf, gsr, 5, 5);

        Functionals gsrf_func = new Functionals();
        ssj.addTransformer(gsrf_func, gsrf, 5, 5);

        /**
         * Process Temp
         */
        SkinTempChannel temp = new SkinTempChannel();
        ssj.addSensor(msBand, temp);

        Derivative tempd = new Derivative();
        tempd.options.zero.set(false);
        ssj.addTransformer(tempd, temp, 5, 5);

        Functionals tempd_func = new Functionals();
        ssj.addTransformer(tempd_func, tempd, 5, 5);

        /**
         * Classify
         */
        ClassifierT stress = new ClassifierT();
        stress.options.trainerPath.set("/sdcard/Glassistant/model/");
        stress.options.trainerFile.set("stress_model.trainer");
        Provider[] input = new Provider[]{hrd_func, gsr_func, tempd_func, gsrf_func};
        ssj.addTransformer(stress, input, 5, 5);

        Logger log = new Logger();
        ssj.addConsumer(log, stress, window, 0);

        // Run test
        long end = System.currentTimeMillis() + TEST_LENGTH;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // stop framework
        ssj.stop();
        ssj.clear();
    }
}
