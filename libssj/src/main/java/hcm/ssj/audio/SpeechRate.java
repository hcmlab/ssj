/*
 * SpeechRate.java
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

package hcm.ssj.audio;

import java.util.Iterator;
import java.util.LinkedList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Computes the SpeechRate of the input signal
 * Algorithm by Nivja H. De Jong
 * - uses audio processing tools from TarsosDSP
 * Created by Johnny on 05.03.2015.
 */
public class SpeechRate extends Consumer
{
    public class Options extends OptionList
    {
        public final Option<String> sender = new Option<>("sender", _name, String.class, "");
        public final Option<String> event = new Option<>("event", "SpeechRate", String.class, "");
        public final Option<Float> thresholdVoicedProb = new Option<>("thresholdVoicedProb", 0.5f, Float.class, "in Hz");
        public final Option<Float> intensityIgnoranceLevel = new Option<>("intensityIgnoranceLevel", 1.0f, Float.class, "in dB");
        public final Option<Float> minDipBetweenPeaks = new Option<>("minDipBetweenPeaks", 3.0f, Float.class, "in dB");
        public final Option<Integer> width = new Option<>("width", 3, Integer.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    private Stream _intensity = null;
    private Stream _voiced = null;
    private int _intensity_ind = -1;
    private int _voiced_ind = -1;

    private float[] _tmp = new float[512];

    public SpeechRate()
    {
        _name = "SpeechRate";
    }

    @Override
    public void enter(Stream[] stream_in)
    {
        for(Stream s : stream_in)
        {
            if (_intensity_ind < 0)
            {
                _intensity_ind = s.findDataClass("Intensity");
                if (_intensity_ind >= 0)
                    _intensity = s;
            }
            if (_voiced_ind < 0)
            {
                _voiced_ind = s.findDataClass("VoicedProb");
                if (_voiced_ind >= 0)
                    _voiced = s;
            }
        }

        if((_intensity == null || _intensity.type != Cons.Type.FLOAT)
        || (_voiced == null || _voiced.type != Cons.Type.FLOAT))
        {
            Log.e("invalid input configuration. SPL Energy (double) and VoicedProb (float) is required.");
            return;
        }

        if(_evchannel_out == null)
            Log.e("no outgoing event channel has been registered");
    }

    @Override
    protected void consume(Stream[] stream_in)
    {
        float[] intensity = _intensity.select(_intensity_ind).ptrF();
        float[] voiced = _voiced.ptrF();

        Log.ds("computing sr for " + _intensity.num + " samples");

        //compute peaks
        LinkedList<Integer> peaks = findPeaks(intensity, _intensity.num, options.intensityIgnoranceLevel.get(), options.minDipBetweenPeaks.get());

        Log.ds("peaks (pre-cull) = " + peaks.size());

        //remove non-voiced peaks
        Iterator<Integer> peak = peaks.iterator();
        int i,j;
        double t;
        while (peak.hasNext())
        {
            i = peak.next();

            t = _intensity.time + i * _intensity.step;
            j = (int)((t - _voiced.time) / _voiced.step);

            if(j >= _voiced.num)
                j = _voiced.num - 1;

            if (voiced[j * _voiced.dim + _voiced_ind] < options.thresholdVoicedProb.get())
                peak.remove();
        }

        double duration = stream_in[0].num / stream_in[0].sr;


        Log.ds("peaks = " + peaks.size() + ", sr = " + peaks.size() / duration);

        Event ev = Event.create(Cons.Type.STRING);
        ev.sender = options.sender.get();
        ev.name = options.event.get();
        ev.time = (int)(1000 * stream_in[0].time + 0.5);
        ev.dur = (int)(1000 * duration + 0.5);
        ev.state = Event.State.COMPLETED;
        ev.setData("<tuple string=\"Speechrate (syllables/sec)\" value=\""+ (peaks.size() / duration) +"\" />");
        _evchannel_out.pushEvent(ev);
    }


    @Override
    public void flush(Stream[] stream_in)
    {}

    /**
     * Reimplementaiton of Jong and Wempe's PRAAT peak detector for speech rate analysis as described in
     * N.H. De Jong and T. Wempe, Praat script to detect syllable nuclei and measure speech rate automatically, 2009, doi:10.3758/BRM.41.2.385
     * @param data audio intensity
     * @param threshold threshold to be applied above median
     */
    private LinkedList<Integer> findPeaks(float[] data, int length, double threshold, double minDip)
    {
        float min = Util.min(data, 0, length);

        if(_tmp.length < length)
            _tmp = new float[length];
        System.arraycopy(data, 0, _tmp, 0, length);
        double med = Util.median(_tmp, 0, length);

        threshold += med;
        if(threshold  < min)
            threshold = min;

        int width = options.width.get();
        if(width == 0) width = 1;
        LinkedList<Integer> peaks = findPeaks_(data, length, width, threshold);
        if(peaks.size() == 0)
            return peaks;

        Log.ds("peaks (pre-mindip-cull) = " + peaks.size());

        Iterator<Integer> i = peaks.iterator();
        int prev = i.next();
        int current;
        double minLocal;
        while(i.hasNext())
        {
            current = i.next();

            //find min between the two peaks
            minLocal = Util.min(data, prev, current - prev);

            if(Math.abs(data[current] - minLocal) <= minDip)
                i.remove();
            else
                prev = current;
        }

        return peaks;
    }

    private LinkedList<Integer> findPeaks_(float[] data, int length, int width, double threshold) {
        return findPeaks_(data, length, width, threshold, 0.0D, false);
    }

    private LinkedList<Integer> findPeaks_(float[] data, int length, int width, double threshold, double decayRate, boolean isRelative) {
        LinkedList<Integer> peaks = new LinkedList<>();
        int mid = 0;
        int end = length;

        for(double av = data[0]; mid < end; ++mid) {
            av = decayRate * av + (1.0D - decayRate) * data[mid];
            if(av < data[mid]) {
                av = data[mid];
            }

            int i = mid - width;
            if(i < 0) {
                i = 0;
            }

            int stop = mid + width + 1;
            if(stop > length) {
                stop = length;
            }

            int var15;
            for(var15 = i++; i < stop; ++i) {
                if(data[i] > data[var15]) {
                    var15 = i;
                }
            }

            if(var15 == mid) {
                if(overThreshold(data, length, var15, width, threshold, isRelative, av)) {
                    peaks.add(var15);
                }
            }
        }

        return peaks;
    }

    boolean overThreshold(float[] data, int length, int index, int width, double threshold, boolean isRelative, double av)
    {
//        Log.i(index + " : " + data[index] +"\tAv1: " + av);

        if(data[index] < av) {
            return false;
        } else if(!isRelative) {
            return data[index] > threshold;
        } else {
            int iStart = index - 3 * width;
            if(iStart < 0) {
                iStart = 0;
            }

            int iStop = index + 1 * width;
            if(iStop > length) {
                iStop = length;
            }

            double sum = 0.0D;

            int count;
            for(count = iStop - iStart; iStart < iStop; iStart++) {
                sum += data[iStart];
            }

//            Log.i("\t" + (sum / (double) count) + "\t" + (data[index] - sum / (double) count - threshold));
            return data[index] > sum / (double)count + threshold;
        }
    }
}
