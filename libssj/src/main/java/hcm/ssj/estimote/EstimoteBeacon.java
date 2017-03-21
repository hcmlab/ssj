/*
 * EstimoteBeacon.java
 * Copyright (c) 2017
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

package hcm.ssj.estimote;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.UUID;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 08.03.2017.
 */

public class EstimoteBeacon extends Sensor implements BeaconManager.ServiceReadyCallback
{
	public enum IdentificationMode
	{
		MAC_ADDRESS,
		UUID_MAJOR_MINOR
	}

	public class Options extends OptionList
	{
		public final Option<String>             region = new Option<>("region", "beacon region", String.class, "");
		public final Option<String>             uuid   = new Option<>("uuid", "", String.class, "");
		public final Option<Integer>            major  = new Option<>("major", null, Integer.class, "");
		public final Option<Integer>            minor  = new Option<>("minor", null, Integer.class, "");
		public final Option<IdentificationMode> idMode = new Option<>("idMode", IdentificationMode.UUID_MAJOR_MINOR, IdentificationMode.class, "");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	protected BeaconManager  beaconManager;
	protected BeaconListener listener;
	protected Region         region;

	protected boolean connected;

	public EstimoteBeacon()
	{
		_name = "EstimoteBeacon";

		listener = new BeaconListener();
	}

	@Override
	protected boolean connect()
	{
		Log.i("Connecting to estimote beacons");
		connected = false;

		listener.reset();
		listener.setIdMode(options.idMode.get());

		region = new Region(options.region.get(), options.uuid.get() != null ? UUID.fromString(options.uuid.get()) : null, options.major.get(), options.minor.get());

		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable()
		{
			public void run()
			{
				beaconManager = new BeaconManager(SSJApplication.getAppContext());
				beaconManager.connect(EstimoteBeacon.this);
				beaconManager.setRangingListener(listener);
				beaconManager.setForegroundScanPeriod(200, 0);
				//beaconManager.setBackgroundScanPeriod(200, 0);
			}
		}, 1);

		long time = SystemClock.elapsedRealtime();
		while (!_terminate && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000)
		{
			try
			{
				Thread.sleep(Cons.SLEEP_IN_LOOP);
			}
			catch (InterruptedException e)
			{
			}
		}

		if (!connected)
		{
			Log.e("Unable to connect to estimote beacons");
		}

		return connected;
	}

	@Override
	public void onServiceReady()
	{
		Log.i("Estimote service ready, starting ranging");

		connected = true;

		beaconManager.startRanging(region);
	}

	@Override
	protected void disconnect()
	{
		connected = false;

		if (beaconManager != null && region != null)
		{
			beaconManager.stopRanging(region);
			beaconManager.disconnect();
		}
	}
}
