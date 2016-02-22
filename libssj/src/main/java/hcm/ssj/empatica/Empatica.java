/*
 * Empatica.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.empatica;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class Empatica extends Sensor implements EmpaStatusDelegate
{
	public class Options
	{
		public String apiKey     = "e07128d944fe4b7081912cfe9042b3d6";
	}

	public Options options = new Options();

	protected EmpaDeviceManager     deviceManager;
	protected EmpaticaListener      listener;
	protected boolean               empaticaInitialized;

	public Empatica()
	{
		_name = "SSJ_sensor_Empatica";
		empaticaInitialized = false;
	}

	@Override
	public void connect()
	{
		// Create data listener
		listener = new EmpaticaListener();

		// Empatica device manager must be initialized in the main ui thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable()
		{
			public void run()
			{
				// Create device manager
				deviceManager = new EmpaDeviceManager(SSJApplication.getAppContext(), listener, Empatica.this);

				// Register the device manager using your API key. You need to have Internet access at this point.
				deviceManager.authenticateWithAPIKey(options.apiKey);
			}
		}, 1);


		long time = SystemClock.elapsedRealtime();
		while (!_terminate && !listener.receivedData && SystemClock.elapsedRealtime() - time < Cons.WAIT_SENSOR_CONNECT)
		{
			try {
				Thread.sleep(Cons.SLEEP_ON_IDLE);
			} catch (InterruptedException e) {}
		}

		if(!listener.receivedData)
			throw new RuntimeException("device not connected");
	}

	@Override
	public void disconnect()
	{
		deviceManager.disconnect();
	}

	@Override
	public void didUpdateStatus(EmpaStatus empaStatus)
	{
		Log.i(_name, "Empatica status: " + empaStatus);

		switch (empaStatus)
		{
			case READY:
				// Start scanning
				deviceManager.startScanning();
				break;

			case DISCONNECTED:
				listener.reset();
				break;

			case CONNECTING:
				break;
			case CONNECTED:
				break;
			case DISCONNECTING:
				break;
			case DISCOVERING:
				break;
		}
	}

	@Override
	public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus, EmpaSensorType empaSensorType)
	{
		// Sensor status update
	}

	@Override
	public void didDiscoverDevice(BluetoothDevice bluetoothDevice, int rssi, boolean allowed)
	{
		// Stop scanning. The first allowed device will do.
		if (allowed)
		{
			deviceManager.stopScanning();

			try
			{
				// Connect to the device
				Log.i(_name, "Connecting to device: " + bluetoothDevice.getName() + "(MAC: " + bluetoothDevice.getAddress() + ")");

				deviceManager.connectDevice(bluetoothDevice);
				// Depending on your configuration profile, you might be unable to connect to a device.
				// This should happen only if you try to connect when allowed == false.

				empaticaInitialized = true;
			}
			catch (ConnectionNotAllowedException e)
			{
				Log.e(_name, "Can't connect to device: " + bluetoothDevice.getName() + "(MAC: " + bluetoothDevice.getAddress() + ")");
			}
		}
	}

	@Override
	public void didRequestEnableBluetooth()
	{
		Log.e(_name, "Bluetooth not enabled!");
	}
}
