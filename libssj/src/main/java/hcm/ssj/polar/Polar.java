/*
 * Polar.java
 * Copyright (c) 2021
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

package hcm.ssj.polar;

import android.os.SystemClock;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.errors.PolarInvalidArgument;

/**
 * Created by Michael Dietz on 08.04.2021.
 */
public class Polar extends Sensor
{
	public class Options extends OptionList
	{
		public final Option<String> deviceIdentifier = new Option<>("deviceIdentifier", "", String.class, "Polar device id found printed on the sensor/device (in format '12345678') or bt address (in format '00:11:22:33:44:55')");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	PolarListener listener;
	PolarBleApi api;

	public Polar()
	{
		_name = "Polar";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	protected boolean connect() throws SSJFatalException
	{
		Log.d("Connecting to: " + options.deviceIdentifier.get());

		api = PolarBleApiDefaultImpl.defaultImplementation(SSJApplication.getAppContext(), PolarBleApi.FEATURE_HR | PolarBleApi.FEATURE_DEVICE_INFO | PolarBleApi.FEATURE_BATTERY_INFO | PolarBleApi.FEATURE_POLAR_SENSOR_STREAMING);
		listener = new PolarListener(api, options.deviceIdentifier.get());
		api.setApiCallback(listener);

		try
		{
			api.connectToDevice(options.deviceIdentifier.get());
		}
		catch (PolarInvalidArgument polarInvalidArgument)
		{
			throw new SSJFatalException(polarInvalidArgument);
		}

		// Wait until sensor is connected
		long time = SystemClock.elapsedRealtime();
		while (!listener.connected && !_terminate && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000)
		{
			try
			{
				Thread.sleep(Cons.SLEEP_IN_LOOP);
			}
			catch (InterruptedException e)
			{
			}
		}

		if (!listener.connected)
		{
			Log.w("Unable to connect to polar sensor. Make sure it is on and NOT paired to your smartphone.");
			disconnect();
			return false;
		}

		return true;
	}

	@Override
	protected void disconnect() throws SSJFatalException
	{
		Log.i("Disconnecting Polar Sensor");

		if (api != null)
		{
			try
			{
				api.disconnectFromDevice(options.deviceIdentifier.get());
				api.cleanup();
			}
			catch (PolarInvalidArgument polarInvalidArgument)
			{
				throw new SSJFatalException(polarInvalidArgument);
			}
		}
	}
}
