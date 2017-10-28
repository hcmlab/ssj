/*
 * DynAccelerationChannel.java
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

package hcm.ssj.myo;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Dynamic Acceleration which has the gravity component removed
 *
 * Created by Johnny on 01.04.2015.
 */
public class DynAccelerationChannel extends SensorChannel
{
	public class Options extends OptionList
	{
		public final Option<Float> gravity = new Option<>("gravity", 9.814f, Float.class, "Frankfurt");
		public final Option<Boolean> meterPerSecond = new Option<>("meterPerSecond", false, Boolean.class, "");
		public final Option<Boolean> absolute = new Option<>("absolute", false, Boolean.class, "do measurements relative to the global coordinate system");
		public final Option<Integer> sampleRate = new Option<>("sampleRate", 50, Integer.class, "");

		/**
		 *
		 */
		private Options() {
			addOptions();
		}
	}
	public final Options options = new Options();

	protected MyoListener _listener;

	float[] _acc     = new float[3];
	float[] _ori     = new float[4];
	float[] _gravity = new float[3];

	public DynAccelerationChannel()
	{
		_name = "Myo_DynAcceleration";
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		_listener = ((Myo)_sensor).listener;

		if (stream_out.num != 1)
		{
			Log.w("unsupported stream format. sample number = " + stream_out.num);
		}
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		float[] out = stream_out.ptrF();

		_acc[0] = _listener.accelerationX;
		_acc[1] = _listener.accelerationY;
		_acc[2] = _listener.accelerationZ;

		_ori[0] = (float)_listener.orientationW;
		_ori[1] = (float)_listener.orientationX;
		_ori[2] = (float)_listener.orientationY;
		_ori[3] = (float)_listener.orientationZ;

		/**
		 * Determine gravity direction
		 * Code taken from http://www.varesano.net/blog/fabio/simple-gravity-compensation-9-dom-imus
		 * Author: Fabio Varesano
		 */
		_gravity[0] = 2 * (_ori[1] * _ori[3] - _ori[0] * _ori[2]);
		_gravity[1] = 2 * (_ori[0] * _ori[1] + _ori[2] * _ori[3]);
		_gravity[2] = _ori[0] * _ori[0] - _ori[1] * _ori[1] - _ori[2] * _ori[2] + _ori[3] * _ori[3];

		for (int k = 0; k < 3; k++)
		{
			out[k] = _acc[k] - _gravity[k];
		}

		if (options.absolute.get())
		{
			//TODO, need to find proper mat/vec/quat library in java
			Log.w("not supported yet");
			//convert to global coordinate system
//                    Quaternion q(xq.x(), xq.y(), xq.z(), xq.w());
//                    Matrix mat(q);
//                    dynacc = mat * dynacc;
		}

		if (options.meterPerSecond.get())
		{
			for (int k = 0; k < 3; k++)
			{
				out[k] *= options.gravity.get();
			}
		}

		return true;
	}

	@Override
	public void flush(Stream stream_out) throws SSJFatalException
	{
	}

	@Override
	public double getSampleRate()
	{
		return options.sampleRate.get();
	}

	@Override
	public int getSampleDimension()
	{
		return 3;
	}

	@Override
	public int getSampleBytes()
	{
		return 4;
	}

	@Override
	public Cons.Type getSampleType()
	{
		return Cons.Type.FLOAT;
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		stream_out.desc[0] = "AccX";
		stream_out.desc[1] = "AccY";
		stream_out.desc[2] = "AccZ";
	}
}
