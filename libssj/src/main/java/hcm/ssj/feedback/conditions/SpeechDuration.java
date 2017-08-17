/*
 * SpeechDuration.java
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

package hcm.ssj.feedback.conditions;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;

/**
 * Created by Johnny on 01.12.2014.
 */
public class SpeechDuration extends Condition
{

    float _dur = 0;
    boolean _shouldSum= false;

    @Override
    public float parseEvent(Event event)
    {
        if(_shouldSum)
            _dur += event.dur / 1000.0f;
        else
            _dur = event.dur / 1000.0f;

        return _dur;
    }

    public boolean checkEvent(Event event)
    {
        if (event.name.equalsIgnoreCase(_event)
            && event.sender.equalsIgnoreCase(_sender)
            && event.state == Event.State.COMPLETED)
        {
            float value = parseEvent(event);
            if((value == thres_lower) || (value >= thres_lower && value < thres_upper))
                return true;
        }

        return false;
    }

    @Override
    protected void load(XmlPullParser xml, Context context)
    {
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "condition");
        }
        catch (XmlPullParserException | IOException e)
        {
            Log.e("error parsing config file", e);
        }

        _shouldSum = Boolean.getBoolean(xml.getAttributeValue(null, "sum"));
        super.load(xml, context);
    }
}
