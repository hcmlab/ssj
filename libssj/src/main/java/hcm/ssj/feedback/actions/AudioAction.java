/*
 * AudioAction.java
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
import android.content.res.AssetFileDescriptor;
import android.media.SoundPool;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.classes.FeedbackClass;

/**
 * Created by Johnny on 01.12.2014.
 */
public class AudioAction extends Action
{
    Context _context = null;

    public AssetFileDescriptor _afd = null;

    public float intensity = 1;
    public int soundId;

    public AudioAction()
    {
        type = FeedbackClass.Type.Audio;
    }

    protected void load(XmlPullParser xml, Context context)
    {
        super.load(xml, context);
        _context = context;

        try
        {
            String res = xml.getAttributeValue(null, "res");
            if(res != null)
            {
                _afd =  context.getAssets().openFd(res);
            }
            else
                throw new IOException("no sound defined");

            String intensity_str = xml.getAttributeValue(null, "intensity");
            if(intensity_str != null)
                intensity = Float.valueOf(intensity_str);
        }
        catch(IOException e)
        {
            Log.e("error parsing config file", e);
        }
    }

    public void registerWithPlayer(SoundPool player)
    {
        try
        {
            soundId = player.load(_afd, 1);
            _afd.close();
        }
        catch (IOException e)
        {
            Log.e("error loading audio files", e);
        }
    }
}
