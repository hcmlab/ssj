/*
 * Pipeline.java
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

package hcm.demo;

import android.util.Log;

import com.jjoe64.graphview.GraphView;

import hcm.ssj.audio.Microphone;
import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Provider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.file.SimpleFileWriter;

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
            TheFramework.getFramework().reset();
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
        try {
            _ssj.options.bufferSize.set(10.0f);
            _ssj.options.countdown.set(10);
            _ssj.options.log.set(true);

            Microphone mic = new Microphone();
            Provider audio = mic.registerOutput(mic.OUT_AUDIO);
            _ssj.addSensor(mic);

            // Transformer
            SimpleFileWriter sfw = new SimpleFileWriter();
            _ssj.addConsumer(sfw, audio, 1, 0);
        }
        catch(Exception e)
        {
            Log.e("SSJ_Demo", "error building pipe", e);
            return;
        }

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
        _ssj.reset();
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
