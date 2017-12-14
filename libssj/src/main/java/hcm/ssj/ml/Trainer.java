/*
 * Trainer.java
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

import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.Selector;

/**
 * Generic classifier
 */
public class Trainer extends Consumer implements IModelHandler
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     * All options for the consumer
     */
    public class Options extends IModelHandler.Options
    {
        public final Option<Boolean> merge = new Option<>("merge", true, Boolean.class, "merge input streams");

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
    private Model model = null;

    public Trainer()
    {
        _name = this.getClass().getSimpleName();
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

        Log.d("waiting for model to become ready ...");
        model.waitUntilReady();
        Log.d("model ready");

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
            model.validateInput(input);
        }
        catch (IOException e)
        {
            throw new SSJFatalException("model validation failed", e);
        }

        if(model.getInputDim() != null)
        {
            selector = new Selector();
            selector.options.values.set(model.getInputDim());
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
        if(trigger == null || trigger.type != Cons.Type.STRING)
            throw new SSJFatalException("Event trigger missing or invalid. Make sure Trainer is setup to receive string events.");

        Stream[] input = stream_in;

        if(options.merge.get()) {
            merge.transform(input, stream_merged[0]);
            input = stream_merged;
        }
        if(selector != null) {
            selector.transform(input, stream_selected[0]);
            input = stream_selected;
        }

        model.train(input[0], trigger.ptrStr());
    }

    @Override
    public void setModel(Model model)
    {
        this.model = model;
    }

    @Override
    public Model getModel()
    {
        return model;
    }
}
