/*
 * Model.java
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

package hcm.ssj.ml;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Component;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

/**
 * Generic model for machine learning
 * Created by Ionut Damian on 10.10.2016.
 */
public abstract class Model extends Component
{
    protected int[] select_dimensions = null;

    protected String modelFileName;
    protected String modelOptionFileName;
    protected String dirPath = null;

    protected int input_bytes = 0;
    protected int input_dim = 0;
    protected double input_sr = 0;
    protected Cons.Type input_type = Cons.Type.UNDEF;

    protected boolean isTrained = false;

    protected int output_dim = 0;
    protected String[] output_names = null;

    public class Options extends OptionList
    {
        public final Option<FilePath> file = new Option<>("file", null, FilePath.class, "trainer file containing model information");

        public Options()
        {
            super();
            addOptions();
        }
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("SSJ_" + _name);

        if(!_isSetup)
        {
            // pipe.error(_name, "not initialized", null);
            Log.e("not initialized");
            _safeToKill = true;

            return;
        }

        try
        {
            load();
        }
        catch (IOException e)
        {
            Log.e("error loading model", e);
        }

        synchronized (this)
        {
            this.notifyAll();
        }

        _safeToKill = true;
    }

    public void waitUntilReady()
    {
        if(!isTrained())
		{
			synchronized (this)
			{
				try
				{
					this.wait();
				}
				catch (InterruptedException e)
				{
				    // Do nothing
				}
			}
		}
    }

    public void setup() throws SSJException
    {
        if (_isSetup)
        {
            return;
        }

        // Check if trainer file has been set
        if (getOptions().file.get() != null && getOptions().file.get().value != null && !getOptions().file.get().value.isEmpty())
        {
            String trainerFile = getOptions().file.get().value;
            String fileName = trainerFile.substring(trainerFile.lastIndexOf(File.separator)+1);
            dirPath = trainerFile.substring(0, trainerFile.lastIndexOf(File.separator));

            try
            {
                parseTrainerFile(FileUtils.getFile(dirPath, fileName));
            }
            catch (IOException | XmlPullParserException e)
            {
                throw new SSJException("error parsing trainer file", e);
            }

            init(input_dim, output_dim, output_names);

            _isSetup = true;
        }
        else
        {
            Log.w("option file not defined");
        }
    }

    public void setup(String[] classNames, int bytes_input, int dim_input, double sr_input, Cons.Type type_input)
    {
        this.input_bytes = bytes_input;
        this.input_dim = dim_input;
        this.input_sr = sr_input;
        this.input_type = type_input;

        this.output_names = classNames;
        this.output_dim = classNames.length;

        init(input_dim, this.output_dim, classNames);
        _isSetup = true;
    }

    public String getName()
    {
        return _name;
    }

    public void load() throws IOException
    {
        loadModel(FileUtils.getFile(dirPath, modelFileName + "." + FileCons.FILE_EXTENSION_MODEL));

        if (modelOptionFileName != null && !modelOptionFileName.isEmpty())
        {
            loadOption(FileUtils.getFile(dirPath, modelOptionFileName + "." + FileCons.FILE_EXTENSION_OPTION));
        }

        Log.d("model loaded (file: " + modelFileName + ")");
    }

    public void validateInput(Stream[] input) throws IOException
    {
        if (!isTrained())
        {
            throw new IOException("model not loaded or trained");
        }

        if (input[0].bytes != input_bytes || input[0].type != input_type)
        {
            throw new IOException("input stream (type=" + input[0].type + ", bytes=" + input[0].bytes
                                          + ") does not match model's expected input (type=" + input_type + ", bytes=" + input_bytes + ", sr=" + input_sr + ")");
        }
        if (input[0].sr != input_sr)
        {
            Log.w("input stream (sr=" + input[0].sr + ") may not be correct for model (sr=" + input_sr + ")");
        }

        if (input[0].dim != input_dim)
        {
            throw new IOException("input stream (dim=" + input[0].dim + ") does not match model (dim=" + input_dim + ")");
        }

        if (input[0].num > 1)
        {
            Log.w ("stream num > 1, only first sample is used");
        }
    }

    private void parseTrainerFile(File file) throws XmlPullParserException, IOException, SSJException
    {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new FileReader(file));

