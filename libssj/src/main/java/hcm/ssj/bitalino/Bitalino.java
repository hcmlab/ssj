/*
 * Bitalino.java
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

package hcm.ssj.bitalino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;

import java.util.LinkedList;
import java.util.Set;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import info.plux.pluxapi.Communication;
import info.plux.pluxapi.bitalino.BITalinoCommunication;
import info.plux.pluxapi.bitalino.BITalinoCommunicationFactory;
import info.plux.pluxapi.bitalino.BITalinoException;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class Bitalino extends Sensor
{
	public class Options extends OptionList
	{
		public final Option<String> name = new Option<>("name", null, String.class, "device name");
		public final Option<String> address = new Option<>("address", null, String.class, "mac address of device, only used if name is left empty");
		public final Option<Communication> connectionType = new Option<>("connectionType", Communication.BTH, Communication.class, "type of connection");
		public final Option<Integer> sr = new Option<>("sr", 10, Integer.class, "sample rate, supported values: 1, 10, 100, 1000");

		private Options() {
			addOptions();
		}
	}

	public enum Channel
	{
		A1_EMG(0),
		A2_ECG(1),
		A3_EDA(2),
		A4_EEG(3),
		A5_ACC(4),
		A6_LUX(5),
		D1_LED(6),
		D2_BUZ(7);

		int id;
		Channel(int id) {this.id = id;}

		boolean isAnalogue() {return id <= 5;}
		int getDataPosition() { return isAnalogue() ? id : id - 6;}
	}

	public final Options options = new Options();

	protected BITalinoCommunication client;
	protected BitalinoListener listener;

	protected LinkedList<Channel> channels;

	void addChannel(Channel ch)
	{
		channels.add(ch);
	}

	void removeChannel(Channel ch)
	{
		channels.remove(ch);
	}

	public Bitalino()
	{
		_name = "Bitalino";

		channels = new LinkedList<>();
		listener = new BitalinoListener();
	}

	@Override
	protected boolean connect()
	{
		boolean connected = false;

		disconnect(); //clean up old connection
		listener.reset();

		Log.i("connecting to bitalino ...");

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		String address = null;

		if(options.address.get() != null && !options.address.get().equals(""))
		{
			if (!BluetoothAdapter.checkBluetoothAddress(options.address.get())) {
				Log.e("invalid MAC address: " + options.address.get());
				return false;
			}

			address = options.address.get();
		}
		else if(options.name.get() != null && !options.name.get().equals(""))
		{
			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			// If there are paired devices
			if (pairedDevices.size() > 0)
			{
				// Loop through paired devices
				for (BluetoothDevice d : pairedDevices)
				{
					if (d.getName().equalsIgnoreCase(options.name.get()))
					{
						address = d.getAddress();
					}
				}
			}
		}

		if (address != null)
		{
			try
			{
				client = new BITalinoCommunicationFactory().getCommunication(options.connectionType.get(), SSJApplication.getAppContext(), listener);
				connected = client.connect(address);

				Log.i("waiting for connection ...");

				//wait for connection
				long time = SystemClock.elapsedRealtime();
				while (!_terminate && !listener.hasReceivedData() && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000)
				{
					try {
						Thread.sleep(Cons.SLEEP_IN_LOOP);
					} catch (InterruptedException e) {}
				}

				int[] analogue_channels = new int[channels.size()];
				int i = 0;
				for(Channel ch : channels)
					if(ch.isAnalogue())
						analogue_channels[i++] = ch.getDataPosition();

				int sr = options.sr.get();
				if(sr > 100)
					sr = 1000;
				else if(sr > 10)
					sr = 100;
				else if(sr > 1)
					sr = 10;

				connected = client.start(analogue_channels, sr);

				Log.i("connected to bitalino " + address);
			}
			catch (BITalinoException e)
			{
				Log.e("Error connecting to device", e);
			}
		}

		return connected;
	}

	protected boolean checkConnection()
	{
		return listener.isConnected();
	}

	@Override
	protected void disconnect()
	{
		Log.d("Disconnecting from Bitalino");
		if (client != null)
		{
			try
			{
				client.stop();
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}

				client.disconnect();
			}
			catch (BITalinoException e)
			{
				Log.e("Error while disconnecting from device", e);
			}
		}

		client = null;
	}

	@Override
	public void clear()
	{
		channels.clear();
		super.clear();
	}
}
