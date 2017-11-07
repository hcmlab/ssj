/*
 * Classifier.java
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

package hcm.ssj.ml;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Locale;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.event.StringEvent;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileUtils;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.Selector;

/**
 * Generic classifier
 */
public class Classifier extends Consumer implements IModelHandler
{
    /**
     * All options for the consumer
     */
    public class Options extends IModelHandler.Options
    {
        public final Option<Boolean> merge = new Option<>("merge", true, Boolean.class, "merge input streams");
        public final Option<Boolean> bestMatchOnly = new Option<>("bestMatchOnly", true, Boolean.class, "print or send class with highest result only");
        public final Option<Boolean> log = new Option<>("log", true, Boolean.class, "print results in log");
        public final Option<String> sender = new Option<>("sender", "Classifier", String.class, "event sender name, written in every event");
        public final Option<String> event = new Option<>("event", "Result", String.class, "event name");

        private Options()
        {
            super();
            addOptions();
        }
    }

    public final Options options = new Options();

    private Selector selector = null;
    private Stream[] stream_merged;
    private Stream[] stream_selected;
    private Merge merge = null;
    private ModelDescriptor modelDescriptor = null;

    public Classifier()
    {
        _name = this.getClass().getSimpleName();
    }

    @Override
    public void init(Stream stream_in[]) throws SSJException
    {
        try {
            if(options.modelSource.get() != null)
            {
                modelDescriptor = new ModelDescriptor(options.modelSource.get());
            }
            else if(options.trainerFile.get() != null && !options.trainerFile.get().isEmpty())
            {
                modelDescriptor = new ModelDescriptor(FileUtils.getFile(options.trainerPath.get(), options.trainerFile.get()));
            }
            else
            {
                throw new IOException("neither model source nor trainer file has been provided");
            }
        } catch (IOException | XmlPullParserException e) {
            throw new SSJException("unable to load trainer file", e);
        }
    }

    /**
     * @param stream_in  Stream[]
     */
    @Override
    public void enter(Stream[] stream_in) throws SSJFatalException
    {
        if (stream_in.length > 1 && !options.merge.get())
        {
            throw new SSJFatalException("sources count not supported");
        }

        if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
        {
            throw new SSJFatalException("stream type not supported");
        }

        try
        {
            Log.d("loading model ...");
            modelDescriptor.loadModel(options.trainerPath.get());
            Log.d("model loaded");
        }
        catch (IOException e)
        {
            throw new SSJFatalException("unable to load model", e);
        }

        Stream[] input = stream_in;

        if(options.merge.get())
        {
            merge = new Merge();
            stream_merged = new Stream[1];
            stream_merged[0] = Stream.create(input[0].num, merge.getSampleDimension(input), input[0].sr, input[0].type);
            merge.enter(stream_in, stream_merged[0]);
            input = stream_merged;
        }

        try
        {
            modelDescriptor.validateInput(input);
        }
        catch (IOException e)
        {
            throw new SSJFatalException("model validation failed", e);
        }

        if(selector != null)
        {
            stream_selected = new Stream[1];
            stream_selected[0] = Stream.create(input[0].num, selector.options.values.get().length, input[0].sr, input[0].type);
            selector.enter(input, stream_selected[0]);
        }
    }

    /**
     * @param stream_in  Stream[]
	 * @param trigger
     */
    @Override
    public void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        Stream[] input = stream_in;

        if(options.merge.get()) {
            merge.transform(input, stream_merged[0]);
            input = stream_merged;
        }
        if(selector != null) {
            selector.transform(input, stream_selected[0]);
            input = stream_selected;
        }

        float[] probs = modelDescriptor.getModel().forward(input);

        if(options.bestMatchOnly.get())
        {
            // Get array index of element with largest probability.
            int bestLabelIdx = Util.maxIndex(probs);
            String bestMatch = String.format(Locale.GERMANY, "BEST MATCH: %s (%.2f%% likely)",
                                             modelDescriptor.getClassNames()[bestLabelIdx],
                                             probs[bestLabelIdx] * 100f);

            if (_evchannel_out != null)
            {
                Event ev = new StringEvent(bestMatch);
                ev.sender = options.sender.get();
                ev.name = options.event.get();
                ev.time = (int) (1000 * stream_in[0].time + 0.5);
                double duration = stream_in[0].num / stream_in[0].sr;
                ev.dur = (int) (1000 * duration + 0.5);
                ev.state = Event.State.COMPLETED;

                _evchannel_out.pushEvent(ev);
            }

            if (options.log.get())
            {
                Log.i(bestMatch);
            }
        }
        else
        {
            if (_evchannel_out != null)
            {
                Event ev = Event.create(Cons.Type.FLOAT);
                ev.sender = options.sender.get();
                ev.name = options.event.get();
                ev.time = (int) (1000 * stream_in[0].time + 0.5);
                double duration = stream_in[0].num / stream_in[0].sr;
                ev.dur = (int) (1000 * duration + 0.5);
                ev.state = Event.State.COMPLETED;
                ev.setData(probs);

                _evchannel_out.pushEvent(ev);
            }

            if (options.log.get())
            {
                String[] class_names = modelDescriptor.getClassNames();
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < probs.length; i++)
                {
                    stringBuilder.append(class_names[i]);
                    stringBuilder.append(" = ");
                    stringBuilder.append(probs[i]);
                    stringBuilder.append("; ");
                }

                Log.i(stringBuilder.toString());
            }
        }
    }

    @Override
    public ModelDescriptor getModelDescriptor()
    {
        return modelDescriptor;
    }

    public boolean hasReferableModel()
    {
        return (options.trainerFile.get() != null
                && options.trainerPath.get() != null
                && !options.trainerFile.get().isEmpty()
                && !options.trainerPath.get().isEmpty());
    }
}
