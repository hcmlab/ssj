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

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import hcm.ssj.BuildConfig;

/**
 * Created by Johnny on 17.03.2016.
 */
public class Log
{
    public class Entry
    {
        double t;
        String msg;

        public Entry(double t, String msg)
        {
            this.t = t;
            this.msg = msg;
        }
    }

    private LinkedList<Entry> buffer = new LinkedList<>();
    private StringBuilder builder = new StringBuilder();
    private TheFramework frame = null;
    private static Log instance = null;

    Log() {}

    public void setFramework(TheFramework frame)
    {
        this.frame = frame;
    }

    public static Log getInstance()
    {
        if(instance == null)
            instance = new Log();

        return instance;
    }

    public void reset()
    {
        buffer.clear();
    }

    public void saveToFile(String filename)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(filename);

            NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
            nf.setMaximumFractionDigits(3);
            nf.setMinimumFractionDigits(3);

            Iterator<Entry> iter = buffer.iterator();
            while(iter.hasNext())
            {
                Entry e = iter.next();

                builder.setLength(0);
                builder.append(nf.format(e.t));
                builder.append("\t");
                builder.append(e.msg);
                builder.append("\r\n");

                fos.write(builder.toString().getBytes());
            }

            fos.close();
        }
        catch (IOException e)
        {
            Log.e("Exception in creating logfile", e);
        }
    }

    private String getCaller()
    {
        StackTraceElement element = Thread.currentThread().getStackTrace()[5];
        return element.getClassName().replace("hcm.ssj.", "");
    }

    private synchronized String newEntry(String msg)
    {
        builder.setLength(0);
        builder.append('[').append(getCaller()).append("] ").append(msg);

        String str = builder.toString();
        buffer.add(new Entry((frame == null) ? 0 : frame.getTime(), str));

        return str;
    }

    public static void d(String msg)
    {
        android.util.Log.d(Cons.LOGTAG, getInstance().newEntry(msg));
    }
    public static void d(String msg, Exception e)
    {
        android.util.Log.d(Cons.LOGTAG, getInstance().newEntry(msg), e);
    }

    //selective log variant
    public static void ds(String msg)
    {
        if (BuildConfig.DEBUG)
            android.util.Log.d(Cons.LOGTAG, getInstance().newEntry(msg));
    }
    public static void ds(String msg, Exception e)
    {
        if (BuildConfig.DEBUG)
            android.util.Log.d(Cons.LOGTAG, getInstance().newEntry(msg), e);
    }

    public static void i(String msg)
    {
        android.util.Log.i(Cons.LOGTAG, getInstance().newEntry(msg));
    }

    public static void i(String msg, Exception e)
    {
        android.util.Log.i(Cons.LOGTAG, getInstance().newEntry(msg), e);
    }

    public static void e(String msg)
    {
        android.util.Log.e(Cons.LOGTAG, getInstance().newEntry(msg));
    }

    public static void e(String msg, Exception e)
    {
        android.util.Log.e(Cons.LOGTAG, getInstance().newEntry(msg), e);
    }

    public static void w(String msg)
    {
        android.util.Log.w(Cons.LOGTAG, getInstance().newEntry(msg));
    }
    public static void w(String msg, Exception e)
    {
        android.util.Log.w(Cons.LOGTAG, getInstance().newEntry(msg), e);
    }
}
