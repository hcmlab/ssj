/*
 * AngelSensorListener.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

package hcm.ssj.angelsensor;

import android.content.Context;
import android.os.Handler;

import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChAccelerationWaveform;
import com.angel.sdk.ChOpticalWaveform;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;
import com.angel.sdk.SrvWaveformSignal;

import java.util.Vector;

import hcm.ssj.core.SSJApplication;

/**
 * Created by simon on 17.06.16.
 */
public class AngelSensorListener
{
	private BleDevice mBleDevice;

	private Handler         mHandler;
	private Vector<Integer> opticalGreenLED;
	private Vector<Integer> opticalBlueLED;


	//private Vector<Tupel<float, float, float>> acceleration;

	public AngelSensorListener()
	{
		reset();
	}

	public void reset()
	{
		if (opticalGreenLED == null)
		{
			opticalGreenLED = new Vector<Integer>();

		}
		else
		{
			opticalGreenLED.removeAllElements();
		}
		if (opticalBlueLED == null)
		{
			opticalBlueLED = new Vector<Integer>();

		}
		else
		{
			opticalBlueLED.removeAllElements();
		}

	}

	private final BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue> mAccelerationWaveformListener = new BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue>()
	{
		@Override
		public void onValueReady(ChAccelerationWaveform.AccelerationWaveformValue accelerationWaveformValue)
		{
			if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null)
			{
				for (Integer item : accelerationWaveformValue.wave)
				{
					//mAccelerationWaveformView.addValue(item);
					//vector push
					//provide()

				}
			}

		}
	};


	public void connect(String deviceAddress)
	{
		// A device has been chosen from the list. Create an instance of BleDevice,
		// populate it with interesting services and then connect

		if (mBleDevice != null)
		{
			mBleDevice.disconnect();
		}
		Context context = SSJApplication.getAppContext();
		mHandler = new Handler(context.getMainLooper());
		mBleDevice = new BleDevice(context, mDeviceLifecycleCallback, mHandler);


		try
		{
			mBleDevice.registerServiceClass(SrvWaveformSignal.class);
			mBleDevice.registerServiceClass(SrvHeartRate.class);
			mBleDevice.registerServiceClass(SrvHealthThermometer.class);
			mBleDevice.registerServiceClass(SrvBattery.class);
			mBleDevice.registerServiceClass(SrvActivityMonitoring.class);

		}
		catch (NoSuchMethodException e)
		{
			throw new AssertionError();
		}
		catch (IllegalAccessException e)
		{
			throw new AssertionError();
		}
		catch (InstantiationException e)
		{
			throw new AssertionError();
		}

		mBleDevice.connect(deviceAddress);


	}


	private final BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue> mOpticalWaveformListener = new BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue>()
	{
		@Override
		public void onValueReady(ChOpticalWaveform.OpticalWaveformValue opticalWaveformValue)
		{

			if (opticalWaveformValue != null && opticalWaveformValue.wave != null)
			{
				for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave)
				{
					opticalGreenLED.add(item.green);
					opticalBlueLED.add(item.blue);
					//mBlueOpticalWaveformView.addValue(item.blue);

				}
			}
		}
	};

	private final BleDevice.LifecycleCallback mDeviceLifecycleCallback = new BleDevice.LifecycleCallback()
	{
		@Override
		public void onBluetoothServicesDiscovered(BleDevice bleDevice)
		{
			bleDevice.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
			bleDevice.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
		}

		@Override
		public void onBluetoothDeviceDisconnected()
		{

		}

		@Override
		public void onReadRemoteRssi(int i)
		{

		}
	};

	public int getBvp()
	{
		if (opticalGreenLED.size() > 0)
		{
			int tmp = opticalGreenLED.lastElement();
			if (opticalGreenLED.size() > 1)
			{
				opticalGreenLED.removeElementAt(opticalGreenLED.size() - 1);
			}
			return tmp;

		}
		else
		{
			return 0;
		}
	}

};
