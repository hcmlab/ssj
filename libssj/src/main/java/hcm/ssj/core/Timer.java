/*
 * Timer.java
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

package hcm.ssj.core;

import android.os.SystemClock;

import java.util.ArrayList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class Timer {

    double _delta; //in s
    double _next; //in s
    double _offset = 0; //in s

    long _now; //in ms
    long _init; //in ms
    long _next_ms; //in ms

    long _tick_start = 0;

    final int HISTORY_SIZE = 10;
    ArrayList<Long> _history = new ArrayList<Long>();

    public Timer()
    {
        reset();
    }

    public Timer (int interval_in_ms)
    {
        setClockMs(interval_in_ms);
        reset();
    }

    public Timer (double interval_in_s)
    {
        setClockS(interval_in_s);
        reset();
    }

    public void setClockS(double seconds)
    {
        _delta = seconds;
    }

    public void setClockMs(long milliseconds)
    {
        _delta = milliseconds / 1000.0;
    }

    public void setClockHz(double hz)
    {
        setClockS(1.0 / hz);
    }

    public void reset ()
    {
        _init = SystemClock.elapsedRealtime();
        _next = _delta + _offset;
    }

    //offsets the first tick, requires a "reset"
    public void setStartOffset(double seconds)
    {
        _offset = seconds;
    }

    //offsets the next tick, requires a "reset"
    public void setStartOffset(long milliseconds)
    {
        _offset = milliseconds / 1000.0;
    }

    //equivalent to SSI's wait()
    public void sync ()
    {
        _now = SystemClock.elapsedRealtime() - _init;
        _next_ms = (long)(_next * 1000 + 0.5);

        while (_now < _next_ms)
        {
            try
            {
                Thread.sleep ( _next_ms - _now );
            }
            catch (InterruptedException e){
                Log.w("thread interrupt");
            }

            _now = SystemClock.elapsedRealtime() - _init;
        }

        if(_now - _next_ms > _delta * 1000)
            Log.d(Thread.currentThread().getStackTrace()[3].getClassName().replace("hcm.ssj.", ""),
                  "thread too busy, missed sync point");

        _next += _delta;
    }

    public void tick_start()
    {
        _tick_start = SystemClock.elapsedRealtime();
    }

    public void tick_end()
    {
        _history.add(SystemClock.elapsedRealtime() - _tick_start);
        if(_history.size() > HISTORY_SIZE)
            _history.remove(0);
    }

    public void tick()
    {
        if(_tick_start != 0)
            _history.add(SystemClock.elapsedRealtime() - _tick_start);

        _tick_start = SystemClock.elapsedRealtime();

        if(_history.size() > HISTORY_SIZE)
            _history.remove(0);
    }

    public double getMax()
    {
        long max = 0;
        for(Long delta : _history) {
            if(delta > max)
                max = delta;
        }
        return max / 1000.0;
    }

    public double getAvgDur()
    {
        if(_history.size() == 0)
            return 0;

        long sum = 0;
        for(Long delta : _history) {
            sum += delta;
        }

        double avg = (double)sum / (double)_history.size();
        return avg / 1000.0;
    }

    public long getElapsedMs()
    {
        return SystemClock.elapsedRealtime() - _init;
    }

    public double getElapsed()
    {
        return getElapsedMs() / 1000.0;
    }
}
