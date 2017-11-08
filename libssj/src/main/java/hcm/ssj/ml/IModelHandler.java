/*
 * IFileWriter.java
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

import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.file.FileCons;

/**
 * Interface for objects which work with models
 */
public interface IModelHandler
{

	/**
     * Standard options
     */
    class Options extends OptionList
    {
        public final Option<String> trainerPath = new Option<>("trainerPath", FileCons.SSJ_EXTERNAL_STORAGE, String.class, "path where trainer is located");
        public final Option<String> trainerFile = new Option<>("trainerFile", null, String.class, "trainer file name");
        public final Option<IModelHandler> modelSource = new Option<>("modelSource", null, IModelHandler.class, "use the model of another component");

        /**
         *
         */
        protected Options()
        {
            addOptions();
        }
    }

    boolean hasReferableModel();
    ModelDescriptor getModelDescriptor();
}
