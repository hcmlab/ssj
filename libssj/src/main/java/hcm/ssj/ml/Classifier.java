/*
 * Classifier.java
 * Copyright (c) 2017
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

package hcm.ssj.ml;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.LoggingConstants;
import hcm.ssj.signal.Merge;
import hcm.ssj.signal.Selector;

/**
 * Generic classifier
 */
public class Classifier extends Consumer
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        public final Option<String> trainerPath = new Option<>("trainerPath", LoggingConstants.SSJ_EXTERNAL_STORAGE, String.class, "path where trainer is located");
        public final Option<String> trainerFile = new Option<>("trainerFile", null, String.class, "trainer file name");
        public final Option<Boolean> merge = new Option<>("merge", true, Boolean.class, "merge input streams");
        public final Option<Boolean> log = new Option<>("log", true, Boolean.class, "print results in log");
        public final Option<Boolean> showLabel = new Option<>("showLabel", false, Boolean.class, "prints a single label in log");
        public final Option<String> sender = new Option<>("sender", "Classifier", String.class, "event sender name, written in every event");
        public final Option<String> event = new Option<>("event", "Result", String.class, "event name");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    private String[] class_names = null;

    private Merge _merge = null;
    private Stream[] _stream_merged;
    private Selector _selector = null;
    private Stream[] _stream_selected;
    private Model _model;

    private int classNum = 0;
    private ArrayList<String> classNames = new ArrayList<String>();

    private int bytes = 0;
    private int dim = 0;
    private float sr = 0;
    private Cons.Type type = Cons.Type.UNDEF;

    /**
     *
     */
    public Classifier()
    {
        _name = this.getClass().getSimpleName();
    }

    @Override
    public void init(Stream stream_in[]) throws SSJException
    {
        try {
            load(getFile(options.trainerPath.get(), options.trainerFile.get()));
        } catch (XmlPullParserException | IOException e) {
            throw new SSJException(e);
        }
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

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            //STREAM
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("streams")) {

                parser.nextTag(); //item
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item")) {

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
                        classNum++;
                        classNames.add(parser.getAttributeValue(null, "name"));
                    }
                    parser.nextTag();
                }
            }

            //SELECT
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("select")) {

                parser.nextTag(); //item
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item")) {

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
                    ((TensorFlow) _model).setNumClasses(classNum);
                    ((TensorFlow) _model).setClassNames(classNames.toArray(new String[0]));
                }

                _model.load(getFile(options.trainerPath.get(), parser.getAttributeValue(null, "path") + ".model"));
                _model.loadOption(getFile(options.trainerPath.get(), parser.getAttributeValue(null, "option") + ".option"));
            }

            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
                break;
        }
    }

    /**
     * @param stream_in  Stream[]
     */
    @Override
    public void enter(Stream[] stream_in)
    {
        if (stream_in.length > 1 && !options.merge.get())
        {
            Log.e("sources count not supported");
        }

        if (stream_in[0].type == Cons.Type.EMPTY || stream_in[0].type == Cons.Type.UNDEF)
        {
            Log.e("stream type not supported");
        }

        if(_model == null || !_model.isTrained())
            Log.e("model not loaded");

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

        if(_evchannel_out == null)
            Log.e("no outgoing event channel has been registered");
    }

    /**
     * @param stream_in  Stream[]
     */
    @Override
    public void consume(Stream[] stream_in)
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

        if (options.showLabel.get())
        {
            String[] class_names = _model.getClassNames();
            int bestLabelIdx = TensorFlow.maxIndex(probs);
            String bestMatch = String.format("BEST MATCH: %s (%.2f%% likely)",
                          class_names[bestLabelIdx], probs[bestLabelIdx] * 100f);
            Log.i(bestMatch);
        }

        if(options.log.get() && !options.showLabel.get())
        {
            String[] class_names = _model.getClassNames();
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

        if(_evchannel_out != null)
        {
            Event ev = Event.create(Cons.Type.FLOAT);
            ev.sender = options.sender.get();
            ev.name = options.event.get();
            ev.time = (int)(1000 * stream_in[0].time + 0.5);
            double duration = stream_in[0].num / stream_in[0].sr;
            ev.dur = (int)(1000 * duration + 0.5);
            ev.state = Event.State.COMPLETED;
            //ev.setData(probs);

            _evchannel_out.pushEvent(ev);
        }
    }

    /**
     * @param filePath Option
     * @param fileName Option
     * @return File
     */
    protected final File getFile(String filePath, String fileName)
    {
        if (filePath == null)
        {
            Log.w("file path not set, setting to default " + LoggingConstants.SSJ_EXTERNAL_STORAGE);
            filePath = LoggingConstants.SSJ_EXTERNAL_STORAGE;
        }
        File fileDirectory = new File(filePath);
        if (fileName == null)
        {
            Log.e("file name not set");
            return null;
        }
        return new File(fileDirectory, fileName);
    }

    public Model getModel()
    {
        return _model;
    }
}
