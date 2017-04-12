/*
 * TactileInstance.java
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

import com.microsoft.band.notifications.VibrationType;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.classes.FeedbackClass;

/**
 * Created by Johnny on 01.12.2014.
 */
public class TactileAction extends Action
{
    public int[] duration = {500};
    public byte[] intensity = {(byte)150};
    public VibrationType vibrationType = VibrationType.NOTIFICATION_ONE_TONE;

    public TactileAction()
    {
        type = FeedbackClass.Type.Tactile;
    }

    protected void load(XmlPullParser xml, Context context)
    {
        super.load(xml, context);

        try
        {
            xml.require(XmlPullParser.START_TAG, null, "action");

            String str = xml.getAttributeValue(null, "intensity");
            if(str != null) intensity = parseByteArray(str, ",");

            str = xml.getAttributeValue(null, "duration");
            if(str != null) duration = parseIntArray(str, ",");

            str = xml.getAttributeValue(null, "type");
            if(str != null) vibrationType = VibrationType.valueOf(str);
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.e("error parsing config file", e);
        }
    }
}
