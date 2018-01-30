/*
 * ThresholdEventSender.java
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

package hcm.ssj.event;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

public class ThresholdEventSender extends Consumer
{
    @Override
    public OptionList getOptions()
    {
        return options;
    }

    public class Options extends OptionList
    {
        public final Option<String> sender = new Option<>("sender", null, String.class, "name of event sender");
        public final Option<String> event = new Option<>("event", "event", String.class, "name of event");

        public final Option<float[]> thresin = new Option<>("threshold_in", null, float[].class, "thresholds (one per dimension) for starting the event.");
        public final Option<float[]> thresout = new Option<>("threshold_out", null, float[].class, "thresholds (one per dimension) for finishing the event (leave empty if same as threshold_in)");

        public final Option<Integer> hangin = new Option<>("hangin", 0, Integer.class, "number of samples over threshold_in to wait until starting event");
        public final Option<Integer> hangout = new Option<>("hangout", 0, Integer.class, "number of samples under threshold_out to wait until finishing event");

        public final Option<Double> loffset = new Option<>("loffset", 0., Double.class, "lower offset in seconds (will be substracted from event start time)");
        public final Option<Double> uoffset = new Option<>("uoffset", 0., Double.class, "upper offset in seconds (will be added to event end time)");

        public final Option<Double> maxdur = new Option<>("maxdur", 2., Double.class, "maximum duration of event (longer events will be split unless skip is true)");
        public final Option<Double> mindur = new Option<>("mindur", 0., Double.class, "minimum duration of event (shorter events will be ignored)");

        public final Option<Boolean> hard = new Option<>("hard", true, Boolean.class, "all dimensions must respect thresholds");
        public final Option<Boolean> skip = new Option<>("skip", false, Boolean.class, "ignore event if max duration is exceeded");
        public final Option<Boolean> eager = new Option<>("eager", false, Boolean.class, "send an event when the observation begins");
        public final Option<Boolean> eall = new Option<>("eall", true, Boolean.class, "forward incomplete events to event board, otherwise only complete events are sent");
        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    boolean _trigger_on;
    double _trigger_start = 0, _trigger_stop = 0;
    int _hangover_in = 0, _hangover_out = 0, _counter_in = 0, _counter_out = 0;
    int _samples_max_dur = 0, _counter_max_dur = 0;
    double _loffset = 0, _uoffset = 0;
    boolean _skip_on_max_dur = false;

    public ThresholdEventSender()
    {
        _name = "ThresholdEventSender";
        options.sender.set(_name);
    }

    @Override
	public void enter(Stream[] stream_in) throws SSJFatalException
    {
        int totaldim = 0;
        for(Stream s : stream_in)
        {
            totaldim += s.dim;
        }

        if(options.thresin.get() == null || options.thresin.get().length != totaldim)
        {
            throw new SSJFatalException("invalid threshold list. Expecting " + totaldim + " thresholds");
        }

        if(options.thresout.get() == null)
        {
            Log.w("thresout undefined, using thresin");
            options.thresout.set(options.thresin.get());
        }

        _trigger_on = false;
        _hangover_in = options.hangin.get();
        _hangover_out = options.hangout.get();
        _counter_in = _hangover_in;
        _counter_out = _hangover_out;
        _loffset = options.loffset.get();
        _uoffset = options.uoffset.get();
        _skip_on_max_dur = options.skip.get();
        _samples_max_dur = (int) (options.maxdur.get() * stream_in[0].sr);
    }

    @Override
    protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        double time = stream_in[0].time;
        double timeStep = 1 / stream_in[0].sr;

        boolean found_event;
        for (int i = 0; i < stream_in[0].num; i++) {

            //differentiate between onset and offset threshold
            float[] threshold = (!_trigger_on) ? options.thresin.get() : options.thresout.get();

            found_event = options.hard.get();
            int thresId = 0;
            for (int j = 0; j < stream_in.length; j++) {
                for (int k = 0; k < stream_in[j].dim; k++)
                {
                    int position = i * stream_in[j].dim + k;
                    boolean result = false;

                    switch(stream_in[j].type)
                    {
                        case BYTE:
                            result = (float)stream_in[j].ptrB()[ position ] > threshold[thresId];
                            break;
                        case FLOAT:
                            result = stream_in[j].ptrF()[ position ] > threshold[thresId];
                            break;
                        case DOUBLE:
                            result = (float)stream_in[j].ptrD()[ position ] > threshold[thresId];
                            break;
                        case SHORT:
                            result = stream_in[j].ptrS()[ position ] > (double)threshold[thresId];
                            break;
                        case INT:
                            result = (float)stream_in[j].ptrI()[ position ] > threshold[thresId];
                            break;
                        case LONG:
                            result = stream_in[j].ptrL()[ position ] > (long)threshold[thresId];
                            break;
                        default:
                            Log.e("unsupported input type");
                            break;
                    }

                    if (options.hard.get())
                    {
                        found_event = found_event && result;
                    }
                    else
                    {
                        found_event = found_event || result;
                    }

                    thresId++;
                }
            }

            if (!_trigger_on) {

                if (found_event) {

                    // possible start of a new event
                    if (_counter_in == _hangover_in) {

                        // store start time and init max dur counter
                        _trigger_start = time + i * timeStep;
                    }
                    // check if event start is proved
                    if (_counter_in-- == 0) {

                        // signal that event start is now proved and init hangout and max counter
                        _trigger_on = true;
                        _counter_out = _hangover_out;
                        _counter_max_dur = _samples_max_dur - _hangover_in;

                        if (options.eager.get()) {
                            Event ev = Event.create(Cons.Type.EMPTY); //empty event
                            ev.name = options.event.get();
                            ev.sender = options.sender.get();
                            ev.time = (int)(1000 * _trigger_start + 0.5);
                            ev.dur = 0;
                            ev.state = Event.State.CONTINUED;
                            _evchannel_out.pushEvent(ev);
                        }

                        Log.ds("event started at " + _trigger_start);
                    }

                } else {

                    // re-init hangin counter
                    _counter_in = _hangover_in;

                }
            } else if (_trigger_on) {

                // check if max dur is reached
                if (--_counter_max_dur == 0) {
                    // send event and reset start/stop time amd max counter
                    _trigger_stop = time + i * timeStep;

                    update (_trigger_start, _trigger_stop - _trigger_start, Event.State.CONTINUED);

                    _trigger_start = _trigger_stop;
                    _counter_max_dur = _samples_max_dur;
                }

                if (!found_event) {

                    // possible end of a new event
                    if (_counter_out == _hangover_out) {

                        // store end time
                        _trigger_stop = time + i * timeStep;

                    }
                    // check if event end is proved
                    if (_counter_out-- == 0) {

                        // event end is now proved and event is sent
                        update(_trigger_start, _trigger_stop - _trigger_start,
                               Event.State.COMPLETED);

                        // signal end of event and init hangin counter
                        _trigger_on = false;
                        _counter_in = _hangover_in;

                        Log.ds("event stopped at " + _trigger_stop);
                    }
                } else {
                    // re-init hangin counter
                    _counter_out = _hangover_out;
                }
            }
        }
    }

    @Override
    public void flush(Stream[] stream_in) throws SSJFatalException
    {}

    boolean update (double time, double dur, Event.State state)
    {
        if (dur < options.mindur.get() || dur <= 0.0) {
            Log.ds("skip event because duration too short " + dur + "@" + time);
            return false;
        }

        if (dur > options.maxdur.get() + 0.000000001) {
            if (_skip_on_max_dur) {
                Log.ds("skip event because duration too long " + dur + "@" + time);
                return false;
            }
            Log.w("crop duration from " + dur +" to " + options.maxdur.get());
            time += dur - options.maxdur.get();
            dur = options.maxdur.get();
        }

        if (options.eall.get() || state == Event.State.COMPLETED) {
            Event ev = Event.create(Cons.Type.EMPTY); //empty event
            ev.name = options.event.get();
            ev.sender = options.sender.get();
            ev.time = Math.max (0,  (int)(1000 * (time - _loffset) + 0.5));
            ev.dur = Math.max (0, (int)(1000 * (dur + _uoffset) + 0.5));
            ev.state = state;
            _evchannel_out.pushEvent(ev);
        }

        return true;
    }
}
