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

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileUtils;
import hcm.ssj.file.LoggingConstants;
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
        public final Option<String> trainerPath = new Option<>("trainerPath", LoggingConstants.SSJ_EXTERNAL_STORAGE, String.class, "path where trainer is located");
        public final Option<String> trainerFile = new Option<>("trainerFile", null, String.class, "trainer file name");
        public final Option<Integer> numClasses = new Option<>("numClasses", 0, Integer.class, "number of classification classes");
        public final Option<Boolean> merge = new Option<>("merge", true, Boolean.class, "merge input streams");

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
    private Cons.Type type = Cons.Type.UNDEF;
    private ArrayList<String> classNames = new ArrayList<>();

    private int bytes = 0;
    private int dim = 0;
    private float sr = 0;


    public ClassifierT()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * Load data from option file
     */
    public void load(File file) throws XmlPullParserException, IOException
    {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new FileReader(file));

        parser.next();
        if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("trainer"))
        {
            Log.w("unknown or malformed trainer file");
            return;
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            //STREAM
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("streams"))
            {
                parser.nextTag(); //item
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
                {
                    bytes = Integer.valueOf(parser.getAttributeValue(null, "byte"));
                    dim = Integer.valueOf(parser.getAttributeValue(null, "dim"));
                    sr = Float.valueOf(parser.getAttributeValue(null, "sr"));
                    type = Cons.Type.valueOf(parser.getAttributeValue(null, "type"));
                }
            }

            // CLASS
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("classes"))
            {
                parser.nextTag();

                while (parser.getName().equalsIgnoreCase("item"))
                {
                    if (parser.getEventType() == XmlPullParser.START_TAG)
                    {
                        classNames.add(parser.getAttributeValue(null, "name"));
                    }
                    parser.nextTag();
                }
            }

            //SELECT
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("select"))
            {
                parser.nextTag(); //item
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
                {
                    int stream_id = Integer.valueOf(parser.getAttributeValue(null, "stream"));
                    if (stream_id != 0)
                        Log.w("multiple input streams not supported");
                    String[] select = parser.getAttributeValue(null, "select").split(" ");
                    int[] dims = new int[select.length];
                    for (int i = 0; i < select.length; i++) {
                        dims[i] = Integer.valueOf(select[i]);
                    }

                    _selector = new Selector();
                    _selector.options.values.set(dims);
                }
            }

            //MODEL
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("model"))
            {
                String modelName = parser.getAttributeValue(null, "create");
                _model = Model.create(modelName);

                if (modelName.equalsIgnoreCase("PythonModel"))
                {
                    ((TensorFlow) _model).setNumClasses(classNames.size());
                    ((TensorFlow) _model).setClassNames(classNames.toArray(new String[0]));
                }

                _model.load(FileUtils.getFile(options.trainerPath.get(), parser.getAttributeValue(null, "path") + ".model"));
                _model.loadOption(FileUtils.getFile(options.trainerPath.get(), parser.getAttributeValue(null, "option") + ".option"));
            }

            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
                break;
        }
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        //load model files
        try {
            load(FileUtils.getFile(options.trainerPath.get(), options.trainerFile.get()));
        } catch (IOException | XmlPullParserException e) {
            Log.e("unable to load trainer file", e);
        }

        if(_model == null || !_model.isTrained())
            Log.e("model not loaded");

        if(stream_out.dim != _model.getNumClasses())
            Log.e("specified num classes does not match model: " + stream_out.dim + " != " + _model.getNumClasses());

        //define output stream
        if (_model.getClassNames() != null)
            System.arraycopy(_model.getClassNames(), 0, stream_out.desc, 0, stream_out.desc.length);

        if (stream_in.length > 1 && !options.merge.get())
            Log.e("sources count not supported");

        if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
            Log.e("stream type not supported");

        Stream[] input = stream_in;
        if(input[0].bytes != bytes || input[0].type != type) {
            Log.e("input stream (type=" + input[0].type + ", bytes=" + input[0].bytes
                          + ") does not match model's expected input (type=" + type + ", bytes=" + bytes + ", sr=" + sr + ")");
            return;
        }
        if(input[0].sr != sr) {
            Log.w("input stream (sr=" + input[0].sr + ") may not be correct for model (sr=" + sr + ")");
        }


        if(options.merge.get())
        {
            _merge = new Merge();
            _stream_merged = new Stream[1];
            _stream_merged[0] = Stream.create(input[0].num, _merge.getSampleDimension(input), input[0].sr, input[0].type);
            _merge.enter(stream_in, _stream_merged[0]);
            input = _stream_merged;
        }

        if(input[0].dim != dim) {
            Log.e("input stream (dim=" + input[0].dim + ") does not match model (dim=" + dim + ")");
            return;
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
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        Stream[] input = stream_in;

        if(options.merge.get()) {
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
        if(options.numClasses.get() == 0)
            Log.e("Number of classes not defined! It must be set in the options.");

        return options.numClasses.get();
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

        for (int i = 0; i < overallDimension; i++)
        {
            stream_out.desc[i] = "class" + i;
        }
    }
}
