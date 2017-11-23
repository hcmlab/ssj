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

import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileUtils;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.Selector;

/**
 * Generic classifier
 */
public class ClassifierT extends Transformer implements IModelHandler
{

    /**
     * All options for the transformer
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

    private ModelDescriptor modelDescriptor = null;

    public ClassifierT()
    {
        _name = this.getClass().getSimpleName();
    }

    @Override
    public void init(double frame, double delta) throws SSJException
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
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
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

        if(modelDescriptor.getSelectDimensions() != null)
        {
            selector = new Selector();
            selector.options.values.set(modelDescriptor.getSelectDimensions());
            stream_selected = new Stream[1];
            stream_selected[0] = Stream.create(input[0].num, selector.options.values.get().length, input[0].sr, input[0].type);
            selector.enter(input, stream_selected[0]);
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
            merge.transform(input, stream_merged[0]);
            input = stream_merged;
        }
        if(selector != null) {
            selector.transform(input, stream_selected[0]);
            input = stream_selected;
        }

        float[] probs = modelDescriptor.getModel().forward(input[0]);
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
        if(modelDescriptor == null)
        {
            Log.e("model header not loaded, cannot determine num classes.");
            return 0;
        }

        return modelDescriptor.getNumClasses();
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

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];

        if(modelDescriptor != null)
        {
            //define output stream
            if (modelDescriptor.getClassNames() != null)
                System.arraycopy(modelDescriptor.getClassNames(), 0, stream_out.desc, 0, stream_out.desc.length);
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
