/*
 * MyoTactileFeedback.java
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

package hcm.ssj.feedback;

import android.os.SystemClock;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.myo.Vibrate2Command;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class MyoTactileFeedback extends Feedback
{
	public class Options extends Feedback.Options
	{
		public final Option<int[]> duration = new Option<>("duration", new int[]{500}, int[].class, "duration of tactile feedback");
		public final Option<byte[]> intensity = new Option<>("intensity", new byte[]{(byte) 150}, byte[].class, "intensity of tactile feedback");
		public final Option<String> deviceId = new Option<>("deviceId", null, String.class, "device Id");

		private Options()
		{
			super();
			addOptions();
		}
	}
	public final Options options = new Options();


	private Myo myo = null;
	private hcm.ssj.myo.Myo myoConnector = null;
	private Vibrate2Command cmd = null;
	private int[] duration;
	private byte[] intensity;

	public MyoTactileFeedback()
	{
		_name = "MyoTactileFeedback";
		Log.d("Instantiated MyoTactileFeedback " + this.hashCode());
	}

	@Override
	public void enter()
	{
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new RuntimeException("no input channels");
		}


		Hub hub = Hub.getInstance();

		if (hub.getConnectedDevices().isEmpty())
		{
			myoConnector = new hcm.ssj.myo.Myo();
			myoConnector.options.macAddress.set(options.deviceId.get());
			myoConnector.connect();
		}

		long time = SystemClock.elapsedRealtime();
		while (hub.getConnectedDevices().isEmpty() && SystemClock.elapsedRealtime() - time < Pipeline.getInstance().options.waitSensorConnect.get() * 1000)
		{
			try
			{
				Thread.sleep(Cons.SLEEP_IN_LOOP);
			}
			catch (InterruptedException e)
			{
				Log.d("connection interrupted", e);
			}
		}

		if (hub.getConnectedDevices().isEmpty())
		{
			throw new RuntimeException("device not found");
		}

		Log.i("connected to Myo");

		myo = hub.getConnectedDevices().get(0);
		cmd = new Vibrate2Command(hub);

		lock = options.lock.get();
		duration = options.duration.get();
		intensity = options.intensity.get();
	}

	@Override
	public void notify(Event event)
	{
		// Execute only if lock has expired
		if (checkLock())
		{
			Log.i("vibration " + duration[0] + "/" + (int) intensity[0]);
			cmd.vibrate(myo, duration, intensity);
		}
	}

	@Override
	public void flush()
	{
		if (myoConnector != null)
		{
			myoConnector.disconnect();
		}
	}
}
