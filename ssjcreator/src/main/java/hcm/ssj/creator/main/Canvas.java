/*
 * Canvas.java
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

package hcm.ssj.creator.main;

import android.content.Context;
import android.view.View;

import hcm.ssj.creator.R;
import hcm.ssj.creator.util.Util;
import hcm.ssj.creator.view.PipeListener;
import hcm.ssj.creator.view.PipeView;

/**
 * Canvas tab for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
class Canvas implements ITab
{
    //tab
    private View view;
    private String title;
    private int icon;
    //canvas
    private PipeView pipeView = null;
    private PipeListener listener = null;

    /**
     * @param context Context
     */
    Canvas(Context context)
    {
        //view
        pipeView = new PipeView(context);
        pipeView.setWillNotDraw(false);
        TwoDScrollView twoDScrollView = new TwoDScrollView(context);
        twoDScrollView.setHorizontalScrollBarEnabled(true);
        twoDScrollView.setVerticalScrollBarEnabled(true);
        twoDScrollView.addView(pipeView);
        view = twoDScrollView;
        //title
        title = context.getResources().getString(R.string.str_pipe);
        //icon
        icon = android.R.drawable.ic_menu_edit;
    }

    /**
     *
     */
    void actualizeContent(Util.ButtonAction buttonAction)
    {
        if (pipeView != null)
        {
            pipeView.recalculate(buttonAction);
        }
    }

    /**
     *
     */
    void cleanUp()
    {
        if (pipeView != null && listener != null)
        {
            pipeView.removeViewListener(listener);
            listener = null;
        }
    }

    /**
     * @param listener PipeListener
     */
    void init(PipeListener listener)
    {
        this.listener = listener;
        pipeView.addViewListener(this.listener);
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
