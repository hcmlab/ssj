/*
 * SignalPainter.java
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

package hcm.ssj.graphic;

import android.os.Handler;
import android.os.Looper;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 26.05.2015.
 */
public class SignalPainter extends Consumer
{
    public class Options
    {
        public int colors[] = {0xff0077cc, 0xffff9900, 0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
        public double size = 10.0; //in seconds
        public boolean legend = true;

        public boolean manualBounds = false;
        public double min = 0;
        public double max = 1;

        public int secondScaleStream = 1; //stream id to put on the secondary scale (use -1 to disable)
        public int secondScaleDim = 0; //stream dimension id to put on the secondary scale (use -1 to disable)
        public double secondScaleMin = 0;
        public double secondScaleMax = 1;

        public int numVLabels = 2;
        public int numHLabels = 2;

        public boolean renderMax = true;
    }
    public Options options = new Options();

    private ArrayList<LineGraphSeries<DataPoint>> _series = new ArrayList<>();

    GraphView _view = null;
    int[] _maxPoints;

    public SignalPainter()
    {
        _name = "SSJ_consumer_SignalPainter";
    }

    @Override
    public void enter(Stream[] stream_in)
    {
        if(_view == null)
        {
            Log.e("graph view not registered");
            return;
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
        _view.getViewport().setMaxX(options.size);

        _view.getViewport().setYAxisBoundsManual(options.manualBounds);
        _view.getViewport().setMaxY(options.max);
        _view.getViewport().setMinY(options.min);

        _view.getGridLabelRenderer().setNumHorizontalLabels(options.numHLabels);
        _view.getGridLabelRenderer().setNumVerticalLabels(options.numVLabels);

        _view.getLegendRenderer().setVisible(options.legend);
        _view.getLegendRenderer().setFixedPosition(10, 10);

        _view.getGridLabelRenderer().setLabelVerticalWidth(100);

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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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

                            if (!options.renderMax)
                            {
                                pushData(seriesID, value, time);
                            } else if (value > max)
                                max = value;
                        }

                        if (options.renderMax)
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
                        s.setColor(options.colors[_series.size() % options.colors.length]);

                        //define scale length
                        if(!options.renderMax)
                            _maxPoints[_series.size()] = (int)(options.size * stream_in[i].sr) +1;
                        else
                            _maxPoints[_series.size()] = (int)(options.size * (stream_in[i].sr / (double)stream_in[i].num)) +1;

                        _series.add(s);

                        if (options.secondScaleStream == i && options.secondScaleDim == j)
                        {
                            view.getSecondScale().setMinY(options.secondScaleMin);
                            view.getSecondScale().setMaxY(options.secondScaleMax);
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

    public void registerGraphView(GraphView view)
    {
        _view = view;
    }
}
