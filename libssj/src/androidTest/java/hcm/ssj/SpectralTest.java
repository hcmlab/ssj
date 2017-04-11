/*
 * SpectralTest.java
 * Copyright (c) 2017
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
import android.os.Environment;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.biosig.HRVSpectral;
import hcm.ssj.core.Pipeline;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.file.FileWriter;
import hcm.ssj.signal.Spectrogram;

/**
 * Tests the spectrogram.<br>
 * Created by Ionut Damian on 01.04.2017.
 */
public class SpectralTest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 3 * 60 * 1000;
    private final static SensorType[] SENSOR_TYPES = {SensorType.ACCELEROMETER, SensorType.GRAVITY, SensorType.GYROSCOPE, SensorType.LINEAR_ACCELERATION, SensorType.MAGNETIC_FIELD};
    private final static String FILE = "", PATH = "";

    /**
     *
     */
    public SpectralTest()
    {
        super(Application.class);
    }


    public void test1() throws Exception
    {
        double window = 5.0;

        // Setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(61.0f);
        frame.options.countdown.set(0);
        frame.options.log.set(true);

        // Sensor
        FileReader file = new FileReader();
        file.options.filePath.set(Environment.getExternalStorageDirectory().getPath() + "/SSJ");
        file.options.fileName.set("IBId.stream");
        FileReaderChannel channel = new FileReaderChannel();
        channel.options.chunk.set(0.032);
        channel.setWatchInterval(0);
        channel.setSyncInterval(0);
        frame.addSensor(file, channel);

        // Transformer
        Spectrogram spectrogram = new Spectrogram();
        spectrogram.options.banks.set("0.003 0.040, 0.040 0.150, 0.150 0.400");
        spectrogram.options.nbanks.set(3);
        spectrogram.options.nfft.set(1024);
        spectrogram.options.dopower.set(true);
        spectrogram.options.dolog.set(false);
        frame.addTransformer(spectrogram, channel, 5, 55);

        HRVSpectral feat = new HRVSpectral();
        frame.addTransformer(feat, spectrogram, 5, 0);

        FileWriter write = new FileWriter();
        write.options.fileName.set("IBIs");
        frame.addConsumer(write, spectrogram, 5, 0);

        FileWriter write2 = new FileWriter();
        write2.options.fileName.set("IBIsf");
        frame.addConsumer(write2, feat, 5, 0);

        // start framework
        frame.start();

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

        // stop framework
        frame.stop();
        frame.clear();
    }
}
