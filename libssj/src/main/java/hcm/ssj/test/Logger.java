/*
 * Logger.java
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

package hcm.ssj.test;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class Logger extends Consumer
{
    public class Options extends OptionList
    {
        public final Option<Boolean> reduceNum = new Option<>("reduceNum", true, Boolean.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
    public final Options options = new Options();

    public Logger() {
        _name = "Logger";
    }

    @Override
	public void enter(Stream[] stream_in) throws SSJFatalException
	{
    }

    protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        String msg;
        for (int k = 0; k < stream_in.length; ++k)
        {
            int num = (options.reduceNum.get()) ? 1 : stream_in[k].num;
            for (int i = 0; i < num; ++i)
            {
                msg = "";
                for (int j = 0; j < stream_in[k].dim; ++j)
                {
                    switch (stream_in[k].type)
                    {
                        case BYTE:
                            msg += stream_in[k].ptrB()[i * stream_in[k].dim + j] + " ";
                            break;
                        case CHAR:
                            msg += stream_in[k].ptrC()[i * stream_in[k].dim + j] + " ";
                            break;
                        case SHORT:
                            msg += stream_in[k].ptrS()[i * stream_in[k].dim + j] + " ";
                            break;
                        case INT:
                            msg += stream_in[k].ptrI()[i * stream_in[k].dim + j] + " ";
                            break;
                        case LONG:
                            msg += stream_in[k].ptrL()[i * stream_in[k].dim + j] + " ";
                            break;
                        case FLOAT:
                            msg += stream_in[k].ptrF()[i * stream_in[k].dim + j] + " ";
                            break;
                        case DOUBLE:
                            msg += stream_in[k].ptrD()[i * stream_in[k].dim + j] + " ";
                            break;
                        case BOOL:
                            msg += stream_in[k].ptrBool()[i * stream_in[k].dim + j] + " ";
                            break;
                    }
                }
                Log.i(msg);
            }
        }
    }

    public void flush(Stream[] stream_in) throws SSJFatalException
    {
    }
}
