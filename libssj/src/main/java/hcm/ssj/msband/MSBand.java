/*
 * MSBand.java
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
import com.microsoft.band.sensors.SampleRate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class MSBand extends Sensor
{
	protected BandClient   client;
	protected BandListener listener;

	protected enum Channel
	{
		HeartRate,
		RRInterval,
		GSR,
		SkinTemperature,
		Acceleration,
		Gyroscope,
		AmbientLight,
		Barometer,
		Calories,
		Distance,
		Pedometer,
		Altimeter
	}

	private class ChannelInfo {
		public boolean active = false;
		public int srMode = SampleRate.MS16.ordinal();
	}

	protected ChannelInfo channels[] = new ChannelInfo[Channel.values().length];

	public void configureChannel(Channel ch, boolean active, int srMode)
	{
		channels[ch.ordinal()].active = active;
		channels[ch.ordinal()].srMode = srMode;
	}

	public MSBand()
	{
		_name = "MSBand";

		for (int i = 0; i < channels.length; i++) {
			channels[i] = new ChannelInfo();
		}

		listener = new BandListener();
	}

	@Override
	protected boolean connect() throws SSJFatalException
	{
		boolean connected = false;

		disconnect(); //clean up old connection
		listener.reset();

		Log.i("connecting to ms band ...");
		BandInfo[] devices = BandClientManager.getInstance().getPairedBands();

		if (devices.length > 0)
		{
			client = BandClientManager.getInstance().create(SSJApplication.getAppContext(), devices[0]);
			client.registerConnectionCallback(listener);

			try
			{
				if (ConnectionState.CONNECTED == client.connect().await())
				{
					SharedPreferences pref = SSJApplication.getAppContext().getSharedPreferences("p7", 0);
					pref.edit().putInt("c3", 1).commit();

					// Register listeners
					if(channels[Channel.HeartRate.ordinal()].active) client.getSensorManager().registerHeartRateEventListener(listener);
					if(channels[Channel.RRInterval.ordinal()].active) client.getSensorManager().registerRRIntervalEventListener(listener);
					if(channels[Channel.GSR.ordinal()].active) client.getSensorManager().registerGsrEventListener(listener, GsrSampleRate.values()[channels[Channel.GSR.ordinal()].srMode]);
					if(channels[Channel.SkinTemperature.ordinal()].active) client.getSensorManager().registerSkinTemperatureEventListener(listener);
					if(channels[Channel.Acceleration.ordinal()].active) client.getSensorManager().registerAccelerometerEventListener(listener, SampleRate.values()[channels[Channel.Acceleration.ordinal()].srMode]);
					if(channels[Channel.Gyroscope.ordinal()].active) client.getSensorManager().registerGyroscopeEventListener(listener, SampleRate.values()[channels[Channel.Gyroscope.ordinal()].srMode]);
					if(channels[Channel.AmbientLight.ordinal()].active) client.getSensorManager().registerAmbientLightEventListener(listener);
					if(channels[Channel.Barometer.ordinal()].active) client.getSensorManager().registerBarometerEventListener(listener);
					if(channels[Channel.Calories.ordinal()].active) client.getSensorManager().registerCaloriesEventListener(listener);
					if(channels[Channel.Distance.ordinal()].active) client.getSensorManager().registerDistanceEventListener(listener);
					if(channels[Channel.Pedometer.ordinal()].active) client.getSensorManager().registerPedometerEventListener(listener);
					if(channels[Channel.Altimeter.ordinal()].active) client.getSensorManager().registerAltimeterEventListener(listener);

					// Wait for values
					long time = SystemClock.elapsedRealtime();
					while (!_terminate && !listener.hasReceivedData() && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000)
					{
						try
						{
							Thread.sleep(Cons.SLEEP_IN_LOOP);
						}
						catch (InterruptedException e)
						{
						}
					}

					if (listener.hasReceivedData())
					{
						connected = true;
					}
					else
					{
						Log.e("Unable to connect to ms band");
					}
				}
			}
			catch (InterruptedException | BandException e)
			{
				throw new SSJFatalException("Error while connecting to ms band", e);
			}
			catch (InvalidBandVersionException e)
			{
				throw new SSJFatalException("Old ms band version not supported", e);
			}
		}

		return connected;
	}

	protected boolean checkConnection()
	{
		return listener.isConnected();
	}

	@Override
	protected void disconnect() throws SSJFatalException
	{
		Log.d("Disconnecting from MS Band");
		if (client != null)
		{
			try
			{
				client.getSensorManager().unregisterAllListeners();
				client.disconnect().await(1000, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException | TimeoutException | BandException e)
			{
				Log.e("Error while disconnecting from ms band", e);
			}
		}

		client = null;
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}
}
