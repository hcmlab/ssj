/*
 * SpeechRate.java
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
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;

import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;

/**
 * Created by Johnny on 01.12.2014.
 */
public class SpeechRate extends Condition
{
    LinkedList<Float> _sr = new LinkedList<Float>();
    int _history_size;

    XmlPullParser _parser;

    public SpeechRate()
    {
        try
        {
            _parser = Xml.newPullParser();
            _parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        }
        catch (XmlPullParserException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public float parseEvent(Event event)
    {
        try
        {
            float srate = 0;
            _parser.setInput(new StringReader(event.ptrStr()));

            while (_parser.next() != XmlPullParser.END_DOCUMENT)
            {
                if (_parser.getEventType() == XmlPullParser.START_TAG && _parser.getName().equalsIgnoreCase("tuple"))
                {
                    if (_parser.getAttributeValue(null, "string").equalsIgnoreCase("Speechrate (syllables/sec)")) {
                        srate = Float.parseFloat(_parser.getAttributeValue(null, "value"));

                        _sr.add(srate);
                        if (_sr.size() > _history_size)
                            _sr.removeFirst();

                        break;
                    }
                }
                else if(_parser.getEventType() == XmlPullParser.END_TAG && _parser.getName().equalsIgnoreCase("event"))
                    break; //jump out once we reach end tag
            }
        }
        catch (XmlPullParserException | IOException e)
        {
            throw new RuntimeException(e);
        }

        float value = getAvg(_sr);
        Log.d("SpeechRate_avg = " + value);

        return value;
    }

    private float getAvg(LinkedList<Float> vec)
    {
        if(vec.size() == 0)
            return 0;

        float sum = 0;
        for(float i : vec)
        {
            sum += i;
        }
        return sum / vec.size();
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

        _history_size = Integer.getInteger(xml.getAttributeValue(null, "history"), 5);
        super.load(xml, context);
    }
}
