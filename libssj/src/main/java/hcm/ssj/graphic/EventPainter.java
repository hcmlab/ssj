/*
 * EventPainter.java
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

package hcm.ssj.graphic;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Johnny on 26.05.2015.
 */
public class EventPainter extends EventHandler
{
    public class Options extends OptionList
    {
        public final Option<String> title = new Option<>("title", "events", String.class, "");
        public final Option<Integer> color = new Option<>("color", 0xff0077cc, Integer.class, "");
        public final Option<Integer> numBars = new Option<>("numBars", 2, Integer.class, "");
        public final Option<Integer> spacing = new Option<>("spacing", 50, Integer.class, "space between bars");
        public final Option<GraphView> graphView = new Option<>("graphView", null, GraphView.class, "");        public final Option<Boolean> manualBounds = new Option<>("manualBounds", false, Boolean.class, "");
        public final Option<Double> min = new Option<>("min", 0., Double.class, "");
        public final Option<Double> max = new Option<>("max", 1., Double.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public Options options = new Options();

    private BarGraphSeries<DataPoint> _series;
    GraphView _view = null;

    public EventPainter()
    {
        _name = "EventPainter";
        _doWakeLock = false; //since this is a GUI element, disable wakelock to save energy
    }

    @Override
    public void enter()
    {
        if (options.graphView.get() == null)
        {
            Log.w("graphView isn't set");
        }
        else
        {
            _view = options.graphView.get();
        }

        synchronized (this)
        {
            if (_view == null)
            {
                //wait for graphView creation
                try
                {
                    this.wait();
                } catch (InterruptedException ex)
                {
                    Log.e("graph view not registered");
                }
            }
        }

        _view.getViewport().setXAxisBoundsManual(true);
        _view.getViewport().setMinX(0);
        _view.getViewport().setMaxX(options.numBars.get()+1);

        _view.getViewport().setYAxisBoundsManual(options.manualBounds.get());
        _view.getViewport().setMaxY(options.max.get());
        _view.getViewport().setMinY(options.min.get());

        _view.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        _view.getGridLabelRenderer().setHorizontalAxisTitle(options.title.get());
        _view.getGridLabelRenderer().setNumVerticalLabels(2);
        _view.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        _view.getLegendRenderer().setVisible(false);

        createSeries(_view, options.spacing.get());
    }

    @Override
    public void notify(Event event) {

        DataPoint[] points = new DataPoint[options.numBars.get()];

        for (int i = 0; i < options.numBars.get(); i++) {
            switch (event.type) {
                case BYTE:
                    points[i] = new DataPoint(i+1, event.ptrB()[i]);
                    break;
                case SHORT:
                    points[i] = new DataPoint(i+1, event.ptrShort()[i]);
                    break;
                case INT:
                    points[i] = new DataPoint(i+1, event.ptrI()[i]);
                    break;
                case LONG:
                    points[i] = new DataPoint(i+1, event.ptrL()[i]);
                    break;
                case FLOAT:
                    points[i] = new DataPoint(i+1, event.ptrF()[i]);
                    break;
                case DOUBLE:
                    points[i] = new DataPoint(i+1, event.ptrD()[i]);
                    break;
                default:
                    Log.w("unsupported even type");
                    return;
            }
        }

        pushData(points);
    }

    @Override
    public void flush() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                _view.removeAllSeries();
                _view.clearSecondScale();
                _view = null;
            }
        }, 1);
    }

    private void pushData(final DataPoint[] points)
    {
        for(DataPoint p : points)
            if(Double.isNaN(p.getY()) || Double.isInfinite(p.getY()) || p.getY() == -1 * Double.MAX_VALUE || p.getY() == Double.MAX_VALUE)
                return; //apparently GraphView can't render infinity

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                _series.resetData(points);
            }
        }, 1);
    }

    private void createSeries(final GraphView view, final int spacing)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                _series = new BarGraphSeries<>();
                _series.setColor(options.color.get());
                _series.setDrawValuesOnTop(true);
                _series.setValuesOnTopColor(Color.BLACK);
                _series.setSpacing(spacing);

                view.addSeries(_series);
            }
        }, 1);
    }
}
