/*
 * HRChannel.java
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

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 11.06.2021.
 */
public class SamsungPPGChannel extends SensorChannel
{
	public static final float WATCH_SR = 10;

	public class Options extends OptionList
	{
		public final Option<Float> sampleRate = new Option<>("sampleRate", 10f, Float.class, "");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	SamsungAccessoryConsumer accessoryConsumer;

	float samplingRatio;
	float lastIndex;
	float currentIndex;

	int maxQueueSize;
	int minQueueSize;

	Float currentPPG;

	public SamsungPPGChannel()
	{
		_name = "Samsung_PPG";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		accessoryConsumer = ((SamsungWearable) _sensor).accessoryConsumer;
		accessoryConsumer.sendCommand(SamsungAccessoryConsumer.AccessoryCommand.SENSOR_PPG);

		accessoryConsumer.ppgQueue.clear();

		lastIndex = 0;
		currentIndex = 0;

		if (options.sampleRate.get() > WATCH_SR)
		{
			options.sampleRate.set(WATCH_SR);
		}

		// Get ratio between sensor sample rate and channel sample rate
		samplingRatio = WATCH_SR / options.sampleRate.get();

		maxQueueSize = (int) (WATCH_SR * _frame.options.bufferSize.get());
		minQueueSize = (int) (2 * samplingRatio);
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		float[] out = stream_out.ptrF();

		// Get current sample values
		currentPPG = accessoryConsumer.ppgQueue.peek();

		// Check if queue is empty
		if (currentPPG != null)
		{
			// Assign output values
			out[0] = currentPPG;

			currentIndex += samplingRatio;

			// Remove unused samples (due to sample rate) from queue
			for (int i = (int) lastIndex; i < (int) currentIndex; i++)
			{
				accessoryConsumer.ppgQueue.poll();
			}

			// Reset counters
			if (currentIndex >= WATCH_SR)
			{
				currentIndex = 0;

				// Discard old samples from queue if buffer gets too full
				int currentQueueSize = accessoryConsumer.ppgQueue.size();

				// Log.d("Queue size: " + currentQueueSize);

				if (currentQueueSize > maxQueueSize)
				{
					for (int i = currentQueueSize; i > minQueueSize; i--)
					{
						accessoryConsumer.ppgQueue.poll();
					}
				}
			}

			lastIndex = currentIndex;

			return true;
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
		stream_out.desc[0] = "PPG LED"; // Led
	}
}
