/*
 * Auditory.java
 * Copyright (c) 2018
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

package hcm.ssj.feedback.feedbackmanager.classes;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import org.xmlpull.v1.XmlPullParser;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.FeedbackManager;
import hcm.ssj.feedback.feedbackmanager.actions.Action;
import hcm.ssj.feedback.feedbackmanager.actions.AudioAction;


/**
 * Created by Johnny on 01.12.2014.
 */
public class Auditory extends FeedbackClass
{
    long lock = 0;

    SoundPool player;

    public Auditory(Context context, FeedbackManager.Options options)
    {
        this.context = context;
		this.options = options;
        type = Type.Audio;
    }

    public void release()
    {
        player.release();
        super.release();
    }

    @Override
    public boolean execute(Action action)
    {
        AudioAction ev = (AudioAction) action;

		//check locks
		//global
		if(System.currentTimeMillis() < lock)
		{
			Log.i("ignoring event, global lock active for another " + (lock - System.currentTimeMillis()) + "ms");
			return false;
		}
		//local
		if (System.currentTimeMillis() - ev.lastExecutionTime < ev.lockSelf)
		{
			Log.i("ignoring event, self lock active for another " + (ev.lockSelf - (System.currentTimeMillis() - ev.lastExecutionTime)) + "ms");
			return false;
		}

        player.play(ev.soundId, ev.intensity, ev.intensity, 1, 0, 1);

        //set lock
        if(ev.lock > 0)
            lock = System.currentTimeMillis() + (long) ev.lock;
        else
            lock = 0;

        return true;
    }

    protected void load(XmlPullParser xml, final Context context)
    {
        super.load(xml, context);

        player = new SoundPool(4, AudioManager.STREAM_NOTIFICATION, 0);
        ((AudioAction) action).registerWithPlayer(player);
    }
}
