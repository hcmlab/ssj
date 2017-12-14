/*
 * NaiveBayes.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import hcm.ssj.core.Log;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.SimpleXmlParser;

/**
 * Evaluates live data depending on the naive bayes classifier files of SSI.<br>
 * Created by Frank Gaibler on 22.09.2015.
 */
@Deprecated
public class NaiveBayesOld extends Model
{
    /**
     * All options for the transformer
     */
    public class Options extends Model.Options
    {
        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    //file options
    private boolean userLogNormalDistribution = true;
    private boolean usePriorProbability = false;
    //helper variables
    private int n_features;
    private double[] class_probs = null;
    private double[][] means = null;
    private double[][] std_dev = null;
    private float[] probs;
    private double[] data;

    /**
     *
     */
    public NaiveBayesOld()
    {
        _name = "NaiveBayes";
    }

    @Override
    protected void init(String[] classes, int n_features)
    {
        probs = new float[n_classes];
        _isSetup = true;
    }

    @Override
    public Model.Options getOptions()
    {
        return options;
    }

    /**
     * @param stream Stream
     * @return double[]
     */
    public float[] forward(Stream stream)
    {
        if (!isTrained || class_probs == null || class_probs.length <= 0)
        {
            Log.w("not trained");
            return null;
        }
        if (stream.dim != n_features)
        {
            Log.w("feature dimension differs");
            return null;
        }

        double sum = 0;
        boolean prior = usePriorProbability;
        if (userLogNormalDistribution)
        {
            for (int nclass = 0; nclass < n_classes; nclass++)
            {
                double prob = prior ? naiveBayesLog(class_probs[nclass]) : 0;
                double[] ptr = getValuesAsDouble(stream);
                double temp;
                for (int nfeat = 0, t = 0; nfeat < n_features; nfeat++)
                {
                    if (std_dev[nclass][nfeat] == 0)
                    {
                        t++;
                        continue;
                    }
                    double sqr = std_dev[nclass][nfeat] * std_dev[nclass][nfeat];
                    temp = ptr[t++] - means[nclass][nfeat];
                    if (sqr != 0)
                    {
                        prob += -naiveBayesLog(std_dev[nclass][nfeat]) - (temp * temp) / (2 * sqr);
                    } else
                    {
                        prob += -naiveBayesLog(std_dev[nclass][nfeat]) - (temp * temp) / (2 * Double.MIN_VALUE);
                    }
                }
                probs[nclass] = (float)Math.exp(prob / n_features);
                sum += probs[nclass];
            }

        } else
        {
            for (int nclass = 0; nclass < n_classes; nclass++)
            {
                double norm_const = Math.sqrt(2.0 * Math.PI);
                double prob = prior ? class_probs[nclass] : 0;
                double[] ptr = getValuesAsDouble(stream);
                for (int nfeat = 0, t = 0; nfeat < n_features; nfeat++)
                {
                    double diff = ptr[t++] - means[nclass][nfeat];
                    double stddev = std_dev[nclass][nfeat];
                    if (stddev == 0)
                    {
                        stddev = Double.MIN_VALUE;
                    }
                    double temp = (1 / (norm_const * stddev)) * Math.exp(-((diff * diff) / (2 * (stddev * stddev))));
                    prob *= temp;
                }
                probs[nclass] = (float)prob;
                sum += probs[nclass];
            }
        }
        //normalisation
        if (sum == 0)
        {
            Log.w("sum == 0");
            for (int j = 0; j < n_classes; j++)
            {
                probs[j] = 1.0f / n_classes;
            }
        } else
        {
            for (int j = 0; j < n_classes; j++)
            {
                probs[j] /= sum;
            }
        }
        return probs;
    }

