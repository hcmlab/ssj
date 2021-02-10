/*
 * LowPassFilter.java
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

package hcm.ssj.landmark.utils;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 02.02.2021.
 *
 * Based on:
 * https://github.com/google/mediapipe/blob/master/mediapipe/util/filtering/low_pass_filter.h
 * https://github.com/google/mediapipe/blob/master/mediapipe/util/filtering/low_pass_filter.cc
 */
public class LowPassFilter
{
	float alpha;
	float rawValue;
	float storedValue;

	boolean initialized = false;

	public LowPassFilter(float alpha)
	{
		setAlpha(alpha);
	}

	public float apply(float value)
	{
		float result;

		if (initialized)
		{
			result = alpha * value + (1.0f - alpha) * storedValue;
		}
		else
		{
			result = value;
			initialized = true;
		}

		rawValue = value;
		storedValue = result;

		return result;
	}

	public float applyWithAlpha(float value, float alpha)
	{
		setAlpha(alpha);

		return apply(value);
	}

	public boolean hasLastRawValue()
	{
		return initialized;
	}

	public float lastRawValue()
	{
		return rawValue;
	}

	public float lastValue()
	{
		return storedValue;
	}

	private void setAlpha(float alpha)
	{
		if (alpha < 0.0f || alpha > 1.0f)
		{
			Log.e("Alpha " + alpha + " should be in [0.0, 1.0] range");
		}
		else
		{
			this.alpha = alpha;
		}
	}
}
