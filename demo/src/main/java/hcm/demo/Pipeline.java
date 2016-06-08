/*
 * Pipeline.java
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

package hcm.demo;

import android.os.Environment;
import android.util.Log;

import com.jjoe64.graphview.GraphView;

import hcm.ssj.audio.AudioProvider;
import hcm.ssj.audio.Microphone;
import hcm.ssj.audio.Pitch;
import hcm.ssj.body.Activity;
import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Provider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssj.myo.AccelerationProvider;
import hcm.ssj.myo.DynAccelerationProvider;
import hcm.ssj.myo.Myo;
import hcm.ssj.signal.MvgAvgVar;

public class Pipeline extends Thread {

    private boolean _terminate = false;
    private TheFramework _ssj;

    private MainActivity _act = null;
    private GraphView _graphs[] = null;

    public Pipeline(MainActivity a, GraphView[] graphs)
    {
        _act = a;
        _graphs = graphs;

        if(TheFramework.isInstanced())
            TheFramework.getFramework().clear();
        _ssj = TheFramework.getFramework();
    }

    public void setExceptionHandler(ExceptionHandler h)
    {
        if(_ssj == null)
            return;

        _ssj.setExceptionHandler(h);
    }

    public void run()
    {
        _ssj.options.bufferSize = 10.0f;
        _ssj.options.countdown = 10;
        _ssj.options.logfile = Environment.getExternalStorageDirectory() + "/ssjlog.txt";

        //** connection to sensors
        Microphone mic = new Microphone();
        _ssj.addSensor(mic);
        AudioProvider audio = new AudioProvider();
        audio.options.sampleRate = 16000;
        audio.options.scale = true;
        mic.addProvider(audio);

        Myo myo = new Myo();
        _ssj.addSensor(myo);
        DynAccelerationProvider acc = new DynAccelerationProvider();
        myo.addProvider(acc);


        //** transform data coming from sensors
        Pitch pitch = new Pitch();
        pitch.options.detector = Pitch.YIN;
        pitch.options.computePitchedState = false;
        pitch.options.computePitch = true;
        pitch.options.computeVoicedProb = false;
        pitch.options.computePitchEnvelope = false;
        _ssj.addTransformer(pitch, audio, 0.032, 0);

        Activity activity = new Activity();
        _ssj.addTransformer(activity, acc, 0.1, 5.0);

        MvgAvgVar activityf = new MvgAvgVar();
        activityf.options.window = 10;
        _ssj.addTransformer(activityf, activity, 0.1, 0);


        //** configure GUI
        //paint audio
        SignalPainter paint = new SignalPainter();
        paint.options.manualBounds = true;
        paint.options.min = 0;
        paint.options.max = 1;
        paint.options.renderMax = true;
        paint.options.secondScaleMin = 0;
        paint.options.secondScaleMax = 500;
        paint.registerGraphView(_graphs[0]);
        _ssj.addConsumer(paint, new Provider[]{audio,pitch}, 0.1, 0);

        //paint myo activity
        paint = new SignalPainter();
        paint.options.renderMax = true;
        paint.options.manualBounds = true;
        paint.options.min = -3;
        paint.options.max = 3;
        paint.registerGraphView(_graphs[1]);
        _ssj.addConsumer(paint, acc, 0.1, 0);

        paint = new SignalPainter();
        paint.options.renderMax = false;
        paint.options.manualBounds = true;
        paint.options.min = -3;
        paint.options.colors = new int[]{ 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};;
        paint.options.max = 3;
        paint.options.secondScaleStream = 0;
        paint.options.secondScaleDim = 0;
        paint.options.secondScaleMin = 0;
        paint.options.secondScaleMax = 3;
        paint.registerGraphView(_graphs[1]);
        _ssj.addConsumer(paint, activityf, 0.1, 0);

        Log.i("SSJ_Demo", "starting pipeline");
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

        Log.i("SSJ_Demo", "stopping pipeline");
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
