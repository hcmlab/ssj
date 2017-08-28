/*
 * Log.java
 * Copyright (c) 2017
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import hcm.ssj.BuildConfig;

/**
 * Created by Johnny on 17.03.2016.
 */
public class Log
{
    private final int RECENT_HISTORY_SIZE = 10;

    public enum Level
    {
        VERBOSE(2), DEBUG(3), INFO(4), WARNING(5), ERROR(6), NONE(99);

        Level(int i) {val = i;}
        public final int val;
    }

    /**
     * Interface to register listeners to
     */
    public interface LogListener {
        void msg(int type, String msg);
    }

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
    private Pipeline frame = null;
    private static Log instance = null;
    //
    private static HashSet<LogListener> hsLogListener = new HashSet<>();
    //
    private LinkedHashMap<String, Double> recent = new LinkedHashMap<String, Double>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String,Double> eldest) {
            return size() > RECENT_HISTORY_SIZE;
        }
    };

    Log() {}

    public void setFramework(Pipeline frame)
    {
        this.frame = frame;
    }

    public static Log getInstance()
    {
        if(instance == null)
            instance = new Log();

        return instance;
    }

    public void clear()
    {
        synchronized (this)
        {
            buffer.clear();
            recent.clear();
        }
    }

    public void invalidate()
    {
        clear();
        instance = null;
    }

    public void saveToFile(String path)
    {
        try
        {
            File fileDirectory = Util.createDirectory(path);
            if(fileDirectory == null)
                return;

            File file = new File(fileDirectory, "ssj.log");
            int i = 2;
            while(file.exists())
            {
                file = new File(fileDirectory, "ssj" + (i++) + ".log");
            }

            FileOutputStream fos = new FileOutputStream(file);

            NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
            nf.setMaximumFractionDigits(3);
            nf.setMinimumFractionDigits(3);

            StringBuilder builder = new StringBuilder();

            synchronized (this)
            {
                Iterator<Entry> iter = buffer.iterator();
                while (iter.hasNext())
                {
                    Entry e = iter.next();

                    builder.setLength(0);
                    builder.append(nf.format(e.t));
                    builder.append("\t");
                    builder.append(e.msg);
                    builder.append("\r\n");

                    fos.write(builder.toString().getBytes());
                }
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

    private String buildEntry(String caller, String msg, Throwable tr)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(caller).append("] ").append(msg);

        if(tr != null)
            builder.append(":\n").append(android.util.Log.getStackTraceString(tr));

        return builder.toString();
    }

    private void log(int type, String caller, String msg, Throwable tr)
    {
        if(type < ((frame == null) ? Level.VERBOSE.val : frame.options.loglevel.get().val))
            return;

        String str;
        double time = (frame == null) ? 0 : frame.getTime();
        str = buildEntry(caller, msg, tr);

        //check if entry is in our recent history
        Double lastTime = recent.get(str);
        if(lastTime != null && time - lastTime < ((frame == null) ? 1.0 : frame.options.logtimeout.get()))
            return;

        android.util.Log.println(type, Cons.LOGTAG, str);

        //save in recent
        recent.put(str, time);

        //send message to listeners
        if (hsLogListener.size() > 0) {
            for (LogListener logListener : hsLogListener) {
                logListener.msg(type, str);
            }
        }

        synchronized (this) {
            buffer.add(new Entry(time, str));
        }
    }

    private void log(int type, String msg, Throwable tr)
    {
        log(type, getCaller(), msg, tr);
    }

    /**
     * @param logListener LogListener
     */
    public static void addLogListener(LogListener logListener) {
        hsLogListener.add(logListener);
    }

    /**
     * @param logListener LogListener
     */
    public static void removeLogListener(LogListener logListener) {
        hsLogListener.remove(logListener);
    }

    public static void d(String msg)
    {
        getInstance().log(android.util.Log.DEBUG, msg, null);
    }
    public static void d(String msg, Throwable e)
    {
        getInstance().log(android.util.Log.DEBUG, msg, e);
    }
    public static void d(String tag, String msg)
    {
        getInstance().log(android.util.Log.DEBUG, tag, msg, null);
    }
    public static void d(String tag, String msg, Throwable e)
    {
        getInstance().log(android.util.Log.DEBUG, tag, msg, e);
    }

    //selective log variant
    public static void ds(String msg)
    {
        if (BuildConfig.DEBUG)
            getInstance().log(android.util.Log.DEBUG, msg, null);
    }
    public static void ds(String msg, Throwable e)
    {
        if (BuildConfig.DEBUG)
            getInstance().log(android.util.Log.DEBUG, msg, e);
    }
    public static void ds(String tag, String msg)
    {
        if (BuildConfig.DEBUG)
            getInstance().log(android.util.Log.DEBUG, tag, msg, null);
    }
    public static void ds(String tag, String msg, Throwable e)
    {
        if (BuildConfig.DEBUG)
            getInstance().log(android.util.Log.DEBUG, tag, msg, e);
    }

    public static void i(String msg)
    {
        getInstance().log(android.util.Log.INFO, msg, null);
    }

    public static void i(String msg, Throwable e)
    {
        getInstance().log(android.util.Log.INFO, msg, e);
    }
    public static void i(String tag, String msg)
    {
        getInstance().log(android.util.Log.INFO, tag, msg, null);
    }
    public static void i(String tag, String msg, Throwable e)
    {
        getInstance().log(android.util.Log.INFO, tag, msg, e);
    }

    public static void e(String msg)
    {
        getInstance().log(android.util.Log.ERROR, msg, null);
    }

    public static void e(String msg, Throwable e)
    {
        getInstance().log(android.util.Log.ERROR, msg, e);
    }
    public static void e(String tag, String msg)
    {
        getInstance().log(android.util.Log.ERROR, tag, msg, null);
    }
    public static void e(String tag, String msg, Throwable e)
    {
        getInstance().log(android.util.Log.ERROR, tag, msg, e);
    }

    public static void w(String msg)
    {
        getInstance().log(android.util.Log.WARN, msg, null);
    }
    public static void w(String msg, Throwable e)
    {
        getInstance().log(android.util.Log.WARN, msg, e);
    }
    public static void w(String tag, String msg)
    {
        getInstance().log(android.util.Log.WARN, tag, msg, null);
    }
    public static void w(String tag, String msg, Throwable e)
    {
        getInstance().log(android.util.Log.WARN, tag, msg, e);
    }

    public static void v(String msg)
    {
        getInstance().log(android.util.Log.VERBOSE, msg, null);
    }
    public static void v(String msg, Throwable e)
    {
        getInstance().log(android.util.Log.VERBOSE, msg, e);
    }
    public static void v(String tag, String msg)
    {
        getInstance().log(android.util.Log.VERBOSE, tag, msg, null);
    }
    public static void v(String tag, String msg, Throwable e)
    {
        getInstance().log(android.util.Log.VERBOSE, tag, msg, e);
    }
}
