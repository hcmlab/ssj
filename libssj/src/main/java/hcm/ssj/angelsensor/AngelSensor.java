/*
 * AngelSensor.java
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import com.angel.sdk.BleScanner;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;

public class AngelSensor extends Sensor
{
	private Handler handler;

	protected boolean             angelInitialized;
	protected AngelSensorListener listener;

	BleScanner.ScanCallback mScanCallback = new BleScanner.ScanCallback()
	{
		@Override
		public void onBluetoothDeviceFound(BluetoothDevice device)
		{
			if (device.getName() != null)
			{
				Log.i("Bluetooth LE device found: " + device.getName());
				//mBleScanner.stopScan();
				if (device.getName() != null && device.getName().startsWith("Angel"))
				{

					bluetoothDevice = device;
					//mBleScanner.stop();
					mBleScanner.stopScan();

					listener.connect(device.getAddress());
					Log.i("connected to device " + device.getName());
				}
			}
		}
	};

	public AngelSensor()
	{
		_name = "SSJ_sensor_AngelSensor";
		angelInitialized = false;
	}

	@Override
	public boolean connect()
	{
		listener = new AngelSensorListener();

		try
		{
			if (mBleScanner == null)
			{
				//bluetoothAdapter = BleUtils.getBluetoothAdapter(ctx);
				mBleScanner = new BleScanner(SSJApplication.getAppContext(), mScanCallback);
			}
		}
		catch (Exception e)
		{
			Log.e("Exception:", e);
		}

		mBleScanner.startScan();
		//mBleScanner.setScanPeriod(1000);

		//mBleScanner.start();


		return true;
	}

	@Override
	public void disconnect()
	{

	}

	public void didDiscoverDevice(BluetoothDevice bluetoothDevice, int rssi, boolean allowed)
	{

	}

	private BleScanner       mBleScanner;
	private BluetoothAdapter bluetoothAdapter;
	BluetoothDevice bluetoothDevice;
}
