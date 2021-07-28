/*
 * GATTChannel.java
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

package hcm.ssj.ioput;

import android.os.Build;

import java.util.UUID;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 16.07.2021.
 */
public class GATTChannel extends SensorChannel
{
	public enum SupportedUUIDs
	{
		BATTERY_LEVEL("00002a19-0000-1000-8000-00805f9b34fb"),
		HUMIDITY("00002a6f-0000-1000-8000-00805f9b34fb"),
		TEMPERATURE("00002a6e-0000-1000-8000-00805f9b34fb");

		UUID uuid;

		SupportedUUIDs(String uuid)
		{
			this.uuid = UUID.fromString(uuid);
		}
	}

	public class Options extends OptionList
	{
		public final Option<Float> sampleRate = new Option<>("sampleRate", 1.0f, Float.class, "samples per second");
		public final Option<String> customUUID = new Option<>("customUUID", "", String.class, "manual UUID, overwrites supported UUID field");
		public final Option<String> customName = new Option<>("customName", "", String.class, "data channel name");
		public final Option<SupportedUUIDs> supportedUUID = new Option<>("supportedUUID", SupportedUUIDs.BATTERY_LEVEL, SupportedUUIDs.class, "");

		private Options() {
			addOptions();
		}
	}
	public final Options options = new Options();

	GATTConnection _connection;

	UUID targetUUID;

	public GATTChannel()
	{
		_name = "GATTChannel";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void enter(Stream stream_out) throws SSJFatalException
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			_connection = ((GATTReader) _sensor).connection;

			updateTargetUUID();

			_connection.registerCharacteristic(targetUUID);
		}
	}

	@Override
	protected boolean process(Stream stream_out) throws SSJFatalException
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			Float value = (Float) _connection.characteristicValue.get(targetUUID);

			if (value != null)
			{
				float[] out = stream_out.ptrF();
				out[0] = value;

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

	private void updateTargetUUID()
	{
		String customUUID = options.customUUID.get();

		if (customUUID != null && !customUUID.isEmpty())
		{
			targetUUID = UUID.fromString(customUUID);
		}
		else
		{
			targetUUID = options.supportedUUID.get().uuid;
		}
	}

	@Override
	protected void describeOutput(Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];
		stream_out.desc[0] = "GATT Value";

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			updateTargetUUID();

			if (targetUUID != null)
			{
				GATTConnection.CharacteristicDetails details = GATTConnection.GATT_DETAILS.get(targetUUID.toString());

				if (details != null)
				{
					stream_out.desc[0] = details.name;
				}
				else
				{
					stream_out.desc[0] = options.customName.get();
				}
			}
		}

	}
}
