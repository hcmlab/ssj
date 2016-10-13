/*
 * SignalPainter.java
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

import android.os.Handler;
import android.os.Looper;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 26.05.2015.
 */
public class SignalPainter extends Consumer
{
    public class Options extends OptionList
    {
        public final Option<int[]> colors = new Option<>("colors", new int[]{0xff0077cc, 0xffff9900, 0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900}, int[].class, "");
        public final Option<Double> size = new Option<>("size", 10., Double.class, "in seconds");
        public final Option<Boolean> legend = new Option<>("legend", true, Boolean.class, "");
        public final Option<Boolean> manualBounds = new Option<>("manualBounds", false, Boolean.class, "");
        public final Option<Double> min = new Option<>("min", 0., Double.class, "");
        public final Option<Double> max = new Option<>("max", 1., Double.class, "");
        public final Option<Integer> secondScaleStream = new Option<>("secondScaleStream", 1, Integer.class, "stream id to put on the secondary scale (use -1 to disable)");
        public final Option<Integer> secondScaleDim = new Option<>("secondScaleDim", 0, Integer.class, "put a dimension on the secondary scale (use -1 to disable)");
        public final Option<Double> secondScaleMin = new Option<>("secondScaleMin", 0., Double.class, "");
        public final Option<Double> secondScaleMax = new Option<>("secondScaleMax", 1., Double.class, "");
        public final Option<Integer> numVLabels = new Option<>("numVLabels", 2, Integer.class, "");
        public final Option<Integer> numHLabels = new Option<>("numHLabels", 2, Integer.class, "");
        public final Option<Boolean> renderMax = new Option<>("renderMax", true, Boolean.class, "");
        public final Option<GraphView> graphView = new Option<>("graphView", null, GraphView.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public Options options = new Options();

    private ArrayList<LineGraphSeries<DataPoint>> _series = new ArrayList<>();

    GraphView _view = null;
    int[] _maxPoints;

    public SignalPainter()
    {
        _name = "SignalPainter";
        _doWakeLock = false; //since this is a GUI element, disable wakelock to save energy
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected void init(Stream[] stream_in)
    {
        super.init(stream_in);
        if (options.graphView.get() == null)
        {
            Log.w("graphView isn't set");
        }
        else
        {
            _view = options.graphView.get();
        }
    }
    @Override
    public void enter(Stream[] stream_in)
    {
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

        if(stream_in.length > 2)
        {
            Log.w("plotting more than 2 streams per graph will not work if streams are not similar");
        }

        int dimTotal = 0;
        for(Stream s : stream_in)
            dimTotal += s.dim;

        _maxPoints = new int[dimTotal];

        _view.getViewport().setXAxisBoundsManual(true);
        _view.getViewport().setMinX(0);
        _view.getViewport().setMaxX(options.size.get());

        _view.getViewport().setYAxisBoundsManual(options.manualBounds.get());
        _view.getViewport().setMaxY(options.max.get());
        _view.getViewport().setMinY(options.min.get());

        _view.getGridLabelRenderer().setNumHorizontalLabels(options.numHLabels.get());
        _view.getGridLabelRenderer().setNumVerticalLabels(options.numVLabels.get());

        _view.getLegendRenderer().setVisible(options.legend.get());
        _view.getLegendRenderer().setFixedPosition(10, 10);

        createSeries(_view, stream_in);
    }

    @Override
    protected void consume(Stream[] stream_in)
    {
        int seriesID = 0;
        for (int k = 0; k < stream_in.length; k++)
        {
            for (int i = 0; i < stream_in[k].dim; i++)
            {
                switch (stream_in[k].type)
                {
                    case CHAR:
                    {
                        char[] in = stream_in[k].ptrC();
                        char max = Character.MIN_VALUE;
                        char value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    case SHORT:
                    {
                        short[] in = stream_in[k].ptrS();
                        short max = Short.MIN_VALUE;
                        short value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    case INT:
                    {
                        int[] in = stream_in[k].ptrI();
                        int max = Integer.MIN_VALUE;
                        int value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    case LONG:
                    {
                        long[] in = stream_in[k].ptrL();
                        long max = Long.MIN_VALUE;
                        long value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    case FLOAT:
                    {
                        float[] in = stream_in[k].ptrF();
                        float max = -1 * Float.MAX_VALUE;
                        float value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    case DOUBLE:
                    {
                        double[] in = stream_in[k].ptrD();
                        double max = -1 * Double.MAX_VALUE;
                        double value;
                        double time = stream_in[k].time;
                        for (int j = 0; j < stream_in[k].num; j++, time += stream_in[k].step)
                        {
                            value = in[j * stream_in[k].dim + i];

                            if (!options.renderMax.get())
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax.get())
                            pushData(seriesID, max, stream_in[k].time);
                        break;
                    }

                    default:
                        Log.w("unsupported data type");
                        return;
                }
                seriesID++;
            }
        }
    }

    @Override
    public void flush(Stream[] stream_in) {

        _series.clear();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                _view.removeAllSeries();
                _view.clearSecondScale();
                _view = null;
            }
        }, 1);
    }

    private void pushData(final int seriesID, double value, double time)
    {
        //apparently GraphView can't render infinity
        if(Double.isNaN(value) || Double.isInfinite(value) || value == -1 * Double.MAX_VALUE  || value == Double.MAX_VALUE)
            return;

        final DataPoint p = new DataPoint(time, value);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                if(seriesID < _series.size())
                    _series.get(seriesID).appendData(p, true, _maxPoints[seriesID]);
            }
        }, 1);
    }

    private void createSeries(final GraphView view, final Stream[] stream_in)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                for (int i = 0; i < stream_in.length; i++)
                {
                    for (int j = 0; j < stream_in[i].dim; j++)
                    {
                        LineGraphSeries<DataPoint> s = new LineGraphSeries<>();
                        s.setTitle(stream_in[i].dataclass[j]);
                        s.setColor(options.colors.get()[_series.size() % options.colors.get().length]);

                        //define scale length
                        if(!options.renderMax.get())
                            _maxPoints[_series.size()] = (int)(options.size.get() * stream_in[i].sr) +1;
                        else
                            _maxPoints[_series.size()] = (int)(options.size.get() * (stream_in[i].sr / (double)stream_in[i].num)) +1;

                        _series.add(s);

                        if (options.secondScaleStream.get() == i && options.secondScaleDim.get() == j)
                        {
                            view.getSecondScale().setMinY(options.secondScaleMin.get());
                            view.getSecondScale().setMaxY(options.secondScaleMax.get());
                            view.getSecondScale().addSeries(s);
                        } else
                        {
                            _view.addSeries(s);
                        }
                    }
                }
            }
        }, 1);
    }
}
