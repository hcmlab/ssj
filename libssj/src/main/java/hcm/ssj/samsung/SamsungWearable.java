/*
 * SamsungWearable.java
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

package hcm.ssj.samsung;

import android.os.SystemClock;

import com.samsung.android.sdk.accessory.SAAgentV2;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 04.06.2021.
 */
public class SamsungWearable extends Sensor implements SAAgentV2.RequestAgentCallback, SamsungAccessoryConsumer.AccessoryAnnotationListener
{
	SamsungAccessoryConsumer accessoryConsumer;

	public SamsungWearable()
	{
		_name = "SamsungWearable";
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}

	@Override
	public void onAgentAvailable(SAAgentV2 saAgentV2)
	{
		accessoryConsumer = (SamsungAccessoryConsumer) saAgentV2;
		accessoryConsumer.setAnnotationListener(this);
	}

	@Override
	public void onError(int errorCode, String message)
	{

	}

	@Override
	protected boolean connect() throws SSJFatalException
	{
		boolean connected = false;

		// Wait for saAgent
		if (accessoryConsumer == null)
		{
			SAAgentV2.requestAgent(SSJApplication.getAppContext(), SamsungAccessoryConsumer.class.getName(), this);

			long startTime = SystemClock.elapsedRealtime();
			while (!_terminate && accessoryConsumer == null && SystemClock.elapsedRealtime() - startTime < _frame.options.waitSensorConnect.get() * 1000 / 2)
			{
				try
				{
					Thread.sleep(Cons.SLEEP_IN_LOOP);
				}
				catch (InterruptedException ignored)
				{
				}
			}
		}

		if (accessoryConsumer != null)
		{
			accessoryConsumer.connect();

			long startTime = SystemClock.elapsedRealtime();
			while (!_terminate && !accessoryConsumer.isConnected() && SystemClock.elapsedRealtime() - startTime < _frame.options.waitSensorConnect.get() * 1000)
			{
				try
				{
					Thread.sleep(Cons.SLEEP_IN_LOOP);
				}
				catch (InterruptedException ignored)
				{
				}
			}

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException ignored)
			{
			}

			connected = accessoryConsumer.isConnected();

			Log.i("Connected with wearable: " + connected);

			if (!connected)
			{
				disconnect();
			}
		}

		return connected;
	}

	@Override
	protected void disconnect() throws SSJFatalException
	{
		if (accessoryConsumer != null)
		{
			accessoryConsumer.sendCommand(SamsungAccessoryConsumer.AccessoryCommand.PIPELINE_STOP);

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException ignored)
			{
			}

			accessoryConsumer.disconnect();
		}
	}

	@Override
	protected boolean checkConnection()
	{
		boolean connected = false;

		if (accessoryConsumer != null)
		{
			connected = accessoryConsumer.isConnected();
		}

		return connected;
	}

	@Override
	public void handleAnnotation(String data)
	{
		if (_evchannel_out != null)
		{
			Event ev = Event.create(Cons.Type.STRING);
			ev.sender = "samsung-wearable";
			ev.name = "annotation";
			ev.time = (long) (_frame.getTimeMs() + 0.5);
			ev.dur = 0;
			ev.state = Event.State.COMPLETED;

			ev.setData(data);
			_evchannel_out.pushEvent(ev);
		}
	}
}
