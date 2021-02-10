/*
 * RelativeVelocityFilter.java
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

import hcm.ssj.core.LimitedSizeQueue;
import hcm.ssj.core.Log;

/**
 * This filter keeps track (on a window of specified size) of
 * value changes over time, which as result gives us velocity of how value
 * changes over time. With higher velocity it weights new values higher.
 * <p>
 * Use @window_size and @velocity_scale to tweak this filter for your use case.
 * <p>
 * higher @window_size adds to lag and to stability
 * lower @velocity_scale adds to lag and to stability
 * <p>
 * Based on:
 * https://github.com/google/mediapipe/blob/master/mediapipe/util/filtering/relative_velocity_filter.h
 * https://github.com/google/mediapipe/blob/master/mediapipe/util/filtering/relative_velocity_filter.cc
 */
public class RelativeVelocityFilter
{
	/**
	 * Define max cumulative duration assuming 5 frames per second is a good frame rate, so assuming 10 values
	 * per second or 1 / 5 of a second is a good duration per window element
	 */
	final long kAssumedMaxDuration = (long) (1e3 / 5);
	final double kMicroSecondsToSecond = 1e-3;

	float lastValue = 0;
	float lastValueScale = 1;
	long lastTimestamp = -1;

	int maxWindowSize;
	float velocityScale;
	LimitedSizeQueue<WindowElement> window;
	LowPassFilter lowPassFilter;
	DistanceEstimationMode distanceMode;

	public RelativeVelocityFilter(int windowSize, float velocityScale)
	{
		this(windowSize, velocityScale, DistanceEstimationMode.kLegacyTransition);
	}

	public RelativeVelocityFilter(int windowSize, float velocityScale, DistanceEstimationMode distanceMode)
	{
		this.maxWindowSize = windowSize;
		this.velocityScale = velocityScale;
		this.distanceMode = distanceMode;

		this.window = new LimitedSizeQueue<>(windowSize);
		this.lowPassFilter = new LowPassFilter(1.0f);
	}

	/**
	 * Applies filter to the value.
	 *
	 * @param timestamp  timestamp associated with the value (for instance,
	 *                   timestamp of the frame where you got value from)
	 * @param valueScale value scale (for instance, if your value is a distance
	 *                   detected on a frame, it can look same on different
	 *                   devices but have quite different absolute values due
	 *                   to different resolution, you should come up with an
	 *                   appropriate parameter for your particular use case)
	 * @param value      value to filter
	 */
	public float apply(long timestamp, float valueScale, float value)
	{
		if (lastTimestamp >= timestamp)
		{
			Log.w("New timestamp is equal or less than the last one");

			return value;
		}

		float alpha;

		if (lastTimestamp == -1)
		{
			alpha = 1.0f;
		}
		else
		{
			float distance = distanceMode == DistanceEstimationMode.kLegacyTransition
					? value * valueScale - lastValue * lastValueScale // Original
					: valueScale * (value - lastValue); // Translation invariant

			long duration = timestamp - lastTimestamp;

			float cumulativeDistance = distance;
			long cumulativeDuration = duration;

			long maxCumulativeDuration = (1 + window.size()) * kAssumedMaxDuration;

			// Iterate through queue in reverse to start with newest elements (newest added at end)
			for (int i = window.size() - 1; i > 0; i--)
			{
				WindowElement element = window.get(i);

				if (cumulativeDuration + element.duration > maxCumulativeDuration)
				{
					/*
					This helps in cases when durations are large and outdated
					window elements have bad impact on filtering results
					 */
					break;
				}

				cumulativeDistance += element.distance;
				cumulativeDuration += element.duration;
			}

			final float velocity = (float) (cumulativeDistance / (cumulativeDuration * kMicroSecondsToSecond));
			alpha = 1.0f - 1.0f / (1.0f + velocityScale * Math.abs(velocity));

			window.add(new WindowElement(distance, duration));
		}

		lastValue = value;
		lastValueScale = valueScale;
		lastTimestamp = timestamp;


		return lowPassFilter.applyWithAlpha(value, alpha);
	}

	static class WindowElement
	{
		public float distance;
		public long duration;

		public WindowElement(float distance, long duration)
		{
			this.distance = distance;
			this.duration = duration;
		}
	}

	enum DistanceEstimationMode
	{
		/**
		 * When the value scale changes, uses a heuristic
		 * that is not translation invariant (see the implementation for details).
		 */
		kLegacyTransition,

		/**
		 * The current (i.e. last) value scale is always used for scale estimation.
		 * When using this mode, the filter is translation invariant, i.e.
		 * Filter(Data + Offset) = Filter(Data) + Offset.
		 */
		kForceCurrentState
	}
}
