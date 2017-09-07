/*
 * MSBandTactileFeedback.java
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

import com.microsoft.band.notifications.VibrationType;

import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class MSBandTactileFeedback extends Feedback
{
	public class Options extends Feedback.Options
	{
		public final Option<int[]> duration = new Option<>("duration", new int[]{500}, int[].class, "duration of tactile feedback");
		public final Option<VibrationType> vibrationType = new Option<>("vibrationType", VibrationType.NOTIFICATION_ONE_TONE, VibrationType.class, "vibration type");
		public final Option<Integer> deviceId = new Option<>("deviceId", 0, Integer.class, "device Id");

		private Options()
		{
			super();
			addOptions();
		}
	}
	public final Options options = new Options();

	private BandComm msband = null;

	public MSBandTactileFeedback()
	{
		_name = "MSBandTactileFeedback";
		Log.d("Instantiated MSBandTactileFeedback " + this.hashCode());
	}

	@Override
	public void enter()
	{
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new RuntimeException("no input channels");
		}
		msband = new BandComm(options.deviceId.get());
	}

	@Override
	public void notify(Event event)
	{
		// Execute only if lock has expired
		if (checkLock(options.lock.get()))
		{

			Log.i("vibration " + options.vibrationType.get());
			msband.vibrate(options.vibrationType.get());
		}
	}
}