    /**
     * Load data from option file
     */
    public void loadOption(File file)
    {
        if (file == null)
        {
            Log.w("option file not set in options");
            return;
        }
        SimpleXmlParser simpleXmlParser = new SimpleXmlParser();
        try
        {
            SimpleXmlParser.XmlValues xmlValues = simpleXmlParser.parse(
                    new FileInputStream(file),
                    new String[]{"options", "item"},
                    new String[]{"name", "value"}
            );
            ArrayList<String[]> foundAttributes = xmlValues.foundAttributes;
            for (String[] strings : foundAttributes)
            {
                if (strings[0].equals("log"))
                {
                    userLogNormalDistribution = strings[1].equals("true");
                } else if (strings[0].equals("prior"))
                {
                    usePriorProbability = strings[1].equals("true");
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.e("file could not be parsed", e);
        }
    }

    /**
     * Load data from model file
     */
    public void loadModel(File file)
    {
        if (file == null)
        {
            Log.e("model file not set in options");
            return;
        }
        BufferedReader reader;
        try
        {
            InputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            reader = new BufferedReader(inputStreamReader);
        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
            return;
        }
        String line;
        do
        {
            line = readLine(reader);
        } while (line.startsWith("#"));
        String token[] = line.split("\t");
        if (token.length > 0)
        {
            int classNum = Integer.valueOf(token[0]);
            if(classNum != n_classes)
                Log.w("model definition (n_classes) mismatch between trainer and model file: " + classNum +" != "+ n_classes);
        }
        else
        {
            Log.w("can't read number of classes from classifier file " + file.getName() + "!");
            return;
        }
        if (token.length > 1)
        {
            n_features = Integer.valueOf(token[1]);
        } else
        {
            Log.w("can't read feature dimension from classifier file " + file.getName() + "'!");
            return;
        }
        class_probs = new double[n_classes];
        means = new double[n_classes][];
        std_dev = new double[n_classes][];
        for (int nclass = 0; nclass < n_classes; nclass++)
        {
            means[nclass] = new double[n_features];
            std_dev[nclass] = new double[n_features];
            for (int nfeat = 0; nfeat < n_features; nfeat++)
            {
                means[nclass][nfeat] = 0;
                std_dev[nclass][nfeat] = 0;
            }
            class_probs[nclass] = 0;
        }
        for (int nclass = 0; nclass < n_classes; nclass++)
        {
            do
            {
                line = readLine(reader);
            } while (line.isEmpty() || line.startsWith("#"));
            token = line.split("\t");

            String name = token[0];
            if(!name.equalsIgnoreCase(class_names[nclass]))
            {
                Log.w("model definition (name of class " + nclass + ") mismatch between trainer and model file:" + name + " != " + class_names[nclass]);
                class_names[nclass] = name;
            }

            class_probs[nclass] = Double.valueOf(token[1]);
            for (int nfeat = 0; nfeat < n_features; nfeat++)
            {
                line = readLine(reader);
                token = line.split("\t");
                means[nclass][nfeat] = Double.valueOf(token[0]);
                std_dev[nclass][nfeat] = Double.valueOf(token[1]);
            }
        }
        try
        {
            reader.close();
        } catch (IOException e)
        {
            Log.e("could not close reader");
        }

        isTrained = true;
    }

    /**
     * @param reader BufferedReader
     * @return String
     */
    private String readLine(BufferedReader reader)
    {
        String line = null;
        if (reader != null)
        {
            try
            {
                line = reader.readLine();
            } catch (IOException e)
            {
                Log.e("could not read line");
            }
        }
        return line;
    }

    /**
     * @param x double
     * @return double
     */
    private double naiveBayesLog(double x)
    {
        return x > (1e-20) ? Math.log(x) : -46;
    }

    /**
     * Transforms an input array of a stream into a double array and returns it.
     *
     * @param stream Stream
     * @return double[]
     */
    private double[] getValuesAsDouble(Stream stream)
    {
        if(data == null)
            data = new double[stream.num * stream.dim];

        switch (stream.type)
        {
            case CHAR:
                char[] chars = stream.ptrC();
                for (int i = 0; i < data.length; i++)
                {
                    data[i] = (double) chars[i];
                }
                break;
            case SHORT:
                short[] shorts = stream.ptrS();
                for (int i = 0; i < data.length; i++)
                {
                    data[i] = (double) shorts[i];
                }
                break;
            case INT:
                int[] ints = stream.ptrI();
                for (int i = 0; i < data.length; i++)
                {
                    data[i] = (double) ints[i];
                }
                break;
            case LONG:
                long[] longs = stream.ptrL();
                for (int i = 0; i < data.length; i++)
                {
                    data[i] = (double) longs[i];
                }
                break;
            case FLOAT:
                float[] floats = stream.ptrF();
                for (int i = 0; i < data.length; i++)
                {
                    data[i] = (double) floats[i];
                }
                break;
            case DOUBLE:
                return stream.ptrD();
            default:
                Log.e("invalid input stream type");
                break;
        }

        return data;
    }
}
