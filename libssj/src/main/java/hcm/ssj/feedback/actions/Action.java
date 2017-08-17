/*
 * Action.java
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
	public int lock = 0;
	public int lockSelf = 0;

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
    {
		String lock_str = xml.getAttributeValue(null, "lock");
		if(lock_str != null)
			lock = Integer.valueOf(lock_str);

		lock_str = xml.getAttributeValue(null, "lockSelf");
		if(lock_str != null)
			lockSelf = Integer.valueOf(lock_str);
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
