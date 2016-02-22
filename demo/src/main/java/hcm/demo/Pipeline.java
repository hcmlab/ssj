/*
 * Pipeline.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.demo;

import android.os.Environment;
import android.util.Log;

import com.jjoe64.graphview.GraphView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import hcm.ssj.biosig.GSRArousalEstimation;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.empatica.BVPProvider;
import hcm.ssj.empatica.Empatica;
import hcm.ssj.empatica.GSRProvider;
import hcm.ssj.empatica.IBIProvider;
import hcm.ssj.file.SimpleFileWriter;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssj.test.Logger;

public class Pipeline extends Thread {

    private final String GLASS_MAC = "F8:8F:CA:11:B4:6F";
    private final String GLASS_NAME = "Hcm Lab's Glass";

    private final String SSI_IP = "137.250.171.61";

    private boolean _terminate = false;
    private TheFramework _ssj;
    double _frameSize = 0.1;

    private MainActivity _act = null;
    private GraphView _graphs[] = null;

    public Pipeline(MainActivity a, GraphView[] graphs)
    {
        _act = a;
        _graphs = graphs;
        start();
    }

    public void run()
    {
        //setup an SSJ pipeline to send sensor data to SSI
        _ssj = TheFramework.getFramework();
        _ssj.options.bufferSize = 10.0f;
//
//        Microphone mic = new Microphone();
//        AudioProvider audio = new AudioProvider(mic);
//        audio.options.sampleRate = 16000;
//        audio.options.scale = true;
//        _ssj.addSensorProvider(audio);
//        _ssj.addSensor(mic);
//
//        Pitch pitch = new Pitch();
//        pitch.options.detector = Pitch.FFT_YIN;
//        pitch.options.computePitchedState = false;
//        pitch.options.computePitch = false;
//        pitch.options.computeVoicedProb = false;
//        pitch.options.computePitchEnvelope = true;
//        _ssj.addTransformer(pitch, audio, 0.032, 0);
//
//        MvgAvgVar var = new MvgAvgVar();
//        var.options.window = 5;
//        var.options.format = MvgAvgVar.Format.VARIANCE;
//        _ssj.addTransformer(var, pitch, 0.032, 0);

//        Intensity intensityPraat = new Intensity();
//        intensityPraat.options.subtractMeanPressure = false;
//        _ssj.addTransformer(intensityPraat, audio, 1.0, 0);
//
//        SignalPainter paint = new SignalPainter();
//        paint.options.manualBounds = true;
//        paint.options.min = 0;
//        paint.options.max = 3000;
//        paint.options.renderMax = true;
//        paint.registerGraphView(_graphs[0]);
//        _ssj.addConsumer(paint, var, 0.1, 0);
//
//        paint = new SignalPainter();
//        paint.options.min = 0;
//        paint.options.max = 3000;
//        paint.options.manualBounds = true;
//        paint.options.secondScaleMin = 0;
//        paint.options.secondScaleMax = 500;
//        paint.options.renderMax = true;
//        paint.options.secondScaleDim = 0;
//        paint.options.colors =  new int[]{0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
//        paint.registerGraphView(_graphs[0]);
//        _ssj.addConsumer(paint, pitch, 0.1, 0);

//        SignalPainter paint = new SignalPainter();
//        paint.registerGraphView(_graphs[0]);
//        paint.options.renderMax = false;
//        paint.options.colors =  new int[]{0xffff9900, 0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
//        paint.options.manualBounds = true;
//        paint.options.min = 0;
//        paint.options.max = 1;
//        paint.options.secondScaleDim = 0;
//        paint.options.secondScaleMin = 0;
//        paint.options.secondScaleMax = 100;
//        _ssj.addConsumer(paint, intensityPraat, 1.0, 0);

//        SignalPainter paint5 = new SignalPainter();
//        paint5.registerGraphView(_graphs[1]);
//        paint5.options.colors =  new int[]{0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
//        paint5.options.manualBounds = true;
//        paint5.options.renderMax = false;
//        paint5.options.min = 0;
//        paint5.options.max = 1;
//        _ssj.addConsumer(paint5, pitch, 0.1, 0);
//
//        paint5 = new SignalPainter();
//        paint5.registerGraphView(_graphs[1]);
//        paint5.options.colors =  new int[]{0xffff00ff, 0xff000000, 0xff339900};
//        paint5.options.manualBounds = true;
//        paint5.options.renderMax = false;
//        paint5.options.min = 0;
//        paint5.options.max = 1;
//        paint5.options.secondScaleDim = 0;
//        paint5.options.secondScaleMin = 0;
//        paint5.options.secondScaleMax = 1;
//        _ssj.addConsumer(paint5, pitchf, 0.1, 0);

//        Myo myo = new Myo();
//        myo.options.emgMode = Configuration.EmgMode.FILTERED;
//        DynAccelerationProvider acc = new DynAccelerationProvider(myo);
//        _ssj.addSensorProvider(acc);
//        _ssj.addSensor(myo);
//
//        Activity activity = new Activity();
//        _ssj.addTransformer(activity, acc, 0.1, 5.0);
//
//        SignalPainter paint3 = new SignalPainter();
//        paint3.options.manualBounds = true;
//        paint3.options.min = -3;
//        paint3.options.max = 3;
//        paint3.registerGraphView(_graphs[1]);
//        _ssj.addConsumer(paint3, acc, 0.1, 0);
//
//        SignalPainter paint6 = new SignalPainter();
//        paint6.registerGraphView(_graphs[1]);
//        paint6.options.colors =  new int[]{0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
//        paint6.options.secondScaleDim = 0;
//        paint6.options.secondScaleMin = 0;
//        paint6.options.secondScaleMax = 1.5;
//        paint6.options.manualBounds = true;
//        paint6.options.min = -3;
//        paint6.options.max = 3;
//        _ssj.addConsumer(paint6, activity, 0.1, 0);

//        for (hcm.ssj.androidSensor.SensorType type : hcm.ssj.androidSensor.SensorType.values())
//        {
//            hcm.ssj.androidSensor.SensorConnection sensCon = new hcm.ssj.androidSensor.SensorConnection(type);
//            hcm.ssj.androidSensor.SensorConnectionProvider conProv = new hcm.ssj.androidSensor.SensorConnectionProvider(sensCon);
//            _ssj.addSensorProvider(conProv);
//            _ssj.addSensor(sensCon);
//            SignalPainter paintAndroidSensor = new SignalPainter();
//            paintAndroidSensor.options.manualBounds = true;
//            paintAndroidSensor.options.min = -40;
//            paintAndroidSensor.options.max = 40;
//            paintAndroidSensor.options.renderMax = true;
//            paintAndroidSensor.registerGraphView(_graphs[0]);
//            _ssj.addConsumer(paintAndroidSensor, conProv, 1, 0);
//        }

//        AndroidSensor sensor = new AndroidSensor(SensorType.ACCELEROMETER);
//        AndroidSensorProvider prov = new AndroidSensorProvider(sensor);
//        _ssj.addSensorProvider(prov);
//        _ssj.addSensor(sensor);

//        Empatica empatica = new Empatica();
//        AccelerationProvider acc = new AccelerationProvider(empatica);
//        _ssj.addSensorProvider(acc);
//        _ssj.addSensor(empatica);
//
//        SignalPainter paint = new SignalPainter();
//        paint.options.manualBounds = true;
//        paint.options.min = -10;
//        paint.options.max = 10;
//        paint.options.renderMax = true;
//        paint.registerGraphView(_graphs[0]);
//        _ssj.addConsumer(paint, prov, 0.1, 0);


//        sensor = new SensorConnection(SensorType.GAME_ROTATION_VECTOR);
//        prov = new SensorConnectionProvider(sensor);
//        _ssj.addSensorProvider(prov);
//        _ssj.addSensor(sensor);

//        paint = new SignalPainter();
//        paint.options.manualBounds = true;
//        paint.options.min = -360;
//        paint.options.max = 360;
//        paint.options.renderMax = true;
//        paint.registerGraphView(_graphs[1]);
//        _ssj.addConsumer(paint, prov, 0.1, 0);

        Empatica empatica = new Empatica();
        SensorProvider gsr = empatica.addProvider(new GSRProvider());
        SensorProvider ibi = empatica.addProvider(new IBIProvider());
        SensorProvider bvp = empatica.addProvider(new BVPProvider());
        _ssj.addSensor(empatica);

        GSRArousalEstimation arousal = new GSRArousalEstimation();
        _ssj.addTransformer(arousal, gsr, 0.25, 0);

        SignalPainter paint = new SignalPainter();
        paint.options.manualBounds = true;
        paint.options.min = 0;
        paint.options.max = 1;
        paint.options.renderMax = true;
        paint.options.size = 120;
        paint.registerGraphView(_graphs[0]);
        _ssj.addConsumer(paint, gsr, 0.25, 0);

        paint = new SignalPainter();
        paint.registerGraphView(_graphs[0]);
        paint.options.colors =  new int[]{0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
        paint.options.secondScaleDim = 0;
        paint.options.secondScaleMin = 0;
        paint.options.secondScaleMax = 1;
        paint.options.manualBounds = true;
        paint.options.min = 0;
        paint.options.max = 1;
        paint.options.size = 120;
        _ssj.addConsumer(paint, arousal, 0.25, 0);

        Logger dummy = new Logger();
        _ssj.addConsumer(dummy, gsr, 0.25, 0);

        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/empatica_test/" + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())));
        if(!dir.exists())
            dir.mkdirs();

        File fileHeader = new File(dir, "gsr");
        SimpleFileWriter simpleFileWriter = new SimpleFileWriter();
        simpleFileWriter.options.file = fileHeader;
        _ssj.addConsumer(simpleFileWriter, gsr, 1, 0);

        fileHeader = new File(dir, "arousal");
        simpleFileWriter = new SimpleFileWriter();
        simpleFileWriter.options.file = fileHeader;
        _ssj.addConsumer(simpleFileWriter, arousal, 1, 0);

        fileHeader = new File(dir, "ibi");
        simpleFileWriter = new SimpleFileWriter();
        simpleFileWriter.options.file = fileHeader;
        _ssj.addConsumer(simpleFileWriter, ibi, 1, 0);

        fileHeader = new File(dir, "bvp");
        simpleFileWriter = new SimpleFileWriter();
        simpleFileWriter.options.file = fileHeader;
        _ssj.addConsumer(simpleFileWriter, bvp, 1, 0);

        _ssj.Start();
        _act.notifyPipeState(true);

        while(!_terminate)
        {
            try
            {
                synchronized(this)
                {
                    this.wait();
                }
            }
            catch (InterruptedException e)
            {
                Log.e("pipeline", "Error", e);
            }
        }

        Log.i("LogueWorker", "stopping SSJ");
        _ssj.Stop();
        _ssj.clear();
        _act.notifyPipeState(false);
    }

    public void terminate()
    {
        _terminate = true;

        synchronized(this)
        {
            this.notify();
        }
    }

    public boolean isRunning()
    {
        return _ssj.isRunning();
    }
}
