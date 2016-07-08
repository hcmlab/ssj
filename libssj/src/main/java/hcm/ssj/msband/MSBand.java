/*
 * MSBand.java
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

package hcm.ssj.msband;

import android.content.SharedPreferences;
import android.os.SystemClock;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class MSBand extends Sensor implements HeartRateConsentListener
{
	protected BandClient client;
	protected BandListener listener;

	@Override
	protected boolean connect()
	{
		boolean connected = false;

		listener = new BandListener();

		BandInfo[] devices = BandClientManager.getInstance().getPairedBands();

		if (devices.length > 0)
		{
			client = BandClientManager.getInstance().create(SSJApplication.getAppContext(), devices[0]);

			try
			{
				if (ConnectionState.CONNECTED == client.connect().await())
				{
					SharedPreferences pref = SSJApplication.getAppContext().getSharedPreferences("p7", 0);
					pref.edit().putInt("c3", 1).commit();

					// Register listeners
					client.getSensorManager().registerHeartRateEventListener(listener);
					client.getSensorManager().registerRRIntervalEventListener(listener);
					client.getSensorManager().registerGsrEventListener(listener, GsrSampleRate.MS200);
					client.getSensorManager().registerSkinTemperatureEventListener(listener);
					client.getSensorManager().registerAccelerometerEventListener(listener, SampleRate.MS16);
					client.getSensorManager().registerGyroscopeEventListener(listener, SampleRate.MS16);
					client.getSensorManager().registerAmbientLightEventListener(listener);
					client.getSensorManager().registerBarometerEventListener(listener);
					client.getSensorManager().registerCaloriesEventListener(listener);
					client.getSensorManager().registerDistanceEventListener(listener);
					client.getSensorManager().registerPedometerEventListener(listener);
					client.getSensorManager().registerAltimeterEventListener(listener);

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
						Log.e("Unable to connect to ms band");
					}
				}
			}
			catch (InterruptedException|BandException e)
			{
				Log.e("Error while connection to ms band", e);
			}
			catch (InvalidBandVersionException e)
			{
				Log.e("Old ms band version not supported", e);
			}
		}

		return connected;
	}

	@Override
	protected void disconnect()
	{
		if (client != null)
		{
			try
			{
				client.getSensorManager().unregisterAllListeners();
				client.disconnect().await();
			}
			catch (InterruptedException|BandException e)
			{
				Log.e("Error while disconnecting from ms band", e);
			}
		}
	}

	@Override
	public void userAccepted(boolean b)
	{
		try
		{
			client.getSensorManager().registerHeartRateEventListener(listener);
		}
		catch (BandException e)
		{
			Log.w("did not get consent for HR sensor", e);
		}
	}
}
