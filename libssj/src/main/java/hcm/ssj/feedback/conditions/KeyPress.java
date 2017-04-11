/*
 * Behaviour.java
 * Copyright (c) 2015
 * Author: Ionut Damian
 * *****************************************************
 * This file is part of the Logue project developed at the Lab for Human Centered Multimedia
 * of the University of Augsburg.
 *
 * The applications and libraries are free software; you can redistribute them and/or modify them
 * under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * The software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
public class KeyPress extends Condition
{
    protected String _text;
    protected boolean _isToggle;
    protected float _lastValue = 0;

    public boolean checkEvent(Event event)
    {
        if (event.name.equalsIgnoreCase(_event)
        && event.sender.equalsIgnoreCase(_sender)
        && (_text == null || event.ptrStr().equalsIgnoreCase(_text))
        && (!_isToggle || event.state == Event.State.COMPLETED))
        {
            float value = parseEvent(event);
            if((value == thres_lower) || (value >= thres_lower && value < thres_upper))
                return true;
        }

        return false;
    }

    public float parseEvent(Event event)
    {
        float value;
        if(_isToggle)
        {
            value = 1 - _lastValue;
            _lastValue = value;
        }
        else
        {
            value = (event.state == Event.State.COMPLETED) ? 0 : 1;
        }

        return value;
    }

    protected void load(XmlPullParser xml, Context context)
    {
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "condition");
            _text = xml.getAttributeValue(null, "text");

            String toggle = xml.getAttributeValue(null, "toggle");
            if(toggle == null || toggle.compareToIgnoreCase("false") == 0)
                _isToggle = false;
            else
                _isToggle = true;
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }

        super.load(xml, context);
    }

    public float getLastValue()
    {
        return _lastValue;
    }
}
