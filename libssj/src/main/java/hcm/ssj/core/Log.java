/*
 * Log.java
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

package hcm.ssj.core;

import hcm.ssj.BuildConfig;

/**
 * Created by Johnny on 17.03.2016.
 */
public class Log
{
    private static String getCaller()
    {
        StackTraceElement element = Thread.currentThread().getStackTrace()[4];
        return element.getClassName().replace("hcm.ssj.", "");
    }

    public static void d(String msg)
    {
        android.util.Log.d(Cons.LOGTAG, "[" + getCaller() + "] " + msg);
    }
    public static void d(String msg, Exception e)
    {
        android.util.Log.d(Cons.LOGTAG, "[" + getCaller() + "] " + msg, e);
    }

    //selective log variant
    public static void ds(String msg)
    {
        if (BuildConfig.DEBUG)
            android.util.Log.d(Cons.LOGTAG, "[" + getCaller() + "] " + msg);
    }
    public static void ds(String msg, Exception e)
    {
        if (BuildConfig.DEBUG)
            android.util.Log.d(Cons.LOGTAG, "[" + getCaller() + "] " + msg, e);
    }

    public static void i(String msg)
    {
        android.util.Log.i(Cons.LOGTAG, "[" + getCaller() + "] " + msg);
    }

    public static void i(String msg, Exception e)
    {
        android.util.Log.i(Cons.LOGTAG, "[" + getCaller() + "] " + msg, e);
    }

    public static void e(String msg)
    {
        android.util.Log.e(Cons.LOGTAG, "[" + getCaller() + "] " + msg);
    }
    public static void e(String msg, Exception e)
    {
        android.util.Log.e(Cons.LOGTAG, "[" + getCaller() + "] " + msg, e);
    }

    public static void w(String msg)
    {
        android.util.Log.w(Cons.LOGTAG, "[" + getCaller() + "] " + msg);
    }
    public static void w(String msg, Exception e)
    {
        android.util.Log.w(Cons.LOGTAG, "[" + getCaller() + "] " + msg, e);
    }
}
