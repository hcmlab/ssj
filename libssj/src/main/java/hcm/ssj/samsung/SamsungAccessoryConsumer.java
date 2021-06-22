/*
 * SamsungAccessoryProvider.java
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

package hcm.ssj.samsung;

import android.content.Context;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.IOException;
import java.util.ArrayDeque;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 02.06.2021.
 */
public class SamsungAccessoryConsumer extends SAAgentV2
{
	public static final String TAG = SamsungAccessoryConsumer.class.getSimpleName();
	public static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
	public static final int COMMAND_CHANNEL_ID = 1001;
	public static final int ANNOTATION_CHANNEL_ID = 1002;
	public static final int HR_CHANNEL_ID = 1003;
	public static final int PPG_CHANNEL_ID = 1004;

	public enum AccessoryCommand
	{
		SENSOR_HR("SENSOR_HR"),
		SENSOR_PPG("SENSOR_PPG"),
		PIPELINE_STOP("PIPELINE_STOP");

		final String cmd;

		AccessoryCommand(String command)
		{
			this.cmd = command;
		}
	}

	public interface AccessoryAnnotationListener
	{
		void handleAnnotation(String data);
	}

	ArrayDeque<Float> hrQueue = new ArrayDeque<>();
	ArrayDeque<Float> rrQueue = new ArrayDeque<>();
	ArrayDeque<Float> ppgQueue = new ArrayDeque<>();

	ServiceConnection connectionSocket = null;
	boolean connected = false;
	AccessoryAnnotationListener annotationListener = null;

	public SamsungAccessoryConsumer(Context context)
	{
		super(TAG, context, SASOCKET_CLASS);

		Log.i("Created SamsungAccessoryConsumer");

		reset();

		try
		{
			SA accessory = new SA();
			accessory.initialize(context);
		}
		catch (SsdkUnsupportedException e)
		{
			// try to handle SsdkUnsupportedException
			processUnsupportedException(e);
		}
		catch (Exception e1)
		{
			Log.e("Failed to initialize Samsung Accessory SDK", e1);
			/*
			 * Your application can not use Samsung Accessory SDK. Your application should work smoothly
			 * without using this SDK, or you may want to notify user and close your application gracefully
			 * (release resources, stop Service threads, close UI thread, etc.)
			 */
			releaseAgent();
		}
	}

	public void reset()
	{
		hrQueue.clear();
		rrQueue.clear();
	}

	public void connect()
	{
		findPeerAgents();
	}

	public void disconnect()
	{
		if (connectionSocket != null)
		{
			connectionSocket.close();
			connectionSocket = null;

			updateStatus(false);
		}
	}

	public boolean isConnected()
	{
		return connected;
	}

	private void updateStatus(boolean connected)
	{
		this.connected = connected;
	}

	public void setAnnotationListener(AccessoryAnnotationListener annotationListener)
	{
		this.annotationListener = annotationListener;
	}

	@Override
	protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result)
	{
		if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null))
		{
			for (SAPeerAgent peerAgent : peerAgents)
			{
				requestServiceConnection(peerAgent);
			}
		}
		else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED)
		{
			Log.e("Peer device not connected!");
			updateStatus(false);

		}
		else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND)
		{
			Log.e("Find peer result: service not found");
			updateStatus(false);
		}
		else
		{
			Log.e("Find peer result: no peers have been found!");
		}
	}

	@Override
	protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result)
	{
		if (result == PEER_AGENT_AVAILABLE)
		{
			if (!connected)
			{
				for (SAPeerAgent peerAgent : peerAgents)
				{
					requestServiceConnection(peerAgent);
				}
			}
		}
		else if (result == PEER_AGENT_UNAVAILABLE)
		{
			// Do nothing
		}
	}

	@Override
	protected void onServiceConnectionRequested(SAPeerAgent peerAgent)
	{
		if (peerAgent != null)
		{
			Log.i("Service connection accepted");

			acceptServiceConnectionRequest(peerAgent);
		}
	}

	@Override
	protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result)
	{
		if (result == SAAgentV2.CONNECTION_SUCCESS)
		{
			if (socket != null)
			{
				connectionSocket = (ServiceConnection) socket;
				updateStatus(true);
			}
		}
		else if (result == SAAgentV2.CONNECTION_ALREADY_EXIST)
		{
			Log.e("Service connection already exists");
			updateStatus(true);
		}
		else
		{
			Log.e("Service connection with AccessoryProvider failed (code: " + result + ")");
		}
	}

	public void sendCommand(final AccessoryCommand command)
	{
		if (connectionSocket != null)
		{
			Log.i("Sending command: " + command.cmd);

			new Thread(() -> {
				try
				{
					connectionSocket.send(COMMAND_CHANNEL_ID, command.cmd.getBytes());
				}
				catch (IOException e)
				{
					Log.e("Failed to send data to accessory device", e);
				}
			}).start();
		}
	}

	public void sendData(final String data)
	{
		if (connectionSocket != null)
		{
			Log.i("Sending data: " + data);

			new Thread(() -> {
				try
				{
					connectionSocket.send(COMMAND_CHANNEL_ID, data.getBytes());
				}
				catch (IOException e)
				{
					Log.e("Failed to send data to accessory device", e);
				}
			}).start();
		}
	}

	public class ServiceConnection extends SASocket
	{
		public ServiceConnection()
		{
			super(ServiceConnection.class.getName());
		}

		@Override
		public void onError(int channelId, String errorMessage, int errorCode)
		{
		}

		@Override
		public void onReceive(int channelId, byte[] data)
		{
			if (connectionSocket == null)
			{
				return;
			}

			String dataConverted = new String(data);

			if (channelId == COMMAND_CHANNEL_ID)
			{
				Log.d("Received command: " + dataConverted);
			}
			else if (channelId == ANNOTATION_CHANNEL_ID)
			{
				if (annotationListener != null)
				{
					Log.d("Received annotation: " + dataConverted);

					annotationListener.handleAnnotation(dataConverted);
				}
			}
			else if (channelId == HR_CHANNEL_ID)
			{
				String[] samples = dataConverted.split(",");

				for (String sample : samples)
				{
					String[] sampleData = sample.split(";");

					hrQueue.add(Float.parseFloat(sampleData[0]));
					rrQueue.add(Float.parseFloat(sampleData[1]));
				}
			}
			else if (channelId == PPG_CHANNEL_ID)
			{
				String[] samples = dataConverted.split(",");

				for (String sample : samples)
				{
					ppgQueue.add(Float.parseFloat(sample));
				}
			}
		}

		@Override
		protected void onServiceConnectionLost(int reason)
		{
			disconnect();

			Log.i("Service Connection with AccessoryProvider has been terminated.");
		}
	}


	private boolean processUnsupportedException(SsdkUnsupportedException e)
	{
		boolean unsupported = true;

		Log.e("Unsupported exception", e);

		int errType = e.getType();
		if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED)
		{
			/*
			 * Your application can not use Samsung Accessory SDK. You application should work smoothly
			 * without using this SDK, or you may want to notify user and close your app gracefully (release
			 * resources, stop Service threads, close UI thread, etc.)
			 */
			releaseAgent();
		}
		else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED)
		{
			Log.e("You need to install Samsung Accessory SDK to use this application.");
		}
		else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED)
		{
			Log.e("You need to update Samsung Accessory SDK to use this application.");
		}
		else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED)
		{
			Log.e("We recommend that you update your Samsung Accessory SDK before using this application.");

			unsupported = false;
		}


		return unsupported;
	}
}