        // <trainer> tag
        parser.next();
        if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("trainer"))
        {
            Log.w("unknown or malformed trainer file");
            return;
        }

        ArrayList<String> classNamesList = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            // <streams> tag
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("streams"))
            {
                // <item> tag
                parser.nextTag();

                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
                {
                    input_bytes = Integer.valueOf(parser.getAttributeValue(null, "byte"));
                    input_dim = Integer.valueOf(parser.getAttributeValue(null, "dim"));
                    input_sr = Float.valueOf(parser.getAttributeValue(null, "sr"));
                    input_type = Cons.Type.valueOf(parser.getAttributeValue(null, "type"));
                }
            }

            // <classes> tag
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("classes"))
            {
                // <item> tag
                parser.nextTag();

                while (parser.getName().equalsIgnoreCase("item"))
                {
                    if (parser.getEventType() == XmlPullParser.START_TAG)
                    {
                        classNamesList.add(parser.getAttributeValue(null, "name"));
                    }
                    parser.nextTag();
                }
            }

            // <outputs> tag
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("outputs"))
            {
                // <item> tag
                parser.nextTag();

                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
                {
                    output_dim = Integer.parseInt(parser.getAttributeValue(null, "dim"));
                }
            }

            // <select> tag
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("select"))
            {
                // <item> tag
                parser.nextTag();
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item"))
                {
                    int stream_id = Integer.valueOf(parser.getAttributeValue(null, "stream"));

                    if (stream_id != 0)
                    {
                        Log.w("multiple input streams not supported");
                    }

                    String[] select = parser.getAttributeValue(null, "select").split(" ");
                    select_dimensions = new int[select.length];

                    for (int i = 0; i < select.length; i++)
                    {
                        select_dimensions[i] = Integer.valueOf(select[i]);
                    }
                }
            }

            // <model> tag
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("model"))
            {
                String expectedModel = parser.getAttributeValue(null, "create");

                if(!_name.equals(expectedModel))
                {
                    Log.w("trainer file demands a " + expectedModel + " model, we provide a " + _name + " model.");
                }

                modelFileName = parser.getAttributeValue(null, "path");
                modelOptionFileName =  parser.getAttributeValue(null, "option");

                // Remove model file extension
                if (modelFileName != null && modelFileName.endsWith("." + FileCons.FILE_EXTENSION_MODEL))
                {
                    modelFileName = modelFileName.replaceFirst("(.*)\\." + FileCons.FILE_EXTENSION_MODEL + "$", "$1");
                }

                //  Remove option file extension
                if (modelOptionFileName != null && modelOptionFileName.endsWith("." + FileCons.FILE_EXTENSION_OPTION))
                {
                    modelOptionFileName = modelOptionFileName.replaceFirst("(.*)\\." + FileCons.FILE_EXTENSION_OPTION + "$", "$1");
                }
            }

            // <trainer> end tag
            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
            {
                break;
            }
        }

        if (classNamesList.size() > 0)
        {
            output_names = classNamesList.toArray(new String[0]);
            
            if (output_dim == 0)
            {
                output_dim = output_names.length;
            }
        }
        else
        {
            if (output_dim > 0)
            {
                output_names = new String[output_dim];

                for (int i = 0; i < output_dim; i++)
                {
                    output_names[i] = "out_" + i;
                }
            }
        }
    }

    public void save(String path, String name) throws IOException
    {
        if (!_isSetup)
        {
            return;
        }

        File dir = Util.createDirectory(Util.parseWildcards(path));
        if (dir == null)
        {
            return;
        }

        if (name.endsWith(FileCons.FILE_EXTENSION_TRAINER + FileCons.TAG_DATA_FILE))
        {
            name = name.substring(0, name.length()-2);
        }
        else if (!name.endsWith(FileCons.FILE_EXTENSION_TRAINER))
        {
            name += "." + FileCons.FILE_EXTENSION_TRAINER;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("<trainer ssi-v=\"5\" ssj-v=\"");
        builder.append(Pipeline.getVersion());
        builder.append("\">").append(FileCons.DELIMITER_LINE);

        builder.append("<info trained=\"");
        builder.append(isTrained());
        builder.append("\"/>").append(FileCons.DELIMITER_LINE);

        builder.append("<streams>").append(FileCons.DELIMITER_LINE);
        builder.append("<item byte=\"");
        builder.append(input_bytes);
        builder.append("\" dim=\"");
        builder.append(input_dim);
        builder.append("\" sr=\"");
        builder.append(input_sr);
        builder.append("\" type=\"");
        builder.append(input_type);
        builder.append("\"/>").append(FileCons.DELIMITER_LINE);
        builder.append("</streams>").append(FileCons.DELIMITER_LINE);

        builder.append("<classes>").append(FileCons.DELIMITER_LINE);
        for(String className : output_names)
        {
            builder.append("<item name=\"");
            builder.append(className);
            builder.append("\"/>").append(FileCons.DELIMITER_LINE);
        }
        builder.append("</classes>").append(FileCons.DELIMITER_LINE);

        builder.append("<users>").append(FileCons.DELIMITER_LINE);
        builder.append("<item name=\"userLocal\"/>").append(FileCons.DELIMITER_LINE);
        builder.append("</users>").append(FileCons.DELIMITER_LINE);

        String modelFileName = name + "." + _name;
//        String modelOptionFileName = name + "." + _name;

        builder.append("<model create=\"");
        builder.append(_name);
        builder.append("\" stream=\"0\" path=\"");
        builder.append(modelFileName);
        //builder.append("\" option=\"");
        //builder.append(modelOptionFileName);
        builder.append("\"/>").append(FileCons.DELIMITER_LINE);

        builder.append("</trainer>").append(FileCons.DELIMITER_LINE);

        OutputStream ouputStream = new FileOutputStream(new File(dir, name));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ouputStream));

        writer.write(builder.toString());
        writer.flush();
        writer.close();

        saveModel(new File(dir, modelFileName + "." + FileCons.FILE_EXTENSION_MODEL));
    }

    public abstract Options getOptions();

    /**
     * forward data to the model for classification/inference
     * @param stream Stream
     * @return double[] classification/inference probabilities as outputed by the model
     */
    abstract float[] forward(Stream stream);

    /**
     * Train model with one sample (incremental training)
     * @param stream data of the sample to use for training
     * @param label the label of the data, should match one of the model's classes
     */
    void train(Stream stream, String label)
    {
        Log.e(_name + " does not supported training");
    }

    /**
     * Train model with multiple samples (batch training)
     * @param stream data from where to extract the samples
     * @param anno annotation
     */
    public void train(Stream stream, Annotation anno)
    {
        if(!_isSetup)
        {
            Log.e("model not initialized");
            return;
        }

        for(Annotation.Entry e : anno.getEntries())
        {
            train(stream.substream(e.from, e.to), e.classlabel);
        }

        isTrained = true;
    }

    /**
     * Load model from file
     *
     * @param file Model file
     */
    abstract void loadModel(File file);

    /**
     * Load model options from file
     *
     * @param file Option file
     */
    abstract void loadOption(File file);

    /**
     * Save model to file
     *
     * @param file Model file
     */
    void saveModel(File file)
    {
        Log.e("save not supported");
    }

    /**
     * Save model options to file
     */
    void saveOption(File file) {};

    /**
     * Initialize model variables, called after model parameters have been set but before actually
     * loading the model
     *
     * @param input_dim number of model inputs
     * @param output_dim number of model outputs
     * @param outputNames (optional) output names
     */
    abstract void init(int input_dim, int output_dim, String[] outputNames);

    public boolean isTrained()
    {
        return isTrained;
    }

    /**
     * Set label count for the classifier.
     *
     * @param output_dim amount of object classes to recognize.
     */
    public void setOutputDim(int output_dim)
    {
        this.output_dim = output_dim;
    }

    /**
     * Set label strings for the classifier.
     *
     * @param classNames recognized object classes.
     */
    public void setClassNames(String[] classNames)
    {
        this.output_names = classNames;
    }

    public int getOutputDim()
    {
        return output_dim;
    }

    public String[] getClassNames()
    {
        return output_names;
    }

    public int[] getInputDim()
    {
        return select_dimensions;
    }

    public double getInputSr()
    {
        return input_sr;
    }

    public Cons.Type getInputType()
    {
        return input_type;
    }
}
