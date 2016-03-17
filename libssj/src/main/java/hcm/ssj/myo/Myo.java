/*
 * Myo.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssj.myo;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.thalmic.myo.Hub;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;

/**
 * Created by Michael Dietz on 01.04.2015.
 */
public class Myo extends Sensor
{
	public class Options
	{
		public String  macAddress    = "F3:41:FA:27:EB:08";

		//when locking is enabled, myo is locked by default
		//and gesture events are not triggered while in this state.
		//A special gesture is required to "unlock" the device
		public boolean locking =  false;

        public Configuration.EmgMode emg = Configuration.EmgMode.FILTERED;
        public boolean imu = true;

		//disabling the gesture classifier will also disable the "sync" mechanism
        public boolean gestures = false;
	}
	public Options options = new Options();

	protected Hub              hub;
	protected MyoListener      listener;
    protected boolean          myoInitialized;
    Configuration config;

	public Myo()
	{
		_name = "SSJ_sensor_Myo";
	}

	@Override
	public void connect()
	{
        myoInitialized = false;
		hub = Hub.getInstance();
        listener = new MyoListener();

		// Myo hub must be initialized in the main ui thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable()
		{
			public void run()
			{
				// Check if hub can be initialized
				if (!hub.init(SSJApplication.getAppContext()))
				{
					Log.e("Could not initialize the Hub.");
				}
				else
				{
					myoInitialized = true;
				}

				hub.setLockingPolicy(options.locking ? Hub.LockingPolicy.STANDARD : Hub.LockingPolicy.NONE);

				// Disable usage data sending
				hub.setSendUsageData(false);

                // Add listener for callbacks
                hub.addListener(listener);

				// Connect to myo
				if (hub.getConnectedDevices().isEmpty())
				{
					// If there is a mac address connect to it, otherwise look for myo nearby
					if (!options.macAddress.isEmpty())
					{
						Log.i("Connecting to MAC: " + options.macAddress);
						hub.attachByMacAddress(options.macAddress);
					}
					else
					{
						Log.i("Connecting to nearest myo");
						hub.attachToAdjacentMyo();
					}
				}
			}
		}, 1);

		// Wait until myo is connected
		long time = SystemClock.elapsedRealtime();
		while (!_terminate && hub.getConnectedDevices().isEmpty() && SystemClock.elapsedRealtime() - time < Cons.WAIT_SENSOR_CONNECT)
		{
			try {
				Thread.sleep(Cons.SLEEP_ON_IDLE);
			} catch (InterruptedException e) {}
		}

		if(hub.getConnectedDevices().isEmpty())
		{
			hub.shutdown();
			throw new RuntimeException("device not found");
		}

        com.thalmic.myo.Myo myo = hub.getConnectedDevices().get(0);

        //configure myo
        config = new Configuration(hub, listener, options.emg, options.imu, options.gestures);
        config.apply(myo.getMacAddress());

		myo.unlock(com.thalmic.myo.Myo.UnlockType.HOLD);

		Log.i("Myo successfully connected: " + myo.getMacAddress());
	}

	@Override
	public void disconnect()
	{
		com.thalmic.myo.Myo myo = hub.getConnectedDevices().get(0);
		myo.lock();

        config.undo(myo.getMacAddress());
		hub.shutdown();
    }
}
