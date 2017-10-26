/*
 * ClassifierT.java
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
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
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
public class ClassifierT extends Transformer
{

    /**
     * All options for the transformer
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
    private Model _model = null;

    private ModelDescriptor modelInfo = null;

    public ClassifierT()
    {
        _name = this.getClass().getSimpleName();
    }


    public void load(File file) throws XmlPullParserException, IOException
    {
        prepareModel(file);
        loadModel();
    }

    /**
     * Load trainer file
     */
    private void prepareModel(File file) throws XmlPullParserException, IOException
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
        }
        else
        {
            _model = options.model.get();
        }
    }

    private void loadModel() throws IOException
    {
        _model.load(FileUtils.getFile(options.trainerPath.get(), modelInfo.modelFileName));
        _model.loadOption(FileUtils.getFile(options.trainerPath.get(), modelInfo.modelOptionFileName));
    }

    @Override
    public void init(double frame, double delta) throws SSJException
    {
        try {
            prepareModel(FileUtils.getFile(options.trainerPath.get(), options.trainerFile.get()));
        } catch (IOException | XmlPullParserException e) {
            throw new SSJException("unable to load trainer file", e);
        }
    }

    /**
	 * @param stream_in  Stream[]
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        //load model files
        try {
            loadModel();
        } catch (IOException e) {
            throw new SSJFatalException("unable to load model file", e);
        }

        if(_model == null || !_model.isTrained())
        {
            throw new SSJFatalException("model not loaded");
        }

		if (stream_out.dim != _model.getNumClasses())
		{
            throw new SSJFatalException("stream out does not match model: " + stream_out.dim + " != " + _model.getNumClasses());
		}

		if (stream_in.length > 1 && !options.merge.get())
		{
            throw new SSJFatalException("sources count not supported. Did you forget to set merge to true?");
		}

		if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
		{
            throw new SSJFatalException("stream type not supported");
		}

        Stream[] input = stream_in;
        if(input[0].bytes != modelInfo.bytes || input[0].type != modelInfo.type) {
            throw new SSJFatalException("input stream (type=" + input[0].type + ", bytes=" + input[0].bytes
                          + ") does not match model's expected input (type=" + modelInfo.type + ", bytes=" + modelInfo.bytes + ", sr=" + modelInfo.sr + ")");
        }
        if(input[0].sr != modelInfo.sr) {
            Log.w("input stream (sr=" + input[0].sr + ") may not be correct for model (sr=" + modelInfo.sr + ")");
        }

        if(options.merge.get() && stream_in.length > 1)
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
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        Stream[] input = stream_in;

        if(options.merge.get() && stream_in.length > 1) {
            _merge.transform(input, _stream_merged[0]);
            input = _stream_merged;
        }
        if(_selector != null) {
            _selector.transform(input, _stream_selected[0]);
            input = _stream_selected;
        }

        float[] probs = _model.forward(input);
        if (probs != null)
        {
            float[] out = stream_out.ptrF();
            for (int i = 0; i < probs.length; i++)
            {
                out[i] = probs[i];
            }
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        if(_model == null)
        {
            Log.e("model header not loaded, cannot determine num classes.");
            return 0;
        }

        return _model.getNumClasses();
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return 1;
    }

    public Model getModel()
    {
        return _model;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];

        if(_model != null)
        {
            //define output stream
            if (_model.getClassNames() != null)
                System.arraycopy(_model.getClassNames(), 0, stream_out.desc, 0, stream_out.desc.length);
        }
        else
        {
            for (int i = 0; i < overallDimension; i++)
            {
                stream_out.desc[i] = "class" + i;
            }
        }
    }
}
