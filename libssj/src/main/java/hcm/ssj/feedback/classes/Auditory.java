/*
 * VisualFeedback.java
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

package hcm.ssj.feedback.classes;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import org.xmlpull.v1.XmlPullParser;

import hcm.ssj.core.Log;
import hcm.ssj.feedback.actions.Action;
import hcm.ssj.feedback.actions.AudioAction;


/**
 * Created by Johnny on 01.12.2014.
 */
public class Auditory extends FeedbackClass
{
    Activity activity;

    long lock = 0;

    SoundPool player;

    public Auditory(Activity activity)
    {
        this.activity = activity;
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
        //update only if the global lock has passed
        if(System.currentTimeMillis() < lock)
        {
            Log.i("ignoring event, lock active for another " + (lock - System.currentTimeMillis()) + "ms");
            return false;
        }

        AudioAction ev = (AudioAction) action;

        //check self-lock
        //only execute if enough time has passed since last execution of this instance
        if (ev.lockSelf == -1 || System.currentTimeMillis() - ev.lastExecutionTime < ev.lockSelf)
            return false;

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
