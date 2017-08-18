/*
 * Model.java
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

import java.io.File;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Generic model for machine learning
 * Created by Ionut Damian on 10.10.2016.
 */
public abstract class Model {

    protected String _name = "Model";
    protected boolean _isTrained = false;

    public class Options extends OptionList {}
    public final Options options = new Options();

    public static Model create(String name)
    {
        if(name.compareToIgnoreCase("NaiveBayes") == 0)
            return new NaiveBayes();
        else if (name.compareToIgnoreCase("SVM") == 0)
            return new SVM();
        else if (name.compareToIgnoreCase("PythonModel") == 0)
            return new TensorFlow();

        Log.e("unknown model");
        return null;
    }


    public String getName()
    {
        return _name;
    }

    /**
     * forward data to the model for classification/inference
     * @param stream Stream
     * @return double[] classification/inference probabilities as outputed by the model
     */
    abstract float[] forward(Stream[] stream);

    /**
     * Train model with data from the stream
     * @param stream Stream
     */
    abstract void train(Stream[] stream);

    /**
     * Load model from file
     */
    abstract void load(File file);

    /**
     * Load model options from file
     */
    abstract void loadOption(File file);

    /**
     * Save model to file
     */
    abstract void save(File file);

    public boolean isTrained() {
        return _isTrained;
    }

    abstract int getNumClasses();
    abstract String[] getClassNames();
}
