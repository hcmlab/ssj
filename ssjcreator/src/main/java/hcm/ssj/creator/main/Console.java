/*
 * Console.java
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

package hcm.creator.main;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import hcm.creator.R;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.file.LoggingConstants;

/**
 * Console tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
class Console implements ITab
{
    //tab
    private View view;
    private String title;
    private int icon;
    //console
    private TextView textViewConsole = null;
    private String strLogMsg = "";
    private boolean handleLogMessages = false;
    private Thread threadLog = new Thread()
    {
        private final int sleepTime = 100;
        private Handler handlerLog = new Handler(Looper.getMainLooper());
        private Runnable runnableLog = new Runnable()
        {
            public void run()
            {
                if (textViewConsole != null)
                {
                    textViewConsole.setText(strLogMsg);
                }
            }
        };

        @Override
        public void run()
        {
            while (handleLogMessages)
            {
                try
                {
                    handlerLog.post(runnableLog);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    };
    private Log.LogListener logListener = new Log.LogListener()
    {
        private String[] tags = {"0", "1", "V", "D", "I", "W", "E", "A"};
        private final int max = 100000;
        private final int interim = 100000 / 2;

        /**
         * @param msg String
         */
        public void msg(int type, String msg)
        {
            strLogMsg += (type > 0 && type < tags.length ? tags[type] : type) + "/" + Cons.LOGTAG + ": " + msg + LoggingConstants.DELIMITER_LINE;
            int length = strLogMsg.length();
            if (length > max)
            {
                strLogMsg = strLogMsg.substring(length - interim);
                strLogMsg = strLogMsg.substring(strLogMsg.indexOf(LoggingConstants.DELIMITER_LINE) + LoggingConstants.DELIMITER_LINE.length());
            }
        }
    };

    /**
     * @param context Context
     */
    Console(Context context)
    {
        //view
        ScrollView scrollViewConsole = new ScrollView(context);
        textViewConsole = new TextView(context);
        scrollViewConsole.addView(textViewConsole);
        view = scrollViewConsole;
        //title
        title = context.getResources().getString(R.string.str_console);
        //icon
        icon = android.R.drawable.ic_menu_recent_history;
    }

    /**
     *
     */
    void clear()
    {
        strLogMsg = "";
    }

    /**
     *
     */
    void cleanUp()
    {
        handleLogMessages = false;
        Log.removeLogListener(logListener);
    }

    /**
     *
     */
    void init()
    {
        Log.addLogListener(logListener);
        handleLogMessages = true;
        threadLog.start();
    }

    /**
     * @return View
     */
    @Override
    public View getView()
    {
        return view;
    }

    /**
     * @return String
     */
    @Override
    public String getTitle()
    {
        return title;
    }

    /**
     * @return int
     */
    @Override
    public int getIcon()
    {
        return icon;
    }
}
