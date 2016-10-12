/*
 * NaiveBayes.java
 * Copyright (c) 2016
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.SimpleXmlParser;

/**
 * Evaluates live data depending on the naive bayes classifier files of SSI.<br>
 * Created by Frank Gaibler on 22.09.2015.
 */
public class NaiveBayes extends Model
{
    /**
     * All options for the transformer
     */
    public class Options extends OptionList
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
    private int n_classes;
    private int n_features;
    private String[] class_names = null;
    private double[] class_probs = null;
    private double[][] means = null;
    private double[][] std_dev = null;

    /**
     *
     */
    public NaiveBayes()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
     * @param stream Stream
     * @return double[]
     */
    protected float[] forward(Stream stream)
    {
        if (!_isTrained || class_probs == null || class_probs.length <= 0)
        {
            Log.w("not trained");
            return null;
        }
        if (stream.dim != n_features)
        {
            Log.w("feature dimension differs");
            return null;
        }
        float[] probs = new float[n_classes];
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

    @Override
    void train(Stream stream) {
    }

    /**
     * Load data from option file
     */
    protected void loadOption(File file)
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
            Log.e("file could not be parsed");
        }
    }

    @Override
    void save(File file) {
    }

    @Override
    int getNumClasses() {
        return n_classes;
    }

    @Override
    String[] getClassNames() {
        return class_names;
    }

    /**
     * Load data from model file
     */
    protected void load(File file)
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
            n_classes = Integer.valueOf(token[0]);
        } else
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
        class_names = new String[n_classes];
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
            class_names[nclass] = token[0];
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

        _isTrained = true;
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
        switch (stream.type)
        {
            case CHAR:
                char[] chars = stream.ptrC();
                double[] c = new double[stream.num * stream.dim];
                for (int i = 0; i < c.length; i++)
                {
                    c[i] = (double) chars[i];
                }
                return c;
            case SHORT:
                short[] shorts = stream.ptrS();
                double[] s = new double[stream.num * stream.dim];
                for (int i = 0; i < s.length; i++)
                {
                    s[i] = (double) shorts[i];
                }
                return s;
            case INT:
                int[] ints = stream.ptrI();
                double[] in = new double[stream.num * stream.dim];
                for (int i = 0; i < in.length; i++)
                {
                    in[i] = (double) ints[i];
                }
                return in;
            case LONG:
                long[] longs = stream.ptrL();
                double[] l = new double[stream.num * stream.dim];
                for (int i = 0; i < l.length; i++)
                {
                    l[i] = (double) longs[i];
                }
                return l;
            case FLOAT:
                float[] floats = stream.ptrF();
                double[] f = new double[stream.num * stream.dim];
                for (int i = 0; i < f.length; i++)
                {
                    f[i] = (double) floats[i];
                }
                return f;
            case DOUBLE:
                return stream.ptrD();
            default:
                Log.e("invalid input stream type");
                return new double[stream.num * stream.dim];
        }
    }
}
