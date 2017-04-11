/*
 * VisualInstance.java
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
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.classes.FeedbackClass;

/**
 * Created by Johnny on 01.12.2014.
 */
public class VisualAction extends Action
{
    public Drawable icons[];

    public int dur = 0;
    public int lock = 0;
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
                icons[i] = Drawable.createFromStream(context.getAssets().open(icon_names[i]), null);
            }

            String bright_str = xml.getAttributeValue(null, "brightness");
            if (bright_str != null)
                brightness = Float.valueOf(bright_str);

            String dur_str = xml.getAttributeValue(null, "dur");
            if (dur_str != null)
                dur = Integer.valueOf(dur_str);

            String lock_str = xml.getAttributeValue(null, "lock");
            if (lock_str != null)
                lock = Integer.valueOf(lock_str);
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }
    }
}
