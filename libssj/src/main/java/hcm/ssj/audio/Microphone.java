/*
 * Microphone.java
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

package hcm.ssj.audio;

import android.media.AudioFormat;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Sensor;

/**
 * Audio Sensor - "connects" to the audio interface
 * Created by Johnny on 05.03.2015.
 */
public class Microphone extends Sensor {

    public Microphone()
    {
        _name = "SSJ_sensor_Microphone";
    }

    @Override
    public void connect()
    {
    }

    @Override
    public void disconnect()
    {
    }

    public static int audioFormatSampleBytes(int f)
    {
        switch (f)
        {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_DEFAULT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            case AudioFormat.ENCODING_INVALID:
            default:
                return 0;
        }
    }

    public static Cons.Type audioFormatSampleType(int f)
    {
        switch (f)
        {
            case AudioFormat.ENCODING_PCM_8BIT:
                return Cons.Type.CHAR;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_DEFAULT:
                return Cons.Type.SHORT;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return Cons.Type.FLOAT;
            case AudioFormat.ENCODING_INVALID:
            default:
                return Cons.Type.UNDEF;
        }
    }
}
