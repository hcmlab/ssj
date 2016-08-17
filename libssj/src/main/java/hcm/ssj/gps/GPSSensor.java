/*
 * GPSSensor.java
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

package hcm.ssj.gps;

import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 05.07.2016.
 */
public class GPSSensor extends Sensor
{
	public class Options extends OptionList
	{
		public final Option<Long>    minTime     = new Option<>("minTime", 200L, Long.class, "Minimum time interval between location updates, in milliseconds");
		public final Option<Float>   minDistance = new Option<>("minDistance", 1f, Float.class, "Minimum distance between location updates, in meters");
		public final Option<Boolean> ignoreData  = new Option<>("ignoreData", false, Boolean.class, "Set to 'true' if no error should occur when no data is received from gps");
		public final Option<Boolean> useNetwork  = new Option<>("useNetwork", true, Boolean.class, "Set to 'false' if NETWORK_PROVIDER should not be used as fallback");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	GPSListener     listener;
	LocationManager locationManager;

	public GPSSensor()
	{
		_name = "GPS";
	}

	@Override
	protected boolean connect()
	{
		boolean connected = false;

		listener = new GPSListener(options.minTime.get());

		locationManager = (LocationManager) SSJApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(new Runnable()
			{
				public void run()
				{
					// Register listener
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, options.minTime.get(), options.minDistance.get(), listener);

					if (options.useNetwork.get())
					{
						locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, options.minTime.get(), options.minDistance.get(), listener);
					}
				}
			}, 1);

			// Wait for values
			long time = SystemClock.elapsedRealtime();
			while (!_terminate && !listener.receivedData && SystemClock.elapsedRealtime() - time < Cons.WAIT_SENSOR_CONNECT)
			{
				try
				{
					Thread.sleep(Cons.SLEEP_ON_IDLE);
				}
				catch (InterruptedException e)
				{
				}
			}

			if (listener.receivedData)
			{
				connected = true;
			}
			else
			{
				Log.e("unable to connect to gps");
			}
		}

		if (options.ignoreData.get() && connected == false)
		{
			connected = true;
		}

		return connected;
	}

	@Override
	protected void disconnect()
	{
		if (locationManager != null && listener != null)
		{
			// Remove listener
			locationManager.removeUpdates(listener);
		}
	}
}
