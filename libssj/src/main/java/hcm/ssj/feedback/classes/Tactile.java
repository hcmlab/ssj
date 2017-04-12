/*
 * TactileFeedback.java
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
import android.os.SystemClock;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.InvalidParameterException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.feedback.BandComm;
import hcm.ssj.feedback.actions.Action;
import hcm.ssj.feedback.actions.TactileAction;
import hcm.ssj.myo.Vibrate2Command;


/**
 * Created by Johnny on 01.12.2014.
 */
public class Tactile extends FeedbackClass
{
    enum Device
    {
        Myo,
        MsBand
    }

    Activity activity;

    boolean firstCall = true;
    Myo myo = null;
    BandComm msband = null;
    Vibrate2Command cmd = null;

    long lock = 0;

    Device deviceType = Device.Myo;

    public Tactile(Activity activity)
    {
        this.activity = activity;
        type = Type.Tactile;
    }

    public void firstCall()
    {
        if(deviceType == Device.Myo) {
            Hub hub = Hub.getInstance();

            long time = SystemClock.elapsedRealtime();
            while (hub.getConnectedDevices().isEmpty() && SystemClock.elapsedRealtime() - time < Cons.WAIT_BL_CONNECT) {
                try {
                    Thread.sleep(Cons.SLEEP_IN_LOOP);
                } catch (InterruptedException e) {
                }
            }

            if (hub.getConnectedDevices().isEmpty())
                throw new RuntimeException("device not found");

            Log.i("connected to Myo");

            myo = hub.getConnectedDevices().get(0);
            cmd = new Vibrate2Command(hub);
        }
        else if(deviceType == Device.MsBand)
        {
            msband = new BandComm();
        }

        firstCall = false;
    }

    @Override
    public boolean execute(Action action)
    {
        if(firstCall)
            firstCall();

        TactileAction ev = (TactileAction) action;

        //check locks
        //global
        if(System.currentTimeMillis() < lock)
        {
            Log.i("ignoring event, lock active for another " + (lock - System.currentTimeMillis()) + "ms");
            return false;
        }
        //local
        if (System.currentTimeMillis() - ev.lastExecutionTime < ev.lockSelf)
        {
            Log.i("ignoring event, self lock active for another " + (ev.lockSelf - (System.currentTimeMillis() - ev.lastExecutionTime)) + "ms");
            return false;
        }

        if(deviceType == Device.Myo) {
            Log.i("vibration " + ev.duration[0] + "/" + (int) ev.intensity[0]);
            cmd.vibrate(myo, ev.duration, ev.intensity);
        }
        else if(deviceType == Device.MsBand) {
            Log.i("vibration " + ev.vibrationType);
            msband.vibrate(ev.vibrationType);
        }

        //set lock
        if(ev.lock > 0)
            lock = System.currentTimeMillis() + (long) ev.lock;
        else
            lock = 0;

        return true;
    }

    public byte[] multiply(byte[] src, float mult)
    {
        byte dst[] = new byte[src.length];

        int val_int;
        for(int i = 0; i < src.length; ++i)
        {
            val_int = (int)((int)src[i] * mult);
            if(val_int > 255)
                val_int = 255;

            dst[i] = (byte)val_int;
        }

        return dst;
    }

    protected void load(XmlPullParser xml, final Context context)
    {
        try {
            xml.require(XmlPullParser.START_TAG, null, "feedback");

            String device_name = xml.getAttributeValue(null, "device");
            if (device_name != null) {
                deviceType = Device.valueOf(device_name);
            }
        }
        catch(IOException | XmlPullParserException | InvalidParameterException e)
        {
            Log.e("error parsing config file", e);
        }

        super.load(xml, context);
    }
}
