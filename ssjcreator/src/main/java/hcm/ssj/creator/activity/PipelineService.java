/*
 * PipelineService.java
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

package hcm.ssj.creator.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import hcm.ssj.core.Pipeline;
import hcm.ssj.creator.R;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

/**
 * Created by Michael Dietz on 10.09.2021.
 */
public class PipelineService extends Service
{
	public static final int ONGOING_NOTIFICATION_ID = 1;
	public static final String SERVICE_NOTIFICATION_CHANNEL = "hcm.ssj.creator";
	public static final String SERVICE_NOTIFICATION_CHANNEL_NAME = "Pipeline Service";

	Pipeline pipeline;

	@Override
	public IBinder onBind(Intent intent)
	{
		// Don't allow binding
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		startForegroundNotification();

		startPipeline();
	}

	private void startPipeline()
	{
		pipeline = Pipeline.getInstance();

		if (!pipeline.isRunning())
		{
			new Thread()
			{
				@Override
				public void run()
				{
					pipeline.start();
				}
			}.start();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	private void startForegroundNotification()
	{
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			// Create separate notification channel
			NotificationChannel channel = new NotificationChannel(SERVICE_NOTIFICATION_CHANNEL, SERVICE_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);

			builder = builder.setChannelId(SERVICE_NOTIFICATION_CHANNEL);
		}

		Notification notification = builder.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.str_notification_text))
				.setSmallIcon(R.drawable.logo_small_black)
				.setContentIntent(pendingIntent)
				.setTicker(getString(R.string.str_notification_text))
				.build();


		startForeground(ONGOING_NOTIFICATION_ID, notification);
	}
}
