/*
 * Instance.java
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

package hcm.ssj.feedback.events;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.classes.Feedback;

/**
 * Created by Johnny on 01.12.2014.
 */
public class Event
{
    protected static int s_id = 0;
    public float thres_lower = 0;
    public float thres_upper = 0;
    public long lastExecutionTime = 0;
    protected int id;
    protected Feedback.Type type;
    protected Feedback.Valence valence;

    protected Event()
    {
        id = s_id++;
    }

    public static Event create(Feedback.Type feedback_type, XmlPullParser xml, Context context)
    {
        Event i = null;
        if (feedback_type == Feedback.Type.Visual)
            i = new VisualEvent();
        else if (feedback_type == Feedback.Type.Tactile)
            i = new TactileEvent();
        else if (feedback_type == Feedback.Type.Audio)
            i = new AudioEvent();
        else
            i = new Event();

        i.load(xml, context);
        return i;
    }

    public Feedback.Valence getValence() {
        return valence;
    }

    public void release()
    {}

    protected void load(XmlPullParser xml, Context context)
    {
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "event");

            String from = xml.getAttributeValue(null, "from");
            String equals = xml.getAttributeValue(null, "equals");
            String to = xml.getAttributeValue(null, "to");

            String valence_str = xml.getAttributeValue(null, "valence");
            if(valence_str != null)
                valence = Feedback.Valence.valueOf(valence_str);

            if(equals != null)
            {
                thres_lower = Float.parseFloat(equals);
            }
            else if(from != null && to != null)
            {
                thres_lower = Float.parseFloat(from);
                thres_upper = Float.parseFloat(to);
            }
            else
            {
                throw new IOException("threshold value(s) not set");
            }

        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }
    }

    public int[] parseIntArray(String str, String delim)
    {
        String arr[] = str.split(delim);
        int out[] = new int[arr.length];
        for(int i = 0; i < arr.length; ++i)
            out[i] = Integer.valueOf(arr[i]);

        return out;
    }

    public byte[] parseByteArray(String str, String delim)
    {
        String arr[] = str.split(delim);
        byte out[] = new byte[arr.length];
        for(int i = 0; i < arr.length; ++i)
            out[i] = (byte)Integer.valueOf(arr[i]).intValue();

        return out;
    }

    @Override
    public String toString()
    {
        return type.toString() + "_" + id;
    }
}
