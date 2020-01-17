/*
 * PulseChannel.java
 * Copyright (c) 2020
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

package hcm.ssj.bitalino;

import hcm.ssj.core.Cons;
import hcm.ssj.core.LimitedSizeQueue;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import info.plux.pluxapi.bitalino.BITalinoFrame;

/**
 * Created by Michael Dietz on 07.01.2020.
 */
public class PulseChannel extends SensorChannel
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
	{
		public final Option<Integer> channel = new Option<>("Channel", 0, Integer.class, "channel id (between 0 and 5)");
		public final Option<Integer> beatThreshold = new Option<>("Beat threshold", 550, Integer.class, "Signal threshold for heart beat");
		public final Option<Boolean> outputRaw = new Option<>("Output Raw", true, Boolean.class, "Output raw signal value");
		public final Option<Boolean> outputBpm = new Option<>("Output BPM", false, Boolean.class, "Output calculated bpm value");
		public final Option<Boolean> outputIbi = new Option<>("Output IBI", false, Boolean.class, "Output calculated ibi value");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	PulseListener listener;

	public PulseChannel()
	{
		_name = "Bitalino_PulseChannel";

		this.listener = new PulseListener();
	}

	@Override
	protected void init() throws SSJException
	{
		Bitalino sensor = ((Bitalino)_sensor);

		sensor.addChannel(options.channel.get());
		sensor.listener = this.listener;
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		if (!listener.isConnected())
		{
			return false;
		}

		float[] out = stream_out.ptrF();
		int dim = 0;

		if (options.outputRaw.get())
		{
			out[dim++] = listener.rawSignal;
		}
		if (options.outputBpm.get())
		{
			out[dim++] = listener.bpm;
		}
		if (options.outputIbi.get())
		{
			out[dim] = listener.ibi;
		}

		return true;
	}

	@Override
	protected double getSampleRate()
	{
		return ((Bitalino)_sensor).options.sr.get();
	}

	@Override
	protected int getSampleDimension()
	{
		int dim = 0;
		if (options.outputRaw.get())
		{
			dim += 1;
		}
		if (options.outputBpm.get())
		{
			dim += 1;
		}
		if (options.outputIbi.get())
		{
			dim += 1;
		}

		return dim;
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

		int dim = 0;

		if (options.outputRaw.get())
		{
			stream_out.desc[dim++] = "raw pulse signal";
		}
		if (options.outputBpm.get())
		{
			stream_out.desc[dim++] = "bpm";
		}
		if (options.outputIbi.get())
		{
			stream_out.desc[dim] = "ibi";
		}
	}


	class PulseListener extends BitalinoListener
	{
		private static final int QUEUE_SIZE = 10;

		private long lastDataTime;
		private long lastBeatTime;
		private int max;
		private int min;
		private int amplitude;
		private int threshold;
		private boolean isPulse;
		private boolean firstBeat;
		private boolean secondBeat;
		private LimitedSizeQueue<Long> lastIbis;

		int rawSignal;
		long ibi;
		float avgIbi;
		float bpm;

		public PulseListener()
		{
			lastIbis = new LimitedSizeQueue<>(QUEUE_SIZE);

			reset();
		}

		public void reset()
		{
			super.reset();

			lastDataTime = 0;
			lastBeatTime = 0;

			// 750ms per beat = 80 Beats Per Minute (BPM)
			ibi = 750;

			avgIbi = 0;

			rawSignal = 0;

			// Max at 1/2 the input range of 0..1023
			max = 512;

			// Min at 1/2 the input range
			min = 512;

			threshold = options.beatThreshold.get();

			// Beat amplitude 1/10 of input range.
			amplitude = 100;

			bpm = 0;

			isPulse = false;
			firstBeat = true;
			secondBeat = false;

			if (lastIbis != null)
			{
				lastIbis.clear();
			}
		}

		@Override
		public void onBITalinoDataAvailable(BITalinoFrame biTalinoFrame)
		{
			dataReceived();

			lastDataTime = System.currentTimeMillis();

			rawSignal = biTalinoFrame.getAnalog(options.channel.get());

			// Monitor the time since the last beat to avoid noise
			long timeSinceLastBeat = lastDataTime - lastBeatTime;

			// Find min of the pulse wave and avoid noise by waiting 3/5 of last IBI
			if (rawSignal < threshold && timeSinceLastBeat > ibi / 5.0 * 3.0)
			{
				if (rawSignal < min)
				{
					min = rawSignal;
				}
			}

			// Find highest point in pulse wave (threshold condition helps avoid noise)
			if (rawSignal > threshold && rawSignal > max)
			{
				max = rawSignal;
			}

			// Avoid high frequency noise
			if (timeSinceLastBeat > 250)
			{
				// Pulse detected
				if (rawSignal > threshold && !isPulse && timeSinceLastBeat > ibi / 5.0 * 3.0)
				{
					// Set the Pulse flag when we think there is a pulse
					isPulse = true;

					// Measure time between beats in ms
					ibi = lastDataTime - lastBeatTime;

					// Keep track of time for next pulse
					lastBeatTime = lastDataTime;

					// Fill the queue to get a realistic BPM at startup
					if (secondBeat)
					{
						secondBeat = false;

						for (int i = 0; i < QUEUE_SIZE; i++)
						{
							lastIbis.add(ibi);
						}
					}

					// IBI value is unreliable so discard it
					if (firstBeat)
					{
						firstBeat = false;
						secondBeat = true;

						return;
					}

					// Add the latest IBI to the rate array
					lastIbis.add(ibi);

					// Average the last 10 IBI values
					avgIbi = 0;

					for (int i = 0; i < QUEUE_SIZE; i++)
					{
						avgIbi += lastIbis.get(i);
					}

					avgIbi = avgIbi / (float) QUEUE_SIZE;

					// Calculate beats per minute
					bpm = 60000 / avgIbi;
				}
			}

			// When the values are going down, the beat is over
			if (rawSignal < threshold && isPulse)
			{
				// Reset the Pulse flag
				isPulse = false;

				// Get amplitude of the pulse wave
				amplitude = max - min;

				// Set threshold at 50% of amplitude
				threshold = (int) (amplitude / 2.0f + min);

				// Reset min and max
				min = threshold;
				max = threshold;
			}

			// If 2.5 seconds go by without a beat
			if (timeSinceLastBeat > 2500)
			{
				reset();

				lastDataTime = System.currentTimeMillis();
				lastBeatTime = lastDataTime;
			}
		}
	}
}
