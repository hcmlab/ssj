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

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.Selector;

/**
 * Generic classifier
 */
public class Trainer extends Consumer
{
    /**
     * All options for the consumer
     */
    public class Options extends OptionList
    {
        public final Option<String> trainerPath = new Option<>("trainerPath", FileCons.SSJ_EXTERNAL_STORAGE, String.class, "path where trainer is located");
        public final Option<String> trainerFile = new Option<>("trainerFile", null, String.class, "trainer file name");
        public final Option<Boolean> merge = new Option<>("merge", true, Boolean.class, "merge input streams");
        public final Option<Model> model = new Option<>("model", null, Model.class, "model to use (use null to load from file)");

        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    private Selector _selector = null;
    private Stream[] _stream_merged;
    private Stream[] _stream_selected;
    private Merge _merge = null;
    private Model _model;
    private ModelDescriptor modelInfo = null;

    public Trainer()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * Load model from file
     */
    public void load(File file) throws XmlPullParserException, IOException
    {
        modelInfo = new ModelDescriptor();
        modelInfo.parseTrainerFile(file);

        if(modelInfo.select_dimensions != null)
        {
            _selector = new Selector();
            _selector.options.values.set(modelInfo.select_dimensions);
        }

        if(options.model.get() == null)
        {
            _model = Model.create(modelInfo.modelName);
            _model.setNumClasses(modelInfo.classNames.size());
            _model.setClassNames(modelInfo.classNames.toArray(new String[0]));

            _model.load(FileUtils.getFile(options.trainerPath.get(), modelInfo.modelFileName));
            _model.loadOption(FileUtils.getFile(options.trainerPath.get(), modelInfo.modelOptionFileName));
        }
        else
        {
            _model = options.model.get();
        }
    }

    /**
	 * @param stream_in  Stream[]
	 */
    @Override
    public void enter(Stream[] stream_in) throws SSJFatalException
    {
        try
        {
            File trainerFile = FileUtils.getFile(options.trainerPath.get(), options.trainerFile.get());
            load(trainerFile);
        }
        catch (XmlPullParserException | IOException e)
        {
            throw new SSJFatalException("unable to load model", e);
        }

        if (stream_in.length > 1 && !options.merge.get())
        {
            throw new SSJFatalException("sources count not supported");
        }

        if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
        {
            throw new SSJFatalException("stream type not supported");
        }

        if(_model == null || !_model.isTrained())
        {
            throw new SSJFatalException("model not loaded");
        }

        Stream[] input = stream_in;
        if(input[0].bytes != modelInfo.bytes || input[0].type != modelInfo.type) {
            throw new SSJFatalException("input stream (type=" + input[0].type + ", bytes=" + input[0].bytes
                          + ") does not match model's expected input (type=" + modelInfo.type + ", bytes=" + modelInfo.bytes + ", sr=" + modelInfo.sr + ")");
        }
        if(input[0].sr != modelInfo.sr) {
            Log.w("input stream (sr=" + input[0].sr + ") may not be correct for model (sr=" + modelInfo.sr + ")");
        }

        if(options.merge.get())
        {
            _merge = new Merge();
            _stream_merged = new Stream[1];
            _stream_merged[0] = Stream.create(input[0].num, _merge.getSampleDimension(input), input[0].sr, input[0].type);
            _merge.enter(stream_in, _stream_merged[0]);
            input = _stream_merged;
        }

        if(input[0].dim != modelInfo.dim) {
            throw new SSJFatalException("input stream (dim=" + input[0].dim + ") does not match model (dim=" + modelInfo.dim + ")");
        }
        if (input[0].num > 1) {
            Log.w ("stream num > 1, only first sample is used");
        }

        if(_selector != null)
        {
            _stream_selected = new Stream[1];
            _stream_selected[0] = Stream.create(input[0].num, _selector.options.values.get().length, input[0].sr, input[0].type);
            _selector.enter(input, _stream_selected[0]);
        }
    }

    /**
     * @param stream_in  Stream[]
	 * @param trigger
     */
    @Override
    public void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        if(trigger == null)
            throw new SSJFatalException("Event trigger missing. Make sure Trainer is setup to receive events.");

        Stream[] input = stream_in;

        if(options.merge.get()) {
            _merge.transform(input, _stream_merged[0]);
            input = _stream_merged;
        }
        if(_selector != null) {
            _selector.transform(input, _stream_selected[0]);
            input = _stream_selected;
        }

        _model.train(input, trigger.name);
    }

    public Model getModel()
    {
        return _model;
    }
}
