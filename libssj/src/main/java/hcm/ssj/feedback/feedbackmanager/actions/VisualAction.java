/*
 * VisualAction.java
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

package hcm.ssj.feedback.feedbackmanager.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.feedbackmanager.classes.FeedbackClass;

/**
 * Created by Johnny on 01.12.2014.
 */
public class VisualAction extends Action
{
    public Drawable icons[];

    public int dur = 0;
    public float brightness = 1;

    public VisualAction()
    {
        type = FeedbackClass.Type.Visual;
    }

    protected void load(XmlPullParser xml, Context context)
    {
        super.load(xml, context);

        try
        {
            xml.require(XmlPullParser.START_TAG, null, "action");

            String res_str = xml.getAttributeValue(null, "res");
            if(res_str == null)
                throw new IOException("event resource not defined");

            String[] icon_names = res_str.split("\\s*,\\s*");
            if(icon_names.length > 2)
                throw new IOException("unsupported amount of resources");

            int num = icon_names.length;
            icons = new Drawable[num];

            for(int i = 0; i< icon_names.length; i++)
            {
                String assetsString = "assets:";
                if(icon_names[i].startsWith(assetsString))
                {
                    icons[i] = Drawable.createFromStream(context.getAssets().open(icon_names[i].substring(assetsString.length())), null);
                }
                else {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + icon_names[i];
                    icons[i] =  Drawable.createFromStream(new FileInputStream(path), null);
                }
            }

            String bright_str = xml.getAttributeValue(null, "brightness");
            if (bright_str != null)
                brightness = Float.valueOf(bright_str);

            String dur_str = xml.getAttributeValue(null, "duration");
            if (dur_str != null)
                dur = Integer.valueOf(dur_str);
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }
    }
}
