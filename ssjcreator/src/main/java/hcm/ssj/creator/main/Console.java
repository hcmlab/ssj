/*
 * Console.java
 * Copyright (c) 2018
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

package hcm.ssj.creator.main;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.creator.R;
import hcm.ssj.file.FileCons;

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

                    //Check if scrollView is at the very bottom.
                    boolean scrollDown = false;
                    if(view instanceof ConsoleScrollView)
                    {
                        scrollDown = !view.canScrollVertically(1) && ((ConsoleScrollView)view).isBottom;
                    }

                    handlerLog.post(runnableLog);
                    Thread.sleep(sleepTime);

                    // Scroll to the bottom if it was on bottom before posting the new message
                    if(view instanceof ScrollView && scrollDown)
                    {
                        ((ScrollView) view).fullScroll(ScrollView.FOCUS_DOWN);
                    }
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
        private final int max = 10000;
        private final int interim = max / 2;

        /**
         * @param msg String
         */
        public void msg(int type, String msg)
        {
            strLogMsg += (type > 0 && type < tags.length ? tags[type] : type) + "/" + Cons.LOGTAG + ": " + msg + FileCons.DELIMITER_LINE;
            int length = strLogMsg.length();
            if (length > max)
            {
                strLogMsg = strLogMsg.substring(length - interim);
                strLogMsg = strLogMsg.substring(strLogMsg.indexOf(FileCons.DELIMITER_LINE) + FileCons.DELIMITER_LINE.length());
            }
        }
    };

    private class ConsoleScrollView extends ScrollView
    {
        public boolean isBottom = false;

        public ConsoleScrollView(Context context)
        {
            super(context);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt)
        {
            // Grab the last child placed in the ScrollView, we need it to determinate the bottom position.
            View view = getChildAt(0);

            // Calculate the scrolldiff
            int diff = (view.getBottom()-(getHeight()+getScrollY()));

            // if diff is zero, then the bottom has been reached
            if( diff <= 0 )
            {
                // notify that we have reached the bottom
                isBottom = true;
            }
            else
            {
                isBottom = false;
            }

            super.onScrollChanged(l, t, oldl, oldt);
        }
    }

    /**
     * @param context Context
     */
    Console(Context context)
    {
        //view
        textViewConsole = new TextView(context);
        ConsoleScrollView scrollView = new ConsoleScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(textViewConsole);
        view = scrollView;
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
