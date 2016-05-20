/*
 * AudioTest.java
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
import android.media.AudioFormat;
import android.test.ApplicationTestCase;

import java.io.File;

import hcm.ssj.audio.AudioProvider;
import hcm.ssj.audio.AudioWriter;
import hcm.ssj.audio.Microphone;
import hcm.ssj.audio.Pitch;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Provider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.event.ThresholdEventSender;
import hcm.ssj.praat.Intensity;
import hcm.ssj.signal.Avg;
import hcm.ssj.test.EventLogger;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class AudioTest extends ApplicationTestCase<Application>
{
    public AudioTest()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testWriter() throws Exception
    {
        final int TEST_LENGTH = 2 * 60 * 1000;
        //resources
        File dir = getContext().getFilesDir();
        String fileName = getClass().getSimpleName() + "." + getClass().getSimpleName();
        File file = new File(dir, fileName);
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.setValue(10.0f);
        //sensor
        Microphone microphone = new Microphone();
        frame.addSensor(microphone);
        //provider
        AudioProvider audioProvider = new AudioProvider();
        audioProvider.options.audioFormat.setValue(AudioFormat.ENCODING_PCM_16BIT);
        audioProvider.options.channelConfig.setValue(AudioFormat.CHANNEL_IN_STEREO);
        audioProvider.options.sampleRate.setValue(8000);
        audioProvider.options.scale.setValue(true);
        microphone.addProvider(audioProvider);
        //consumer
        AudioWriter audioWriter = new AudioWriter();
        audioWriter.options.audioFormat.setValue(AudioFormat.ENCODING_PCM_16BIT);
        audioWriter.options.filePath.setValue(dir.getPath());
        audioWriter.options.fileName.setValue(fileName);
        frame.addConsumer(audioWriter, audioProvider, 1, 0);
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
        //cleanup
        if (file.exists())
        {
            if (!file.delete())
            {
                throw new RuntimeException("File could not be deleted");
            }
        }
    }

    public void testSpeechrate() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.setValue(10.0f);

        Microphone mic = new Microphone();
        AudioProvider audio = new AudioProvider();
        audio.options.sampleRate.setValue(16000);
        audio.options.scale.setValue(true);
        mic.addProvider(audio);
        frame.addSensor(mic);

        Pitch pitch = new Pitch();
        pitch.options.detector.setValue(Pitch.YIN);
        frame.addTransformer(pitch, audio, 0.04, 0);

        Avg pitch_env = new Avg();
        frame.addTransformer(pitch_env, pitch, 0.04, 0.96);

        Intensity energy = new Intensity();
        frame.addTransformer(energy, audio, 1.0, 0);

        //VAD
        ThresholdEventSender vad = new ThresholdEventSender();
        vad.options.thresin = new float[]{50.0f}; //SPL
        vad.options.mindur.setValue(1.0);
        vad.options.maxdur.setValue(9.0);
        vad.options.hangin.setValue(3);
        vad.options.hangout.setValue(5);
        Provider[] vad_in = {energy};
        frame.addConsumer(vad, vad_in, 1.0, 0);
        EventChannel vad_channel = frame.registerEventProvider(vad);

        hcm.ssj.audio.SpeechRate sr = new hcm.ssj.audio.SpeechRate();
        sr.options.thresholdVoicedProb.setValue(0.3f);
        Provider[] sr_in = {energy, pitch_env};
        frame.addEventConsumer(sr, sr_in, vad_channel);
        EventChannel sr_channel = frame.registerEventProvider(sr);

        EventLogger log = new EventLogger();
        frame.addComponent(log);
        frame.registerEventListener(log, sr_channel);

        try
        {
            frame.Start();

            long start = System.currentTimeMillis();
            while (true)
            {
                if (System.currentTimeMillis() > start + 2 * 60 * 1000)
                    break;

                Thread.sleep(1);
            }

            frame.Stop();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}