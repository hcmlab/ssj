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
import polar.com.sdk.api.model.PolarOhrData;

/**
 * Created by Michael Dietz on 08.04.2021.
 */
public class PolarECGChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Integer> sampleRate = new Option<>("sampleRate", 130, Integer.class, "");

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

	Integer currentSample;

	public PolarECGChannel()
	{
		_name = "Polar_ECG";
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
		_listener.streamingFeatures.add(PolarBleApi.DeviceStreamingFeature.ECG);

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

		if (_listener.sampleRateECG > 0)
		{
			if (samplingRatio == -1)
			{
				// Get ratio between sensor sample rate and channel sample rate
				samplingRatio = _listener.sampleRateECG / (float) options.sampleRate.get();

				maxQueueSize = (int) (_listener.sampleRateECG * (_frame.options.bufferSize.get() + 120));
				minQueueSize = (int) (_listener.sampleRateECG * _frame.options.bufferSize.get());
			}

			// Get current sample values
			currentSample = _listener.ecgQueue.peek();

			// Check if queue is empty
			if (currentSample != null)
			{
				// Assign output values
				out[0] = currentSample;

				currentIndex += samplingRatio;

				// Remove unused samples (due to sample rate) from queue
				for (int i = (int) lastIndex; i < (int) currentIndex; i++)
				{
					_listener.ecgQueue.poll();
				}

				// Reset counters
				if (currentIndex >= _listener.sampleRateECG)
				{
					currentIndex = 0;

					// Discard old samples from queue if buffer gets too full
					int currentQueueSize = _listener.ecgQueue.size();

					// Log.d("Queue size: " + currentQueueSize);

					if (currentQueueSize > maxQueueSize)
					{
						for (int i = currentQueueSize; i > minQueueSize; i--)
						{
							_listener.ecgQueue.poll();
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
		return 1;
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
		stream_out.desc[0] = "ECG µV"; // in microvolts
	}
}
