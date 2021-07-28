/*
 * GATTReader.java
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

package hcm.ssj.ioput;

import android.os.Build;
import android.os.SystemClock;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 16.07.2021.
 */
public class GATTReader extends Sensor
{
	public class Options extends OptionList
	{
		public final Option<String> macAddress = new Option<>("macAddress", "", String.class, "MAC address (in format '00:11:22:33:44:55')");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	GATTConnection connection;

	public GATTReader()
	{
		_name = "GATTReader";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	protected void init() throws SSJException
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			connection = new GATTConnection();
		}
	}

	@Override
	protected boolean connect() throws SSJFatalException
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			connection.connect(options.macAddress.get());

			// Wait until sensor is connected
			long time = SystemClock.elapsedRealtime();
			while (!connection.isConnected() && !_terminate && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000)
			{
				try
				{
					Thread.sleep(Cons.SLEEP_IN_LOOP);
				}
				catch (InterruptedException ignored)
				{
				}
			}

			if (!connection.isConnected())
			{
				Log.w("Unable to connect to GATT sensor.");
				disconnect();

				return false;
			}

			return true;
		}

		return false;
	}

	@Override
	protected void disconnect() throws SSJFatalException
	{
		Log.i("Disconnecting GATT Sensor");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			if (connection != null)
			{
				connection.disconnect();
				connection.close();
			}
		}
	}

	@Override
	protected boolean checkConnection()
	{
		boolean connected = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			if (connection != null)
			{
				connected = connection.isConnected();
			}
		}

		return connected;
	}
}
