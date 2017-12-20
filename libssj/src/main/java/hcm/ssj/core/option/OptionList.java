/*
 * OptionList.java
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

package hcm.ssj.core.option;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;

/**
 * Derivable option list for SSJ.<br>
 * Created by Frank Gaibler on 04.03.2016.
 */
public abstract class OptionList
{
    protected LinkedHashSet<Option> hashSetOptions = new LinkedHashSet<>();

    /**
     *
     */
    protected OptionList()
    {
    }

    /**
     * @return Option[]
     */
    public final Option[] getOptions()
    {
        return hashSetOptions.toArray(new Option[hashSetOptions.size()]);
    }

    /**
     * @param name  String
     * @param value Object
     * @return boolean
     */
    public final boolean setOptionValue(String name, String value)
    {
        for (Option option : hashSetOptions)
        {
            if (option.getName().equals(name))
            {
                option.setValue(value);
                return true;
            }
        }
        return false;
    }

    /**
     * @param name String
     * @return Object
     */
    public final Object getOptionValue(String name)
    {
        for (Option option : hashSetOptions)
        {
            if (option.getName().equals(name))
            {
                return option.get();
            }
        }
        return null;
    }

    /**
     *
     */
    protected final void addOptions()
    {
        //only add on latest subclass
        if (super.getClass().isAssignableFrom(this.getClass()))
        {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields)
            {
                if (field.getType().isAssignableFrom(Option.class))
                {
                    try
                    {
                        Option option = (Option) field.get(this);
                        //only add instantiated options
                        if (option != null)
                        {
                            add(option);
                        }
                    } catch (IllegalAccessException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param option Option
     */
    public void add(Option option)
    {
        hashSetOptions.add(option);
    }
}
