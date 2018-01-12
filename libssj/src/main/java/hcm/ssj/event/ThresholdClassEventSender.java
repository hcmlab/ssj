/*
 * ThresholdClassEventSender.java
 * Copyright (c) 2018
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

package hcm.ssj.event;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

public class ThresholdClassEventSender extends Consumer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
	{
		public final Option<String> sender = new Option<>("sender", null, String.class, "name of event sender");
		public final Option<String[]> classes = new Option<>("classes", new String[]{"low", "medium", "high"}, String[].class, "classes for threshold values (eventnames)");
		public final Option<float[]> thresholds = new Option<>("thresholds", new float[]{0f, 1f, 2f}, float[].class, "thresholds for input");
		public final Option<Float> minDiff = new Option<>("minDiff", 0.1f, Float.class, "minimum difference to previous value");
		public final Option<Boolean> mean = new Option<>("mean", false, Boolean.class, "classify based on mean value of entire frame");
		public final Option<Double> maxDur = new Option<>("maxDur", 2., Double.class, "maximum delay before continued events will be sent");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	private List<SimpleEntry<Float, String>> thresholdList;
	private float lastValue = Float.NEGATIVE_INFINITY;
	private Map.Entry<Float, String> lastClass = null;

	private int samplesMaxDur;
	private double lastTriggerTime;

	public ThresholdClassEventSender()
	{
		_name = "ThresholdClassEventSender";
		options.sender.set(_name);
	}

	@Override
	public void enter(Stream[] stream_in) throws SSJFatalException
	{
		if (stream_in[0].dim != 1)
		{
			throw new SSJFatalException("Dimension != 1 unsupported");
		}

		makeThresholdList();

		samplesMaxDur = (int) (options.maxDur.get() * stream_in[0].sr);
	}

	private void makeThresholdList() throws SSJFatalException
	{
		String[] classes = options.classes.get();
		float[] thresholds = options.thresholds.get();

		if (classes == null || thresholds == null)
		{
			throw new SSJFatalException("classes and thresholds not correctly set");
		}

		if (classes.length != thresholds.length)
		{
			throw new SSJFatalException("number of classes do not match number of thresholds");
		}

		thresholdList = new ArrayList<>();
		for (int i = 0; i < classes.length; ++i)
		{
			thresholdList.add(new SimpleEntry<Float, String>(thresholds[i], classes[i]));
		}
		Collections.sort(thresholdList, new ThresholdListComparator());
	}

	@Override
	protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
	{
		makeThresholdList();

		double time = stream_in[0].time;
		double timeStep = 1 / stream_in[0].sr;
		float sum = 0;

		for (int i = 0; i < stream_in[0].num; i++)
		{
			float value = 0;
			switch (stream_in[0].type)
			{
				case BYTE:
					value = (float) stream_in[0].ptrB()[i];
					break;
				case FLOAT:
					value = stream_in[0].ptrF()[i];
					break;
				case DOUBLE:
					value = (float) stream_in[0].ptrD()[i];
					break;
				case SHORT:
					value = stream_in[0].ptrS()[i];
					break;
				case INT:
					value = (float) stream_in[0].ptrI()[i];
					break;
				case LONG:
					value = stream_in[0].ptrL()[i];
					break;
				default:
					Log.e("unsupported input type");
					break;
			}

			if (options.mean.get())
			{
				sum += value;
			}
			else
			{
				processValue(value, time, stream_in[0].sr, 1);
			}
			time += timeStep;
		}
		if (options.mean.get())
		{
			processValue(sum / stream_in[0].num, time, stream_in[0].sr, stream_in[0].num);
		}
	}

	private void processValue(float value, double time, double sampleRate, int numberOfSamples)
	{
		SimpleEntry<Float, String> newClass = classify(value);
		samplesMaxDur -= numberOfSamples;
		if (newClass == null)
		{
			return;
		}
		if (isEqualLastClass(newClass))
		{
			if (samplesMaxDur <= 0)
			{
				lastValue = value;
				sendEvent(newClass, lastTriggerTime, time - lastTriggerTime, Event.State.CONTINUED);
				samplesMaxDur = (int) (options.maxDur.get() * sampleRate);
			}
		}
		else if (valueDiffersEnoughFromLast(value))
		{
			if (lastClass != null)
			{
				sendEvent(lastClass, lastTriggerTime, time - lastTriggerTime, Event.State.COMPLETED);
			}
			sendEvent(newClass, time, 0, Event.State.CONTINUED);
			lastTriggerTime = time;
			lastValue = value;
			lastClass = newClass;
			samplesMaxDur = (int) (options.maxDur.get() * sampleRate);
		}
	}

	private boolean isEqualLastClass(SimpleEntry<Float, String> newClass)
	{
		return newClass.equals(lastClass);
	}

	private boolean valueDiffersEnoughFromLast(float value)
	{
		return Math.abs(value - lastValue) > options.minDiff.get();
	}

	private SimpleEntry<Float, String> classify(float value)
	{
		SimpleEntry<Float, String> foundClass = null;
		for (SimpleEntry<Float, String> thresholdClass : thresholdList)
		{
			float classValue = thresholdClass.getKey();
			if (value > classValue)
			{
				return thresholdClass;
			}
		}
		return foundClass;
	}

	private void sendEvent(Map.Entry<Float, String> thresholdClass, double time, double duration, Event.State state)
	{
		Event event = Event.create(Cons.Type.EMPTY);
		event.name = thresholdClass.getValue();
		event.sender = options.sender.get();
		event.time = Math.max(0, (int) (1000 * time + 0.5));
		event.dur = Math.max(0, (int) (1000 * duration + 0.5));
		event.state = state;
		_evchannel_out.pushEvent(event);
	}

	private class ThresholdListComparator implements Comparator<SimpleEntry<Float, String>>
	{
		@Override
		public int compare(SimpleEntry<Float, String> o1, SimpleEntry<Float, String> o2)
		{
			// Note: this comparator imposes orderings that are inconsistent with equals.
			// Order is descending.
			if (o1.getKey() > o2.getKey())
			{
				return -1;
			}
			if (o1.getKey() < o2.getKey())
			{
				return 1;
			}
			return 0;
		}
	}
}
