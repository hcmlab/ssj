/*
 * GATTConnection.java
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.RequiresApi;
import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;

/**
 * Created by Michael Dietz on 16.07.2021.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GATTConnection extends BluetoothGattCallback
{
	public static final String CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

	public static Map<String, CharacteristicDetails> GATT_DETAILS = new HashMap<>();

	static {
		GATT_DETAILS.put("00002a19-0000-1000-8000-00805f9b34fb", new CharacteristicDetails("Battery Level", BluetoothGattCharacteristic.FORMAT_UINT8, 0, Cons.Type.INT));
		GATT_DETAILS.put("00002a6f-0000-1000-8000-00805f9b34fb", new CharacteristicDetails("Humidity", BluetoothGattCharacteristic.FORMAT_UINT16, -2, Cons.Type.FLOAT));
		GATT_DETAILS.put("00002a6e-0000-1000-8000-00805f9b34fb", new CharacteristicDetails("Temperature", BluetoothGattCharacteristic.FORMAT_SINT16, -2, Cons.Type.FLOAT));
	}

	static class CharacteristicDetails
	{
		String name;
		int readFormat;
		int decimalOffset = 0;
		Cons.Type outputFormat;

		public CharacteristicDetails(String name, int readFormat, int decimalOffset, Cons.Type outputFormat)
		{
			this.name = name;
			this.readFormat = readFormat;
			this.decimalOffset = decimalOffset;
			this.outputFormat = outputFormat;
		}
	}

	enum GATTConnectionStatus {
		CONNECTING,
		CONNECTED,
		DISCONNECTED;
	}

	private final Lock lock = new ReentrantLock(true);

	BluetoothAdapter bluetoothAdapter = null;
	BluetoothGatt bluetoothGatt = null;
	String previousAddress = null;
	GATTConnectionStatus connectionStatus;

	Map<UUID, BluetoothGattCharacteristic> registeredCharacteristics = new HashMap<>();
	Map<UUID, Object> characteristicValue = new HashMap<>();
	Queue<Request> requests = new ArrayDeque<>();

	public GATTConnection()
	{
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null)
		{
			Log.e("Device does not support Bluetooth");
			return;
		}

		if (!bluetoothAdapter.isEnabled())
		{
			Log.e("Bluetooth not enabled");
			return;
		}

		registeredCharacteristics.clear();
		characteristicValue.clear();

		connectionStatus = GATTConnectionStatus.DISCONNECTED;
	}

	public boolean connect(final String macAddress)
	{
		if (!BluetoothAdapter.checkBluetoothAddress(macAddress))
		{
			Log.e("Invalid MAC address: " + macAddress);
			return false;
		}

		if (previousAddress != null && previousAddress.equalsIgnoreCase(macAddress) && bluetoothGatt != null)
		{
			Log.i("Trying to re-use existing gatt connection");

			if (bluetoothGatt.connect())
			{
				connectionStatus = GATTConnectionStatus.CONNECTING;
				return true;
			}
			else
			{
				return false;
			}
		}

		final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

		if (device == null)
		{
			Log.w("Device not found.  Unable to connect.");
			return false;
		}

		Log.i("Device name: " + device.getName());
		Log.i("Trying to create a new connection");

		bluetoothGatt = device.connectGatt(SSJApplication.getAppContext(), false, this);
		previousAddress = macAddress;

		connectionStatus = GATTConnectionStatus.CONNECTING;

		return true;
	}

	public void disconnect()
	{
		if (bluetoothGatt != null)
		{
			for (BluetoothGattCharacteristic characteristic : registeredCharacteristics.values())
			{
				if (characteristic != null)
				{
					setCharacteristicNotification(characteristic, false);
				}
			}

			bluetoothGatt.disconnect();
		}
		else
		{
			Log.w("Bluetooth adapter not initialized");
		}
	}

	public void close()
	{
		if (bluetoothGatt != null)
		{
			bluetoothGatt.close();
		}
	}

	public boolean isConnected()
	{
		return connectionStatus == GATTConnectionStatus.CONNECTED;
	}

	public void registerCharacteristic(UUID uuid)
	{
		registeredCharacteristics.put(uuid, null);
		characteristicValue.put(uuid, null);
	}

	/**
	 * Reads a bt characteristic, result is reported in onCharacteristicRead() callback
	 * @param characteristic characteristic to read from
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic)
	{
		if (bluetoothGatt != null)
		{
			bluetoothGatt.readCharacteristic(characteristic);
		}
	}

	public synchronized void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled)
	{
		if (bluetoothGatt != null)
		{
			bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

			// Client Characteristic Configuration Descriptor
			final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID));

			if (enabled)
			{
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			}
			else
			{
				descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			}
			bluetoothGatt.writeDescriptor(descriptor);
		}
	}

	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		if (newState == BluetoothProfile.STATE_CONNECTED)
		{
			Log.i("Connected to GATT server");
			connectionStatus = GATTConnectionStatus.CONNECTED;

			bluetoothGatt.discoverServices();
		}
		else if (newState == BluetoothProfile.STATE_DISCONNECTED)
		{
			Log.i("Disconnected from GATT server");
			connectionStatus = GATTConnectionStatus.DISCONNECTED;
		}
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status)
	{
		if (status == BluetoothGatt.GATT_SUCCESS)
		{
			// services discovered
			Log.i("Services discovered!");

			for (BluetoothGattService service : bluetoothGatt.getServices())
			{
				for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
				{
					final UUID uuid = characteristic.getUuid();
					if (registeredCharacteristics.containsKey(uuid))
					{
						registeredCharacteristics.put(uuid, characteristic);
						requests.add(Request.newEnableNotificationsRequest(uuid));
					}
				}
			}

			nextRequest();
		}
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		if (status == BluetoothGatt.GATT_SUCCESS)
		{
			Log.i("Characteristic read: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0));
		}
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		final UUID uuid = characteristic.getUuid();
		final CharacteristicDetails characteristicDetails = GATT_DETAILS.get(uuid.toString());
		Object value = null;

		if (characteristicDetails != null)
		{
			if (characteristicDetails.readFormat == BluetoothGattCharacteristic.FORMAT_UINT8
			|| characteristicDetails.readFormat == BluetoothGattCharacteristic.FORMAT_UINT16
			|| characteristicDetails.readFormat == BluetoothGattCharacteristic.FORMAT_SINT16)
			{
				value = characteristic.getIntValue(characteristicDetails.readFormat, 0) * ((float) Math.pow(10, characteristicDetails.decimalOffset));
			}
		}
		else
		{
			value = characteristic.getValue();
		}

		// Log.i(characteristicDetails.name + ": " + value + " " + System.currentTimeMillis());

		characteristicValue.put(uuid, value);
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		Log.i("Writing characteristic: " + characteristic.getUuid());
	}

	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
		// Log.i("Writing descriptor: " + descriptor.getCharacteristic().getUuid());

		nextRequest();
	}

	public void nextRequest()
	{
		Request request = requests.poll();

		if (request != null)
		{
			switch (request.type)
			{
				case ENABLE_NOTIFICATIONS:
					setCharacteristicNotification(registeredCharacteristics.get(request.uuid), true);
					break;
				case DISABLE_NOTIFICATIONS:
					setCharacteristicNotification(registeredCharacteristics.get(request.uuid), false);
					break;
			}
		}
	}

	protected static final class Request
	{
		private enum Type
		{
			ENABLE_NOTIFICATIONS,
			DISABLE_NOTIFICATIONS
		}

		private final Type type;
		private final UUID uuid;

		private Request(final Type type, final UUID uuid)
		{
			this.type = type;
			this.uuid = uuid;
		}

		public static Request newEnableNotificationsRequest(final UUID uuid)
		{
			return new Request(Type.ENABLE_NOTIFICATIONS, uuid);
		}

		public static Request newDisableNotificationsRequest(final UUID uuid)
		{
			return new Request(Type.DISABLE_NOTIFICATIONS, uuid);
		}
	}
}
