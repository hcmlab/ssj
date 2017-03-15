/*
 * TabHandler.java
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

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TabHost;

import com.jjoe64.graphview.GraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hcm.ssj.camera.CameraPainter;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.Util;
import hcm.ssj.creator.view.PipeListener;
import hcm.ssj.file.IFileWriter;
import hcm.ssj.graphic.SignalPainter;

/**
 * ITab handler for main activity.<br>
 * Created by Frank Gaibler on 23.09.2016.
 */
public class TabHandler
{
    private Activity activity;
    //tabs
    private TabHost tabHost = null;
    private ArrayList<Object> alAdditionalTabs = new ArrayList<>();
    private ArrayList<TabHost.TabSpec> alTabSpecs = new ArrayList<>();
    private final static int FIX_TAB_NUMBER = 2;
    //canvas
    private Canvas canvas;
    //console
    private Console console;
    //annotation
    private boolean annotationExists = false;

    private Annotation annotation = null;

    /**
     * @param activity Activity
     */
    public TabHandler(Activity activity)
    {
        this.activity = activity;
        tabHost = (TabHost) activity.findViewById(R.id.id_tabHost);
        if (tabHost != null)
        {
            tabHost.setup();
            //canvas
            canvas = new Canvas(this.activity);
            addTab(canvas.getView(), canvas.getTitle(), canvas.getIcon());
            //console
            console = new Console(this.activity);
            addTab(console.getView(), console.getTitle(), console.getIcon());
            //init tabs
            canvas.init(new PipeListener()
            {
                @Override
                public void viewChanged()
                {
                    checkAdditionalTabs();
                }
            });
            console.init();
        }
    }

    /**
     * @param view    View
     * @param tabName String
     * @param image   int
     */
    private void addTab(final View view, final String tabName, int image)
    {
        final TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabName);
        tabSpec.setContent(new TabHost.TabContentFactory()
        {
            /**
             * @param tag String
             * @return View
             */
            public View createTabContent(String tag)
            {
                return view;
            }
        });
        tabSpec.setIndicator("", ContextCompat.getDrawable(activity, image));
        tabHost.addTab(tabSpec);
        //necessary to reset tab strip
        int tab = tabHost.getCurrentTab();
        tabHost.setCurrentTab(tabHost.getTabWidget().getTabCount() - 1);
        tabHost.setCurrentTab(tab);
        alTabSpecs.add(tabSpec);
    }

    /**
     * Adds annotation tab after fixed tabs
     *
     * @param view    View
     * @param tabName String
     * @param image   int
     */
    private void addTabAnno(final View view, final String tabName, int image)
    {
        final TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabName);
        tabSpec.setContent(new TabHost.TabContentFactory()
        {
            /**
             * @param tag String
             * @return View
             */
            public View createTabContent(String tag)
            {
                return view;
            }
        });
        tabSpec.setIndicator("", ContextCompat.getDrawable(activity, image));
        alTabSpecs.add(FIX_TAB_NUMBER, tabSpec);
        //
        int current = tabHost.getCurrentTab();
        tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();
        for (TabHost.TabSpec tab : alTabSpecs)
        {
            tabHost.addTab(tab);
        }
        tabHost.setCurrentTab(current == 1 ? 1 : 0);
    }

    /**
     * @param tab int
     */
    private void removeTab(final int tab)
    {
        int current = tabHost.getCurrentTab();
        alTabSpecs.remove(tab);
        tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();
        for (TabHost.TabSpec tabSpec : alTabSpecs)
        {
            tabHost.addTab(tabSpec);
        }
        tabHost.setCurrentTab(current == 1 ? 1 : 0);
    }

    /**
     * Add or remove additional tabs
     */
    private void checkAdditionalTabs()
    {
        Object[] consumers = PipelineBuilder.getInstance().getAll(PipelineBuilder.Type.Consumer);
        int counterAnno = 0;
        //add additional tabs
        for (Object object : consumers)
        {
            //annotation
            if (object instanceof IFileWriter)
            {
                counterAnno++;
                if (!annotationExists)
                {
                    annotationExists = true;
                    annotation = new Annotation(this.activity);
                    addTabAnno(annotation.getView(), annotation.getTitle(), annotation.getIcon());
                }
            }
            //signals
            else if (object instanceof SignalPainter)
            {
                GraphView graphView = ((SignalPainter) object).options.graphView.get();
                if (graphView == null)
                {
                    graphView = new GraphView(activity);
                    ((SignalPainter) object).options.graphView.set(graphView);
                    addTab(graphView, ((SignalPainter) object).getComponentName(), android.R.drawable.ic_menu_view);
                    alAdditionalTabs.add(object);
                }
            }
            //camera
            else if (object instanceof CameraPainter)
            {
                SurfaceView surfaceView = ((CameraPainter) object).options.surfaceView.get();
                if (surfaceView == null)
                {
                    surfaceView = new SurfaceView(activity);
                    ((CameraPainter) object).options.surfaceView.set(surfaceView);
                    addTab(surfaceView, ((CameraPainter) object).getComponentName(), android.R.drawable.ic_menu_camera);
                    alAdditionalTabs.add(object);
                }
            }
        }
        //remove obsolete tabs
        //remove annotation
        if (counterAnno <= 0 && annotationExists)
        {
            annotationExists = false;
            removeTab(FIX_TAB_NUMBER);
        }
        //remove signal and camera
        List list = Arrays.asList(consumers);
        for (int i = alAdditionalTabs.size() - 1; i >= 0; i--)
        {
            if (!list.contains(alAdditionalTabs.get(i)))
            {
                alAdditionalTabs.remove(i);
                removeTab(i + FIX_TAB_NUMBER + (annotationExists ? 1 : 0));
            }
        }
    }

    /**
     *
     */
    public void preStart()
    {
        console.clear();

        if (annotationExists && annotation != null)
        {
            annotation.startAnnotation();
        }
    }

    /**
     *
     */
    public void preStop()
    {
        if (annotationExists && annotation != null)
        {
            annotation.finishAnnotation();
        }
    }

    /**
     * @param appAction Util.AppAction
     * @param o            Object
     */
    public void actualizeContent(Util.AppAction appAction, Object o)
    {
        canvas.actualizeContent(appAction, o);
    }

    /**
     *
     */
    public void cleanUp()
    {
        console.cleanUp();
        canvas.cleanUp();
    }

    /**
     * @return Annotation
     */
    public Annotation getAnnotation()
    {
        return annotation;
    }
}
