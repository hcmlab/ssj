/*
 * PolarChannel.java
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

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.model.PolarAccelerometerData;

/**
 * Created by Michael Dietz on 08.04.2021.
 */
public class PolarACCChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Integer> sampleRate = new Option<>("sampleRate", 50, Integer.class, "");

		private Options() {
			addOptions();
		}
	}
	public final Options options = new Options();

	PolarListener _listener;

	float samplingRatio;
	float lastIndex;
	float currentIndex;

	int maxQueueSize;
	int minQueueSize;

	PolarAccelerometerData.PolarAccelerometerDataSample currentSample;

	public PolarACCChannel()
	{
		_name = "Polar_ACC";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		_listener = ((Polar) _sensor).listener;
		_listener.streamingFeatures.add(PolarBleApi.DeviceStreamingFeature.ACC);

		samplingRatio = -1;

		lastIndex = 0;
		currentIndex = 0;

		maxQueueSize = -1;
		minQueueSize = -1;
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		float[] out = stream_out.ptrF();

		if (_listener.sampleRateACC > 0)
		{
			if (samplingRatio == -1)
			{
				// Get ratio between sensor sample rate and channel sample rate
				samplingRatio = _listener.sampleRateACC / (float) options.sampleRate.get();

				maxQueueSize = (int) (_listener.sampleRateACC * (_frame.options.bufferSize.get() + 120));
				minQueueSize = (int) (_listener.sampleRateACC * _frame.options.bufferSize.get());
			}

			// Get current sample values
			currentSample = _listener.accQueue.peek();

			// Check if queue is empty
			if (currentSample != null)
			{
				// Assign output values
				out[0] = currentSample.x;
				out[1] = currentSample.y;
				out[2] = currentSample.z;

				currentIndex += samplingRatio;

				// Remove unused samples (due to sample rate) from queue
				for (int i = (int) lastIndex; i < (int) currentIndex; i++)
				{
					_listener.accQueue.poll();
				}

				// Reset counters
				if (currentIndex >= _listener.sampleRateACC)
				{
					currentIndex = 0;

					// Discard old samples from queue if buffer gets too full
					int currentQueueSize = _listener.accQueue.size();

					// Log.d("Queue size: " + currentQueueSize);

					if (currentQueueSize > maxQueueSize)
					{
						for (int i = currentQueueSize; i > minQueueSize; i--)
						{
							_listener.accQueue.poll();
						}
					}
				}

				lastIndex = currentIndex;

				return true;
			}
		}

		return false;
	}

	@Override
	protected double getSampleRate()
	{
		return options.sampleRate.get();
	}

	@Override
	protected int getSampleDimension()
	{
		return 3;
	}

	@Override
	protected Cons.Type getSampleType()
	{
		return Cons.Type.FLOAT;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];
		stream_out.desc[0] = "ACC X";
		stream_out.desc[1] = "ACC Y";
		stream_out.desc[2] = "ACC Z";

		/*
		Polar H10:
		Accelerometer data with sample rates of 25Hz, 50Hz, 100Hz and 200Hz and range of 2G, 4G and 8G. Axis specific acceleration data in mG.

		Polar OH1 / Verity Sense:
		Accelerometer data with sample rate of 52Hz and range of 8G. Axis specific acceleration data in mG.
		 */
	}
}
