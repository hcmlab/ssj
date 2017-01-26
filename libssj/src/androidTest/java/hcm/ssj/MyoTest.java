/*
 * MyoTest.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

package hcm.ssj;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.test.ApplicationTestCase;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import hcm.ssj.core.Log;
import hcm.ssj.core.TheFramework;
import hcm.ssj.myo.DynAccelerationChannel;
import hcm.ssj.myo.EMGChannel;
import hcm.ssj.myo.Vibrate2Command;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 02.04.2015.
 */
public class MyoTest extends ApplicationTestCase<Application> {

    String _name = "Myo";

    public MyoTest() {
        super(Application.class);
    }

    public void testAcc() throws Exception {
        TheFramework frame = TheFramework.getFramework();

        hcm.ssj.myo.Myo myo = new hcm.ssj.myo.Myo();
        DynAccelerationChannel acc = new DynAccelerationChannel();
        acc.options.sampleRate.set(10);

        frame.addSensor(myo,acc);

        Logger dummy = new Logger();
        frame.addConsumer(dummy, acc, 0.1, 0);

        try {
            frame.Start();

            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() > start + 20 * 60 * 1000) {
                    break;
                }

                Thread.sleep(1);
            }

            frame.Stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("ACC test finished");
    }

    public void testEmg() throws Exception {
        TheFramework frame = TheFramework.getFramework();

        hcm.ssj.myo.Myo myo = new hcm.ssj.myo.Myo();
        EMGChannel emg = new EMGChannel();
        emg.options.sampleRate.set(10);

        frame.addSensor(myo,emg);

        Logger dummy = new Logger();
        frame.addConsumer(dummy, emg, 0.1, 0);

        try {
            frame.Start();

            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() > start + 10 * 1000) {
                    break;
                }

                Thread.sleep(1);
            }

            frame.Stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("emg test finished");
    }

    /**
     * Method to test the vibrate2-functionality of myo
     */
    public void testVibrate() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                final Hub hub = Hub.getInstance();
                if (!hub.init(getContext(), getContext().getPackageName())) {
                    Log.e("error");
                }
                hub.setLockingPolicy(Hub.LockingPolicy.NONE);

                // Disable usage data sending
                hub.setSendUsageData(false);

                Log.i("attaching...");
                hub.attachByMacAddress("F3:41:FA:27:EB:08");
                Log.i("attached...");
                hub.addListener(new AbstractDeviceListener() {
                    @Override
                    public void onConnect(Myo myo, long timestamp) {
                        super.onAttach(myo, timestamp);
                        startVibrate(myo, hub);
                    }
                });

            }
        }, 1);

        try {
            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() > start + 30 * 1000) {
                    break;
                }

                Thread.sleep(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                final Hub hub = Hub.getInstance();
                hub.shutdown();
            }
        }, 1);
    }

    private void startVibrate(Myo myo, Hub hub) {
        Log.i("connected");
        try {
            Vibrate2Command vibrate2Command = new Vibrate2Command(hub);

            Log.i("vibrate 1...");
            myo.vibrate(Myo.VibrationType.MEDIUM);
            Thread.sleep(3000);

            Log.i("vibrate 2...");
            //check strength 50
            vibrate2Command.vibrate(myo, 1000, (byte) 50);
            Thread.sleep(3000);

            Log.i("vibrate 3 ...");
            //check strength 100
            vibrate2Command.vibrate(myo, 1000, (byte) 100);
            Thread.sleep(3000);

            Log.i("vibrate 4 ...");
            //check strength 100
            vibrate2Command.vibrate(myo, 1000, (byte) 150);
            Thread.sleep(3000);

            Log.i("vibrate 5...");
            //check strength 250
            vibrate2Command.vibrate(myo, 1000, (byte) 200);
            Thread.sleep(3000);

            Log.i("vibrate 6...");
            //check strength 250
            vibrate2Command.vibrate(myo, 1000, (byte) 250);
            Thread.sleep(3000);

            Log.i("vibrate pattern...");
            //check vibrate pattern
            vibrate2Command.vibrate(myo, new int[]{500, 500, 500, 500, 500, 500}, new byte[]{25, 50, 100, (byte) 150, (byte) 200, (byte) 250});
            Thread.sleep(3000);
        } catch (Exception e) {
            Log.e("exception in vibrate test", e);
        }
    }
}
