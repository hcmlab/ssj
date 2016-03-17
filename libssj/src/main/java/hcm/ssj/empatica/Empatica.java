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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

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
		public String apiKey = null;
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
		if(options.apiKey == null || options.apiKey.length() == 0)
			throw new RuntimeException("invalid apiKey - you need to set the apiKey in the sensor options");

		// Create data listener
		listener = new EmpaticaListener();

		//pre-validate empatica to avoid the certificate being bound to one app
		getCertificate();
		applyCertificate();

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

	private void applyCertificate()
	{
		try
		{
			copyFile(new File(Environment.getExternalStorageDirectory(), "empatica/profile"),
					 new File(SSJApplication.getAppContext().getFilesDir(), "profile"));

			copyFile(new File(Environment.getExternalStorageDirectory(), "empatica/signature"),
					 new File(SSJApplication.getAppContext().getFilesDir(), "signature"));
		}
		catch (IOException e)
		{
			throw new RuntimeException("cannot find/copy empatica certificates", e);
		}
	}

	private void getCertificate()
	{
		try {
			HashMap p = new HashMap();
			p.put("api_key", options.apiKey);
			p.put("api_version", "AND_1.3");
			String json = (new GsonBuilder()).create().toJson(p, Map.class);

			//execute json
			HttpPost e = new HttpPost("https://www.empatica.com/connect/empalink/api_login.php");
			e.setEntity(new StringEntity(json));
			e.setHeader("Accept", "application/json");
			e.setHeader("Content-type", "application/json");
			BasicResponseHandler handler = new BasicResponseHandler();
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String response = (String)httpClient.execute(e, handler);

			//check status
			JsonElement jelement = (new JsonParser()).parse(response);
			JsonObject jobject = jelement.getAsJsonObject();
			String status = jobject.get("status").getAsString();
			if(!status.equals("ok"))
				throw new IOException("status check failed");

			//save certificate
			if(jobject.has("empaconf") && jobject.has("empasign")) {
				Log.d("EmpaDeviceManager", "Empaconf & Empasign ok");
				String empaconf = jobject.get("empaconf").getAsString();
				String empasign = jobject.get("empasign").getAsString();
				byte[] empaconfBytes = Base64.decode(empaconf, 0);
				byte[] empasignBytes = Base64.decode(empasign, 0);

				saveFile("profile", empaconfBytes);
				saveFile("signature", empasignBytes);
			} else {
				throw new IOException("Empaconf and Empasign missing from http response");
			}
		}
		catch (IOException e)
		{
			Log.w(_name, "unable to connect to empatica server - empatica needs to connect to the server once a month to validate", e);
		}
	}

	private void saveFile(String name, byte[] data)
	{
		File dir = new File(Environment.getExternalStorageDirectory(), "empatica");
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		File file = new File(dir, name);

		try	{
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.close();
		} catch(IOException e)
		{
			Log.w(_name, "unable to save empatica certificate", e);
		}
	}

	public void copyFile(File src, File dst) throws IOException {
		FileInputStream inStream = new FileInputStream(src);
		FileOutputStream outStream = new FileOutputStream(dst);
		FileChannel inChannel = inStream.getChannel();
		FileChannel outChannel = outStream.getChannel();
		inChannel.transferTo(0, inChannel.size(), outChannel);
		inStream.close();
		outStream.close();
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
