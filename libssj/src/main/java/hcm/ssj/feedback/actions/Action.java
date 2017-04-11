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

package hcm.ssj.feedback.actions;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;

import hcm.ssj.feedback.classes.FeedbackClass;

/**
 * Created by Johnny on 01.12.2014.
 */
public class Action
{
    protected static int s_id = 0;
    public long lastExecutionTime = 0;
    protected int id;
    protected FeedbackClass.Type type;

    protected Action()
    {
        id = s_id++;
    }

    public static Action create(FeedbackClass.Type feedback_type, XmlPullParser xml, Context context)
    {
        Action i = null;
        if (feedback_type == FeedbackClass.Type.Visual)
            i = new VisualAction();
        else if (feedback_type == FeedbackClass.Type.Tactile)
            i = new TactileAction();
        else if (feedback_type == FeedbackClass.Type.Audio)
            i = new AudioAction();
        else
            i = new Action();

        i.load(xml, context);
        return i;
    }

    public void release()
    {}

    protected void load(XmlPullParser xml, Context context)
    {}

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
