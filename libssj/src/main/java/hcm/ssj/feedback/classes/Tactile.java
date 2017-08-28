/*
 * Tactile.java
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

package hcm.ssj.feedback.classes;

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
import hcm.ssj.core.Pipeline;
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

    boolean firstCall = true;
    boolean connected = false;

    private Myo myo = null;
    private hcm.ssj.myo.Myo myoConnector = null;
    private String deviceId;
    private BandComm msband = null;
    private Vibrate2Command cmd = null;

    private long lock = 0;

    private Device deviceType = Device.Myo;

    public Tactile(Context context)
    {
        this.context = context;
        type = Type.Tactile;
    }

    public void firstCall()
    {
        firstCall = false;
        connected = false;

        if(deviceType == Device.Myo) {
            Hub hub = Hub.getInstance();

            if (hub.getConnectedDevices().isEmpty())
            {
                myoConnector = new hcm.ssj.myo.Myo();
                myoConnector.options.macAddress.set(deviceId);
                myoConnector.connect();
            }

            long time = SystemClock.elapsedRealtime();
            while (hub.getConnectedDevices().isEmpty() && SystemClock.elapsedRealtime() - time < Pipeline.getInstance().options.waitSensorConnect.get() * 1000) {
                try {
                    Thread.sleep(Cons.SLEEP_IN_LOOP);
                } catch (InterruptedException e) {
                }
            }

            if (hub.getConnectedDevices().isEmpty())
                throw new RuntimeException("device not found");

            Log.i("connected to Myo");

            connected = true;
            myo = hub.getConnectedDevices().get(0);
            cmd = new Vibrate2Command(hub);
        }
        else if(deviceType == Device.MsBand)
        {
            int id = deviceId == null ? 0 : Integer.valueOf(deviceId);
            msband = new BandComm(id);
        }
    }

    @Override
    public boolean execute(Action action)
    {
        if(firstCall)
            firstCall();

        if(!connected)
            return false;

        Log.i("execute");
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

            deviceId = xml.getAttributeValue(null, "deviceId");
        }
        catch(IOException | XmlPullParserException | InvalidParameterException e)
        {
            Log.e("error parsing config file", e);
        }

        super.load(xml, context);
    }

    @Override
    public void release()
    {
        connected = false;
        firstCall = true;

        if(myoConnector != null)
            myoConnector.disconnect();
        super.release();
    }
}
